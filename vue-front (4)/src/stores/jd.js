import { reactive } from 'vue';
import { parseJd } from '@/api/jd';
import { REQUEST_STATUS } from '@/utils/constants';

const DEFAULT_ERROR_MESSAGE = 'JD 解析失败，请稍后重试';
const JD_HISTORY_STORAGE_KEY = 'app_jd_history_cache';
const JD_HISTORY_MAX_ITEMS = 50;

const state = reactive({
  draftText: '',
  submittedText: '',
  analysisResult: null,
  status: REQUEST_STATUS.idle,
  errorMessage: '',
});

let pendingParsePromise = null;

const normalizeErrorMessage = (error, fallback = DEFAULT_ERROR_MESSAGE) =>
  error?.message || error?.payload?.message || fallback;

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

const loadJdHistory = () => {
  if (typeof window === 'undefined') {
    return [];
  }

  const parsed = safeParseJson(window.localStorage.getItem(JD_HISTORY_STORAGE_KEY), []);
  return Array.isArray(parsed) ? parsed : [];
};

const saveJdHistory = (history) => {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(JD_HISTORY_STORAGE_KEY, JSON.stringify(history));
};

const upsertJdHistoryEntry = (entry) => {
  if (!entry?.id) {
    return;
  }

  const history = loadJdHistory();
  const nextHistory = [entry, ...history.filter((item) => item.id !== entry.id)].slice(
    0,
    JD_HISTORY_MAX_ITEMS,
  );
  saveJdHistory(nextHistory);
};

const toLimitedText = (value, maxLength = 220) => {
  const normalized = String(value || '').replace(/\s+/g, ' ').trim();
  if (!normalized) {
    return '';
  }
  if (normalized.length <= maxLength) {
    return normalized;
  }
  return `${normalized.slice(0, maxLength)}...`;
};

const buildJdHistoryEntry = (payload = {}, result = {}) => ({
  id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  createdAt: new Date().toISOString(),
  jdText: toLimitedText(payload?.jdText || '', 260),
  title: String(result?.title || 'JD 解析结果').trim(),
  summary: toLimitedText(result?.summary || '', 200),
  tags: Array.isArray(result?.tags) ? result.tags.slice(0, 4) : [],
  hardSkillCount: Array.isArray(result?.hardSkills) ? result.hardSkills.length : 0,
  softSkillCount: Array.isArray(result?.softSkills) ? result.softSkills.length : 0,
});

const setStatus = (status, errorMessage = '') => {
  state.status = status;
  state.errorMessage = errorMessage;
};

const setDraftText = (value) => {
  state.draftText = value;
};

const clearDraftText = () => {
  state.draftText = '';
};

const resetAnalysis = () => {
  state.analysisResult = null;
  state.submittedText = '';
  setStatus(REQUEST_STATUS.idle);
};

const runParseRequest = (payload) => {
  state.analysisResult = null;
  setStatus(REQUEST_STATUS.loading);

  pendingParsePromise = parseJd(payload)
    .then((response) => {
      state.analysisResult = response.data;
      setStatus(REQUEST_STATUS.success);
      upsertJdHistoryEntry(buildJdHistoryEntry(payload, response.data));
      return state.analysisResult;
    })
    .catch((error) => {
      state.analysisResult = null;
      setStatus(REQUEST_STATUS.error, normalizeErrorMessage(error));
      return null;
    })
    .finally(() => {
      pendingParsePromise = null;
    });

  return pendingParsePromise;
};

const ensureAnalysisResult = async () => {
  if (state.analysisResult && state.status === REQUEST_STATUS.success) {
    return state.analysisResult;
  }

  if (pendingParsePromise) {
    return pendingParsePromise;
  }

  if (!state.submittedText.trim()) {
    setStatus(REQUEST_STATUS.idle);
    return null;
  }

  return runParseRequest({ jdText: state.submittedText });
};

const parseCurrentJd = async (jdText) => {
  const normalizedText = jdText?.trim() ?? '';

  if (!normalizedText) {
    state.analysisResult = null;
    state.submittedText = '';
    setStatus(REQUEST_STATUS.idle);
    return null;
  }

  state.submittedText = normalizedText;
  return runParseRequest({ jdText: normalizedText });
};

export const useJdStore = () => ({
  state,
  setDraftText,
  clearDraftText,
  resetAnalysis,
  ensureAnalysisResult,
  parseCurrentJd,
  getHistoryRecords: () => loadJdHistory(),
});
