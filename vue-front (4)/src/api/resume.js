import request from '@/utils/request';
import { presentResumeAnalysis } from '@/utils/resumePresentation';

const AUTH_USER_INFO_STORAGE_KEY = 'app_auth_user_info';
const RESUME_HISTORY_STORAGE_KEY = 'app_resume_history_cache';
const DEFAULT_UPLOAD_USER_ID = Number(import.meta.env.VITE_RESUME_UPLOAD_USER_ID) || 1007;
const MAX_HISTORY_SIZE = 20;
const USE_MOCK_RESUME = String(import.meta.env.VITE_USE_MOCK_RESUME ?? import.meta.env.VITE_USE_MOCK ?? 'true') !== 'false';
const PROXY_TARGET = (import.meta.env.VITE_PROXY_TARGET ?? '').trim().replace(/\/$/, '');
const runtimeResumeFileCache = new Map();

const DEFAULT_UPLOAD_GUIDE = Object.freeze({
  uploadTitle: 'Click or drop to upload',
  uploadDescription: 'Support PDF, DOC, DOCX (max 10MB)',
  jdPlaceholder: 'Paste JD text to generate role-oriented optimization hints',
  agentTitle: 'AI Resume Optimizer',
  agentDescription:
    'After upload, parser output is split and stored into structured fragments (project/internship/etc).',
});

const safeParseJson = (value, fallback = null) => {
  if (!value) {
    return fallback;
  }

  try {
    return JSON.parse(value);
  } catch (error) {
    return fallback;
  }
};

const resolveNumericUserId = (rawValue) => {
  if (rawValue === undefined || rawValue === null || rawValue === '') {
    return null;
  }

  if (typeof rawValue === 'number' && Number.isFinite(rawValue)) {
    return Math.trunc(rawValue);
  }

  const source = String(rawValue).trim();

  if (!source) {
    return null;
  }

  const directNumber = Number(source);

  if (Number.isFinite(directNumber)) {
    return Math.trunc(directNumber);
  }

  const digits = source.match(/\d+/)?.[0];

  if (!digits) {
    return null;
  }

  const normalized = Number(digits);
  return Number.isFinite(normalized) ? Math.trunc(normalized) : null;
};

const resolveUploadUserId = () => {
  if (typeof window === 'undefined') {
    return DEFAULT_UPLOAD_USER_ID;
  }

  const userInfo = safeParseJson(window.localStorage.getItem(AUTH_USER_INFO_STORAGE_KEY), {});
  const storageUserId = resolveNumericUserId(userInfo?.userId ?? userInfo?.id);

  return storageUserId && storageUserId > 0 ? storageUserId : DEFAULT_UPLOAD_USER_ID;
};

const resolveVariantFromFileName = (fileName = '') => {
  const extension = fileName.split('.').pop()?.toLowerCase();
  return extension === 'doc' || extension === 'docx' ? 'blue' : 'pink';
};

const stripFileExtension = (fileName = '') => {
  const dotIndex = fileName.lastIndexOf('.');
  return dotIndex > 0 ? fileName.slice(0, dotIndex) : fileName;
};

const loadResumeHistory = () => {
  if (typeof window === 'undefined') {
    return [];
  }

  const parsed = safeParseJson(window.localStorage.getItem(RESUME_HISTORY_STORAGE_KEY), []);
  return Array.isArray(parsed) ? parsed : [];
};

const saveResumeHistory = (history) => {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(RESUME_HISTORY_STORAGE_KEY, JSON.stringify(history));
};

const upsertResumeHistoryEntry = (entry) => {
  if (!entry?.resumeId) {
    return;
  }

  const history = loadResumeHistory();
  const nextHistory = [entry, ...history.filter((item) => item.resumeId !== entry.resumeId)].slice(
    0,
    MAX_HISTORY_SIZE,
  );
  saveResumeHistory(nextHistory);
};

const getHistoryEntryByResumeId = (resumeId) => {
  const normalizedTarget = normalizeResumeIdentifier(resumeId);
  const numericTarget = Number(resumeId);

  return (
    loadResumeHistory().find((item) => {
      const normalizedCurrent = normalizeResumeIdentifier(item.resumeId);
      if (normalizedCurrent && normalizedCurrent === normalizedTarget) {
        return true;
      }

      const numericCurrent = Number(item.resumeId);
      return Number.isFinite(numericCurrent) && Number.isFinite(numericTarget) && numericCurrent === numericTarget;
    }) || null
  );
};

const rememberRuntimeResumeFile = (resumeId, file) => {
  if (!resumeId || !(file instanceof Blob)) {
    return;
  }

  runtimeResumeFileCache.set(String(resumeId), file);
};

const resolveAbsoluteUrl = (url = '') => {
  const value = String(url || '').trim();

  if (!value) {
    return '';
  }

  if (/^(https?:)?\/\//i.test(value)) {
    return value;
  }

  if (value.startsWith('/')) {
    if (PROXY_TARGET) {
      return `${PROXY_TARGET}${value}`;
    }

    if (typeof window !== 'undefined') {
      return `${window.location.origin}${value}`;
    }
  }

  return value;
};

const normalizeResumeIdentifier = (value) => {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  return String(value);
};

const resolveResumeUserId = (entry = {}) => {
  const explicitUserId = resolveNumericUserId(entry.userId ?? entry.ownerUserId);
  return explicitUserId && explicitUserId > 0 ? explicitUserId : resolveUploadUserId();
};

const buildFallbackResumeUrls = (entry = {}) => {
  const resumeId = normalizeResumeIdentifier(entry.resumeId ?? entry.id);
  const resumeFileId = normalizeResumeIdentifier(entry.resumeFileId);
  const userId = resolveResumeUserId(entry);
  const urls = [];

  if (resumeFileId && userId) {
    urls.push(`/interview/admin/users/${userId}/resumes/files/${encodeURIComponent(resumeFileId)}/file`);
  }

  if (resumeId) {
    urls.push(`/api/app/resume/${encodeURIComponent(resumeId)}/download`);
    urls.push(`/resume/${encodeURIComponent(resumeId)}/download`);
  }

  return urls;
};

const resolveResumeOpenUrl = (entry = {}, { download = false } = {}) => {
  const candidates = [
    download ? entry.downloadUrl : entry.previewUrl,
    entry.downloadUrl,
    entry.previewUrl,
    entry.fileUrl,
    entry.url,
    ...buildFallbackResumeUrls(entry),
  ];

  for (const candidate of candidates) {
    const absoluteUrl = resolveAbsoluteUrl(candidate);
    if (absoluteUrl) {
      return absoluteUrl;
    }
  }

  return '';
};

export const openResumeInBrowser = (entry, { download = false } = {}) => {
  if (!entry || typeof window === 'undefined') {
    return {
      ok: false,
      message: 'Current environment does not support opening resumes.',
    };
  }

  const cacheKey = normalizeResumeIdentifier(entry.resumeId ?? entry.id);
  const runtimeFile = runtimeResumeFileCache.get(cacheKey);

  if (runtimeFile instanceof Blob) {
    const blobUrl = URL.createObjectURL(runtimeFile);
    if (download) {
      const anchor = document.createElement('a');
      anchor.href = blobUrl;
      anchor.download = entry?.name || 'resume.pdf';
      anchor.rel = 'noopener noreferrer';
      anchor.click();
    } else {
      window.open(blobUrl, '_blank', 'noopener,noreferrer');
    }

    window.setTimeout(() => {
      URL.revokeObjectURL(blobUrl);
    }, 60_000);

    return {
      ok: true,
      source: 'runtime-file',
      url: blobUrl,
    };
  }

  const url = resolveResumeOpenUrl(entry, { download });
  if (!url) {
    return {
      ok: false,
      message: 'No available preview link for this resume. Please re-upload and try again.',
    };
  }

  if (download) {
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.target = '_blank';
    anchor.rel = 'noopener noreferrer';
    anchor.download = entry?.name || '';
    anchor.click();
    return { ok: true, source: 'url', url };
  }

  const popup = window.open(url, '_blank', 'noopener,noreferrer');
  if (!popup) {
    return {
      ok: false,
      message: 'Popup was blocked by the browser. Please allow popups and retry.',
      url,
    };
  }

  return { ok: true, source: 'url', url };
};

const detectExtensionFromName = (fileName = '') => {
  const normalized = String(fileName || '').trim();
  const dotIndex = normalized.lastIndexOf('.');

  if (dotIndex < 0 || dotIndex === normalized.length - 1) {
    return '';
  }

  return normalized.slice(dotIndex + 1).toLowerCase();
};

const detectExtensionFromMime = (mimeType = '') => {
  const normalized = String(mimeType || '').toLowerCase();

  if (normalized.includes('pdf')) {
    return 'pdf';
  }

  if (
    normalized.includes('wordprocessingml.document') ||
    normalized.includes('application/vnd.openxmlformats-officedocument.wordprocessingml.document')
  ) {
    return 'docx';
  }

  if (normalized.includes('msword')) {
    return 'doc';
  }

  return '';
};

const detectResumeExtension = (entry = {}, blob = null) => {
  const extFromName = detectExtensionFromName(entry.name || entry.fileName || '');
  if (extFromName) {
    return extFromName;
  }

  return detectExtensionFromMime(blob?.type || '');
};

export const getResumePreviewResource = async (entry) => {
  if (!entry) {
    return {
      ok: false,
      message: 'Resume entry is empty. Please re-upload and try again.',
    };
  }

  const cacheKey = normalizeResumeIdentifier(entry.resumeId ?? entry.id);
  const runtimeFile = runtimeResumeFileCache.get(cacheKey);
  let blob = runtimeFile instanceof Blob ? runtimeFile : null;
  let sourceUrl = '';

  if (!blob) {
    sourceUrl = resolveResumeOpenUrl(entry, { download: false }) || resolveResumeOpenUrl(entry, { download: true });
    if (!sourceUrl) {
      return {
        ok: false,
        message: 'No available preview link for this resume. Please re-upload and try again.',
      };
    }

    try {
      blob = await request.get(sourceUrl, null, { responseType: 'blob' });
    } catch (error) {
      return {
        ok: false,
        message: error?.message || 'Failed to fetch resume file for preview.',
      };
    }
  }

  if (!(blob instanceof Blob)) {
    return {
      ok: false,
      message: 'Preview data is invalid. Please re-upload and try again.',
    };
  }

  const extension = detectResumeExtension(entry, blob);
  const fileName = String(entry.name || entry.fileName || (extension ? `resume.${extension}` : 'resume'));

  return {
    ok: true,
    blob,
    extension,
    fileName,
    sourceUrl,
    fromRuntimeCache: runtimeFile instanceof Blob,
  };
};

const readSectionCount = (sectionCounts, key) => {
  const value = Number(sectionCounts?.[key]);
  return Number.isFinite(value) && value > 0 ? value : 0;
};

const buildResumeAnalysis = ({ fileName, targetJdText, sectionCounts }) => {
  const workCount =
    readSectionCount(sectionCounts, 'WORK_EXPERIENCE') +
    readSectionCount(sectionCounts, 'INTERNSHIP_EXPERIENCE');
  const projectCount = readSectionCount(sectionCounts, 'PROJECT_EXPERIENCE');
  const educationCount = readSectionCount(sectionCounts, 'EDUCATION');
  const skillsCount = readSectionCount(sectionCounts, 'SKILLS');
  const totalFragments = Object.values(sectionCounts || {}).reduce((sum, value) => {
    const current = Number(value);
    return sum + (Number.isFinite(current) && current > 0 ? current : 0);
  }, 0);
  const hasJd = Boolean((targetJdText || '').trim());

  const score = Math.min(
    100,
    50 + workCount * 6 + projectCount * 7 + educationCount * 5 + skillsCount * 4 + (hasJd ? 8 : 0),
  );

  return {
    score,
    total: 100,
    badges: [
      hasJd ? 'JD tuned' : 'General parse',
      `Fragments: ${totalFragments}`,
      fileName ? `File: ${fileName}` : 'File parsed',
    ],
    sections: [
      {
        id: 'work-experience',
        title: 'Work and Internship',
        status: workCount > 0 ? 'Parsed' : 'Need content',
        originalText:
          workCount > 0
            ? `Detected ${workCount} work or internship fragment(s) from parser output.`
            : 'No work or internship fragment detected.',
        suggestions: [
          'Add measurable outcomes if possible',
          hasJd ? 'Highlight responsibilities matching JD keywords' : 'Highlight core impact and ownership',
          'Use action + context + result sentence style',
        ],
        actionLabel: 'Rewrite with AI',
      },
      {
        id: 'project-experience',
        title: 'Project Experience',
        status: projectCount > 0 ? 'Parsed' : 'Need content',
        optimizedText:
          projectCount > 0
            ? `Detected ${projectCount} project fragment(s). You can now refine project bullets.`
            : 'No project fragment detected.',
        primaryActionLabel: 'Apply changes',
        secondaryActionLabel: 'Undo',
      },
      {
        id: 'education',
        title: 'Education',
        status: educationCount > 0 ? 'Parsed' : 'Need content',
      },
    ],
    finalActionLabel: 'Generate final resume',
  };
};

const decodeUnicodeEscapes = (value = '') =>
  String(value)
    .replace(/\\u([0-9a-fA-F]{4})/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)))
    .replace(/\\n/g, '\n')
    .replace(/\\r/g, '\n')
    .replace(/\\t/g, ' ')
    .trim();

const normalizeAnalysisData = (data, fallbackInput) => {
  const fallback = buildResumeAnalysis(fallbackInput);
  const source = data && typeof data === 'object' ? data : fallback;
  const fallbackSections = Array.isArray(fallback.sections) ? fallback.sections : [];
  const sourceSections = Array.isArray(source.sections) ? source.sections : fallbackSections;

  return presentResumeAnalysis({
    fallback: source.fallback,
    fallbackReason: source.fallbackReason || '',
    score: Number(source.score ?? fallback.score ?? 0),
    total: Number(source.total ?? fallback.total ?? 100),
    badges: (Array.isArray(source.badges) ? source.badges : fallback.badges).map((badge) =>
      decodeUnicodeEscapes(badge),
    ),
    sections: sourceSections.map((section, index) => {
      const fallbackSection = fallbackSections[index] || {};
      const normalized = { ...fallbackSection, ...(section || {}) };

      if (normalized.title) {
        normalized.title = decodeUnicodeEscapes(normalized.title);
      }
      if (normalized.status) {
        normalized.status = decodeUnicodeEscapes(normalized.status);
      }
      if (normalized.originalText) {
        normalized.originalText = decodeUnicodeEscapes(normalized.originalText);
      }
      if (normalized.optimizedText) {
        normalized.optimizedText = decodeUnicodeEscapes(normalized.optimizedText);
      }
      if (normalized.actionLabel) {
        normalized.actionLabel = decodeUnicodeEscapes(normalized.actionLabel);
      }
      if (normalized.primaryActionLabel) {
        normalized.primaryActionLabel = decodeUnicodeEscapes(normalized.primaryActionLabel);
      }
      if (normalized.secondaryActionLabel) {
        normalized.secondaryActionLabel = decodeUnicodeEscapes(normalized.secondaryActionLabel);
      }
      if (Array.isArray(normalized.suggestions)) {
        normalized.suggestions = normalized.suggestions.map((item) => decodeUnicodeEscapes(item));
      }

      return normalized;
    }),
    finalActionLabel: decodeUnicodeEscapes(source.finalActionLabel || fallback.finalActionLabel),
  });
};

const mapHistoryToLibraryEntry = (entry) => ({
  id: entry.resumeId,
  resumeId: entry.resumeId,
  name: entry.fileName || `resume-${entry.resumeId}.pdf`,
  sizeBytes: Number(entry.sizeBytes) || 0,
  sizeInMb: Number(entry.sizeInMb) || 0,
  updatedAt: entry.updatedAt || new Date().toISOString(),
  variant: resolveVariantFromFileName(entry.fileName || ''),
  userId: entry.userId || null,
  resumeFileId: entry.resumeFileId || null,
  storagePath: entry.storagePath || '',
  downloadUrl: entry.downloadUrl || '',
  previewUrl: entry.previewUrl || '',
  fileUrl: entry.fileUrl || '',
  actionLabel: 'Continue optimize',
  isDefault: false,
});

export const getResumeUploadGuide = async () => ({
  code: 200,
  data: DEFAULT_UPLOAD_GUIDE,
});

export const uploadResume = async (formData) => {
  const selectedFile = formData?.get?.('file');

  if (!selectedFile) {
    throw new Error('No file selected');
  }

  const customTitle = String(formData?.get?.('title') || '').trim();
  const resolvedTitle = customTitle || stripFileExtension(selectedFile.name);

  if (USE_MOCK_RESUME) {
    const mockFormData = new FormData();
    mockFormData.append('file', selectedFile);
    if (resolvedTitle) {
      mockFormData.append('title', resolvedTitle);
    }
    const mockResponse = await request.post('/api/resume/upload', mockFormData);

    const normalizedData = {
      ...mockResponse.data,
      fileName: mockResponse.data?.fileName || selectedFile.name,
      parseStatus: mockResponse.data?.parseStatus || 'success',
      optimizeStatus: mockResponse.data?.optimizeStatus || 'success',
      sizeBytes: Number(selectedFile.size) || 0,
      sizeInMb: Number(((Number(selectedFile.size) || 0) / 1024 / 1024).toFixed(1)),
      updatedAt: new Date().toISOString(),
      variant: resolveVariantFromFileName(selectedFile.name),
      actionLabel: mockResponse.data?.actionLabel || 'Continue optimize',
    };
    rememberRuntimeResumeFile(normalizedData.resumeId, selectedFile);

    upsertResumeHistoryEntry({
      resumeId: normalizedData.resumeId,
      userId: resolveUploadUserId(),
      resumeFileId: normalizedData.resumeFileId || null,
      fileName: normalizedData.fileName,
      sizeBytes: normalizedData.sizeBytes,
      sizeInMb: normalizedData.sizeInMb,
      updatedAt: normalizedData.updatedAt,
      storagePath: normalizedData.storagePath || '',
      downloadUrl: normalizedData.downloadUrl || '',
      previewUrl: normalizedData.previewUrl || '',
      fileUrl: normalizedData.fileUrl || '',
      sectionCounts: normalizedData.sectionCounts || {},
    });

    return {
      ...mockResponse,
      data: normalizedData,
    };
  }

  const requestedUserId = resolveNumericUserId(formData?.get?.('userId')) || resolveUploadUserId();
  const candidateUserIds = Array.from(new Set([requestedUserId, DEFAULT_UPLOAD_USER_ID, 1007, 1].filter(Boolean)));

  let response = null;
  let lastError = null;

  for (const userId of candidateUserIds) {
    const backendFormData = new FormData();
    backendFormData.append('userId', String(userId));
    backendFormData.append('file', selectedFile);

    if (resolvedTitle) {
      backendFormData.append('title', resolvedTitle);
    }

    try {
      response = await request.post('/resume/import', backendFormData);
      break;
    } catch (error) {
      lastError = error;
      const message = (error?.message || '').toLowerCase();
      const canRetry = message.includes('user not found');

      if (!canRetry) {
        throw error;
      }
    }
  }

  if (!response?.data?.resumeId) {
    throw lastError || new Error('Upload failed');
  }

  const normalizedData = {
    ...response.data,
    fileName: response.data.fileName || selectedFile.name,
    parseStatus: 'success',
    optimizeStatus: 'success',
    sizeBytes: Number(selectedFile.size) || 0,
    sizeInMb: Number(((Number(selectedFile.size) || 0) / 1024 / 1024).toFixed(1)),
    updatedAt: new Date().toISOString(),
    variant: resolveVariantFromFileName(selectedFile.name),
    actionLabel: 'Continue optimize',
  };
  rememberRuntimeResumeFile(normalizedData.resumeId, selectedFile);

  upsertResumeHistoryEntry({
    resumeId: normalizedData.resumeId,
    userId: requestedUserId,
    resumeFileId: normalizedData.resumeFileId || null,
    fileName: normalizedData.fileName,
    sizeBytes: normalizedData.sizeBytes,
    sizeInMb: normalizedData.sizeInMb,
    updatedAt: normalizedData.updatedAt,
    storagePath: normalizedData.storagePath || '',
    downloadUrl: normalizedData.downloadUrl || '',
    previewUrl: normalizedData.previewUrl || '',
    fileUrl: normalizedData.fileUrl || '',
    sectionCounts: normalizedData.sectionCounts || {},
  });

  return {
    ...response,
    data: normalizedData,
  };
};

export const analyzeResume = async (payload = {}) => {
  const resumeId = resolveNumericUserId(payload?.resumeId);

  if (!resumeId || resumeId <= 0) {
    throw new Error('Invalid resumeId');
  }

  const targetJdText = payload?.targetJdText || '';

  if (USE_MOCK_RESUME) {
    return request.post('/api/resume/analyze', {
      resumeId,
      targetJdText,
    });
  }

  const resumeResponse = await request.get(`/resume/${resumeId}`);
  const historyEntry = getHistoryEntryByResumeId(resumeId);
  const sectionCounts = historyEntry?.sectionCounts || {};
  const response = await request.post('/resume/analysis', {
    resumeId,
    targetJdText,
  });

  return {
    code: response?.code ?? 200,
    data: normalizeAnalysisData(response?.data, {
      fileName: payload?.fileName || historyEntry?.fileName || resumeResponse?.data?.title || '',
      targetJdText,
      sectionCounts: response?.data?.sectionCounts || sectionCounts,
    }),
  };
};

export const getResumeLibrary = async () => {
  const entries = loadResumeHistory()
    .map((item) => mapHistoryToLibraryEntry(item))
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());

  const defaultResume = entries[0] || null;
  const history = entries
    .slice(1)
    .map((item) => ({
      id: item.id,
      resumeId: item.resumeId,
      name: item.name,
      sizeBytes: item.sizeBytes,
      sizeInMb: item.sizeInMb,
      updatedAt: item.updatedAt,
      variant: item.variant,
      userId: item.userId,
      resumeFileId: item.resumeFileId,
      storagePath: item.storagePath,
      downloadUrl: item.downloadUrl,
      previewUrl: item.previewUrl,
      fileUrl: item.fileUrl,
    }))
    .slice(0, 10);

  return {
    code: 200,
    data: {
      defaultResume,
      history,
    },
  };
};
