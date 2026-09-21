import { reactive } from 'vue';
import {
  analyzeResume,
  getResumeLibrary,
  getResumeUploadGuide,
  uploadResume,
} from '@/api/resume';
import { REQUEST_STATUS, RESUME_UPLOAD } from '@/utils/constants';
import { watch } from 'vue';
import { readResumeSession, saveResumeSession, RESUME_SESSION_KEY } from '@/utils/resumeSession';

const DEFAULT_ERROR_MESSAGES = Object.freeze({
  uploadGuide: '上传说明加载失败，请稍后重试',
  upload: '简历上传失败，请稍后重试',
  analysis: '简历分析失败，请稍后重试',
  library: '简历库加载失败，请稍后重试',
});

const createRequestStatus = () => ({
  uploadGuide: REQUEST_STATUS.idle,
  upload: REQUEST_STATUS.idle,
  analysis: REQUEST_STATUS.idle,
  library: REQUEST_STATUS.idle,
});

const createRequestErrors = () => ({
  uploadGuide: '',
  upload: '',
  analysis: '',
  library: '',
});

const resolveVariantFromFileName = (fileName = '') => {
  const extension = fileName.split('.').pop()?.toLowerCase();
  return extension === 'doc' || extension === 'docx' ? 'blue' : 'pink';
};

const normalizeSizeBytes = (entry) => {
  const sizeBytes = Number(entry?.sizeBytes);

  if (Number.isFinite(sizeBytes) && sizeBytes > 0) {
    return sizeBytes;
  }

  const sizeInMb = Number(entry?.sizeInMb);

  if (Number.isFinite(sizeInMb) && sizeInMb > 0) {
    return Math.round(sizeInMb * 1024 * 1024);
  }

  return 0;
};

const createLibraryEntry = (entry, { isDefault = false } = {}) => {
  if (!entry) {
    return null;
  }

  const name = entry.name ?? entry.fileName ?? 'untitled-resume.pdf';
  const sizeBytes = normalizeSizeBytes(entry);
  const rawSizeInMb = Number(entry.sizeInMb);
  const sizeInMb =
    Number.isFinite(rawSizeInMb) && rawSizeInMb > 0
      ? rawSizeInMb
      : Number((sizeBytes / 1024 / 1024).toFixed(1));

  return {
    id: entry.id ?? entry.resumeId ?? `resume-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    resumeId: entry.resumeId ?? entry.id ?? null,
    userId: entry.userId ?? entry.ownerUserId ?? null,
    resumeFileId: entry.resumeFileId ?? null,
    name,
    sizeBytes,
    sizeInMb,
    updatedAt: entry.updatedAt ?? new Date().toISOString(),
    variant: entry.variant ?? resolveVariantFromFileName(name),
    storagePath: entry.storagePath ?? '',
    downloadUrl: entry.downloadUrl ?? '',
    previewUrl: entry.previewUrl ?? '',
    fileUrl: entry.fileUrl ?? '',
    actionLabel: entry.actionLabel ?? '继续润色',
    isDefault,
  };
};

const toHistoryEntry = (entry) => {
  const normalizedEntry = createLibraryEntry(entry);

  if (!normalizedEntry) {
    return null;
  }

  return {
    id: normalizedEntry.id,
    resumeId: normalizedEntry.resumeId,
    userId: normalizedEntry.userId,
    resumeFileId: normalizedEntry.resumeFileId,
    name: normalizedEntry.name,
    sizeBytes: normalizedEntry.sizeBytes,
    sizeInMb: normalizedEntry.sizeInMb,
    updatedAt: normalizedEntry.updatedAt,
    variant: normalizedEntry.variant,
    storagePath: normalizedEntry.storagePath,
    downloadUrl: normalizedEntry.downloadUrl,
    previewUrl: normalizedEntry.previewUrl,
    fileUrl: normalizedEntry.fileUrl,
  };
};

const normalizeLibrary = (library) => {
  const defaultResume = createLibraryEntry(library?.defaultResume, { isDefault: true });
  const history = Array.isArray(library?.history)
    ? library.history
        .map((item) => toHistoryEntry(item))
        .filter(Boolean)
        .filter((item) => item.id !== defaultResume?.id)
        .slice(0, 10)
    : [];

  return {
    defaultResume,
    history,
  };
};

const state = reactive({
  targetJdText: '',
  uploadGuide: null,
  selectedFile: null,
  selectedFileError: '',
  uploadResult: null,
  analysisResult: null,
  library: {
    defaultResume: null,
    history: [],
  },
  status: createRequestStatus(),
  errorMessage: createRequestErrors(),
});

let pendingGuidePromise = null;
let pendingAnalysisPromise = null;
let pendingLibraryPromise = null;

const currentOwner = () => {
  try {
    if (!window.localStorage.getItem('app_auth_token')) return null;
    const user = JSON.parse(window.localStorage.getItem('app_auth_user_info'));
    const id = user?.userId ?? user?.id;
    return id == null ? null : `user:${id}`;
  } catch { return null; }
};
let sessionOwner = currentOwner();
if (typeof window !== 'undefined') {
  let restored;
  try { restored = readResumeSession(window.sessionStorage, sessionOwner); } catch { /* Storage unavailable. */ }
  if (restored) {
    state.targetJdText = restored.targetJdText || '';
    if (restored.uploadResult?.resumeId && restored.analysisResult) {
      state.uploadResult = restored.uploadResult;
      state.analysisResult = restored.analysisResult;
      state.status.upload = REQUEST_STATUS.success;
      state.status.analysis = REQUEST_STATUS.success;
    }
  }
  const syncOwner = () => {
    const owner = currentOwner();
    if (owner === sessionOwner) return;
    sessionOwner = owner;
    state.selectedFile = null;
    state.uploadResult = null;
    state.analysisResult = null;
    state.targetJdText = '';
    state.library = {defaultResume:null,history:[]};
    state.status = createRequestStatus();
    state.errorMessage = createRequestErrors();
    try { window.sessionStorage.removeItem(RESUME_SESSION_KEY); } catch { /* Storage unavailable. */ }
  };
  window.addEventListener('app:auth-session-changed', syncOwner);
  window.addEventListener('storage', syncOwner);
  watch(() => [state.targetJdText, state.uploadResult, state.analysisResult, state.status.analysis], () => {
    if (currentOwner() !== sessionOwner) { syncOwner(); return; }
    try { saveResumeSession(window.sessionStorage, sessionOwner, state); } catch { /* Storage unavailable. */ }
  }, {deep:true, flush:'sync'});
}

const normalizeErrorMessage = (error, fallback) =>
  error?.message || error?.payload?.message || fallback;

const setRequestState = (key, status, errorMessage = '') => {
  state.status[key] = status;
  state.errorMessage[key] = errorMessage;
};

const syncUploadResultToLibrary = (uploadResult) => {
  if (!uploadResult) {
    return;
  }

  const previousDefault = state.library.defaultResume;
  const nextDefault = createLibraryEntry(
    {
      ...uploadResult,
      id: uploadResult.resumeId,
      fileName: uploadResult.fileName,
    },
    { isDefault: true },
  );

  state.library = {
    defaultResume: nextDefault,
    history: [
      ...(previousDefault && previousDefault.id !== nextDefault.id
        ? [toHistoryEntry(previousDefault)]
        : []),
      ...state.library.history.filter(
        (item) => item.id !== nextDefault.id && item.id !== previousDefault?.id,
      ),
    ].slice(0, 10),
  };
  setRequestState('library', REQUEST_STATUS.success);
};

const setSelectedFileError = (message = '') => {
  state.selectedFileError = message;
};

const clearTargetJdText = () => {
  state.targetJdText = '';
};

const clearSelectedFile = () => {
  state.selectedFile = null;
  state.uploadResult = null;
  state.analysisResult = null;
  setSelectedFileError('');
  setRequestState('upload', REQUEST_STATUS.idle);
  setRequestState('analysis', REQUEST_STATUS.idle);
};

const resetUploadState = () => {
  clearSelectedFile();
  clearTargetJdText();
};

const validateResumeFile = (file) => {
  if (!file) {
    return '请先选择简历文件';
  }

  const extension = file.name.split('.').pop()?.toLowerCase() ?? '';

  if (!RESUME_UPLOAD.allowedExtensions.includes(extension)) {
    return '仅支持 PDF、DOC、DOCX 格式';
  }

  if (file.size > RESUME_UPLOAD.maxSizeBytes) {
    return '文件大小不能超过 10MB';
  }

  return '';
};

const selectResumeFile = (file) => {
  const validationMessage = validateResumeFile(file);

  if (validationMessage) {
    state.selectedFile = null;
    state.uploadResult = null;
    state.analysisResult = null;
    setSelectedFileError(validationMessage);
    setRequestState('upload', REQUEST_STATUS.error, validationMessage);
    return false;
  }

  state.selectedFile = file;
  state.uploadResult = null;
  state.analysisResult = null;
  setSelectedFileError('');
  setRequestState('upload', REQUEST_STATUS.idle);
  setRequestState('analysis', REQUEST_STATUS.idle);
  return true;
};

const setTargetJdText = (value) => {
  state.targetJdText = value;
};

const ensureUploadGuide = async () => {
  if (state.uploadGuide && state.status.uploadGuide === REQUEST_STATUS.success) {
    return state.uploadGuide;
  }

  if (!pendingGuidePromise) {
    setRequestState('uploadGuide', REQUEST_STATUS.loading);

    pendingGuidePromise = getResumeUploadGuide()
      .then((response) => {
        state.uploadGuide = response.data;
        setRequestState('uploadGuide', REQUEST_STATUS.success);
        return state.uploadGuide;
      })
      .catch((error) => {
        setRequestState(
          'uploadGuide',
          REQUEST_STATUS.error,
          normalizeErrorMessage(error, DEFAULT_ERROR_MESSAGES.uploadGuide),
        );
        return null;
      })
      .finally(() => {
        pendingGuidePromise = null;
      });
  }

  return pendingGuidePromise;
};

const uploadCurrentResume = async ({ targetJdText = state.targetJdText, analysisId } = {}) => {
  const requestOwner = currentOwner();
  const validationMessage = validateResumeFile(state.selectedFile);

  if (validationMessage) {
    setSelectedFileError(validationMessage);
    setRequestState('upload', REQUEST_STATUS.error, validationMessage);
    throw new Error(validationMessage);
  }

  setRequestState('upload', REQUEST_STATUS.loading);
  setRequestState('analysis', REQUEST_STATUS.idle);
  setSelectedFileError('');
  state.analysisResult = null;

  try {
    const formData = new FormData();
    formData.append('file', state.selectedFile);

    if (targetJdText?.trim()) {
      formData.append('jdText', targetJdText.trim());
    }

    if (analysisId) {
      formData.append('analysisId', analysisId);
    }

    const response = await uploadResume(formData);
    if (requestOwner !== currentOwner()) throw new Error('登录用户已变化，请重新上传简历');
    state.uploadResult = response.data;
    syncUploadResultToLibrary(state.uploadResult);
    setRequestState('upload', REQUEST_STATUS.success);
    return state.uploadResult;
  } catch (error) {
    if (requestOwner !== currentOwner()) throw error;
    setRequestState(
      'upload',
      REQUEST_STATUS.error,
      normalizeErrorMessage(error, DEFAULT_ERROR_MESSAGES.upload),
    );
    throw error;
  }
};

const ensureAnalysisResult = async () => {
  const requestOwner = currentOwner();
  if (state.analysisResult && state.status.analysis === REQUEST_STATUS.success) {
    return state.analysisResult;
  }

  if (pendingAnalysisPromise) {
    return pendingAnalysisPromise;
  }

  if (!state.uploadResult?.resumeId) {
    setRequestState('analysis', REQUEST_STATUS.idle);
    return null;
  }

  setRequestState('analysis', REQUEST_STATUS.loading);

  pendingAnalysisPromise = analyzeResume({
    targetJdText: state.targetJdText,
    resumeId: state.uploadResult.resumeId,
    fileName: state.uploadResult.fileName,
  })
    .then((response) => {
      if (requestOwner !== currentOwner()) return null;
      state.analysisResult = response.data;
      setRequestState('analysis', REQUEST_STATUS.success);
      return state.analysisResult;
    })
    .catch((error) => {
      if (requestOwner !== currentOwner()) return null;
      state.analysisResult = null;
      setRequestState(
        'analysis',
        REQUEST_STATUS.error,
        normalizeErrorMessage(error, DEFAULT_ERROR_MESSAGES.analysis),
      );
      return null;
    })
    .finally(() => {
      pendingAnalysisPromise = null;
    });

  return pendingAnalysisPromise;
};

const analyzeCurrentResume = async (targetJdText, uploadResult = state.uploadResult) => {
  const requestOwner = currentOwner();
  state.analysisResult = null;
  setRequestState('analysis', REQUEST_STATUS.loading);

  pendingAnalysisPromise = analyzeResume({
    targetJdText,
    resumeId: uploadResult?.resumeId,
    fileName: uploadResult?.fileName,
  })
    .then((response) => {
      if (requestOwner !== currentOwner()) return null;
      state.analysisResult = response.data;
      setRequestState('analysis', REQUEST_STATUS.success);
      return state.analysisResult;
    })
    .catch((error) => {
      if (requestOwner !== currentOwner()) throw error;
      state.analysisResult = null;
      setRequestState(
        'analysis',
        REQUEST_STATUS.error,
        normalizeErrorMessage(error, DEFAULT_ERROR_MESSAGES.analysis),
      );
      throw error;
    })
    .finally(() => {
      pendingAnalysisPromise = null;
    });

  return pendingAnalysisPromise;
};

const uploadAndAnalyzeCurrentResume = async (targetJdText, options = {}) => {
  const uploadResult = await uploadCurrentResume({
    targetJdText,
    analysisId: options.analysisId,
  });

  const analysisResult = await analyzeCurrentResume(targetJdText, uploadResult);

  return {
    uploadResult,
    analysisResult,
  };
};

const ensureResumeLibrary = async () => {
  if (state.status.library === REQUEST_STATUS.success) {
    return state.library;
  }

  if (!pendingLibraryPromise) {
    setRequestState('library', REQUEST_STATUS.loading);

    pendingLibraryPromise = getResumeLibrary()
      .then((response) => {
        state.library = normalizeLibrary(response.data);
        setRequestState('library', REQUEST_STATUS.success);
        return state.library;
      })
      .catch((error) => {
        setRequestState(
          'library',
          REQUEST_STATUS.error,
          normalizeErrorMessage(error, DEFAULT_ERROR_MESSAGES.library),
        );
        return state.library;
      })
      .finally(() => {
        pendingLibraryPromise = null;
      });
  }

  return pendingLibraryPromise;
};

const setDefaultResume = (resumeId) => {
  if (!resumeId || state.library.defaultResume?.id === resumeId) {
    return;
  }

  const targetEntry = state.library.history.find((item) => item.id === resumeId);

  if (!targetEntry) {
    return;
  }

  const previousDefault = state.library.defaultResume;
  const nextDefault = createLibraryEntry(targetEntry, { isDefault: true });

  state.library = {
    defaultResume: nextDefault,
    history: [
      ...(previousDefault ? [toHistoryEntry(previousDefault)] : []),
      ...state.library.history.filter(
        (item) => item.id !== resumeId && item.id !== previousDefault?.id,
      ),
    ].slice(0, 10),
  };
  setRequestState('library', REQUEST_STATUS.success);
};

const deleteResume = (resumeId) => {
  if (!resumeId) {
    return;
  }

  if (state.library.defaultResume?.id === resumeId) {
    const [nextDefaultCandidate, ...restHistory] = state.library.history;

    state.library = {
      defaultResume: nextDefaultCandidate
        ? createLibraryEntry(nextDefaultCandidate, { isDefault: true })
        : null,
      history: restHistory.map((item) => toHistoryEntry(item)).filter(Boolean),
    };
    setRequestState('library', REQUEST_STATUS.success);
    return;
  }

  state.library = {
    defaultResume: state.library.defaultResume,
    history: state.library.history.filter((item) => item.id !== resumeId),
  };
  setRequestState('library', REQUEST_STATUS.success);
};

export const useResumeStore = () => ({
  state,
  selectResumeFile,
  clearSelectedFile,
  setTargetJdText,
  clearTargetJdText,
  validateResumeFile,
  uploadCurrentResume,
  ensureUploadGuide,
  ensureAnalysisResult,
  analyzeCurrentResume,
  uploadAndAnalyzeCurrentResume,
  ensureResumeLibrary,
  setDefaultResume,
  deleteResume,
  resetUploadState,
});
