import * as accountMocks from '@/mocks/account';
import * as homeMocks from '@/mocks/home';
import * as interviewMocks from '@/mocks/interview';
import * as jdMocks from '@/mocks/jd';
import * as resumeMocks from '@/mocks/resume';
import * as userMocks from '@/mocks/user';

const parseMockFlag = (value, fallback) => {
  if (value === undefined || value === null || value === '') {
    return fallback;
  }

  return value !== 'false';
};

const USE_MOCK = parseMockFlag(import.meta.env.VITE_USE_MOCK, true);
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

export const AUTH_EVENTS = Object.freeze({
  unauthorized: 'app:unauthorized',
});

const AUTH_STORAGE_KEYS = Object.freeze({
  token: 'app_auth_token',
  refreshToken: 'app_auth_refresh_token',
  expiresAt: 'app_auth_expires_at',
  userInfo: 'app_auth_user_info',
});

const UNAUTHORIZED_COOLDOWN_MS = 800;
const isClient = typeof window !== 'undefined';

let unauthorizedCooldownTimer = null;

const MODULE_MOCK_FLAGS = Object.freeze({
  account: parseMockFlag(import.meta.env.VITE_USE_MOCK_ACCOUNT, USE_MOCK),
  home: parseMockFlag(import.meta.env.VITE_USE_MOCK_HOME, USE_MOCK),
  interview: parseMockFlag(import.meta.env.VITE_USE_MOCK_INTERVIEW, USE_MOCK),
  jd: parseMockFlag(import.meta.env.VITE_USE_MOCK_JD, USE_MOCK),
  resume: parseMockFlag(import.meta.env.VITE_USE_MOCK_RESUME, USE_MOCK),
  user: parseMockFlag(import.meta.env.VITE_USE_MOCK_USER, USE_MOCK),
});

const mockRouteRegistry = {
  'GET /api/home/content': homeMocks.getHomeContent,
  'GET /api/interview/meta': interviewMocks.getInterviewMeta,
  'POST /api/interview/chat': interviewMocks.sendChatMessage,
  'POST /api/interview/audio-turn': interviewMocks.submitAudioTurn,
  'POST /api/jd/parse': jdMocks.parseJd,
  'GET /api/account/security': accountMocks.getAccountSecurityInfo,
  'POST /api/account/send-code': accountMocks.sendAccountCode,
  'POST /api/account/login/code': accountMocks.loginWithCode,
  'POST /api/account/login/password': accountMocks.loginWithPassword,
  'POST /api/account/register': accountMocks.registerAccount,
  'POST /api/account/reset-password': accountMocks.resetAccountPassword,
  'POST /api/account/logout': accountMocks.logoutAccount,
  'GET /api/resume/upload-guide': resumeMocks.getResumeUploadGuide,
  'POST /api/resume/upload': resumeMocks.uploadResume,
  'POST /api/resume/analyze': resumeMocks.analyzeResume,
  'GET /api/resume/library': resumeMocks.getResumeLibrary,
  'GET /api/user/profile': userMocks.getUserProfile,
  'PUT /api/user/profile': userMocks.updateUserProfile,
  'POST /api/user/profile/avatar': userMocks.uploadUserAvatar,
};

const isAbsoluteUrl = (url) => /^(https?:)?\/\//.test(url);

const getRoutePath = (url = '') => {
  const normalizedUrl = url.split('?')[0];

  if (!isAbsoluteUrl(normalizedUrl)) {
    return normalizedUrl;
  }

  try {
    return new URL(normalizedUrl).pathname;
  } catch (error) {
    return normalizedUrl;
  }
};

const resolveMockModule = (url) => {
  const routePath = getRoutePath(url);

  if (routePath.startsWith('/api/account/')) {
    return 'account';
  }

  if (routePath.startsWith('/api/home/')) {
    return 'home';
  }

  if (routePath.startsWith('/api/interview/')) {
    return 'interview';
  }

  if (routePath.startsWith('/api/jd/')) {
    return 'jd';
  }

  if (routePath.startsWith('/api/resume/')) {
    return 'resume';
  }

  if (routePath.startsWith('/api/user/')) {
    return 'user';
  }

  return null;
};

const shouldUseMockForRoute = (url) => {
  const moduleName = resolveMockModule(url);

  if (!moduleName) {
    return false;
  }

  return MODULE_MOCK_FLAGS[moduleName];
};

const getStoredAuthToken = () => {
  if (!isClient) {
    return '';
  }

  return window.localStorage.getItem(AUTH_STORAGE_KEYS.token) || '';
};

const clearStoredAuthSession = () => {
  if (!isClient) {
    return;
  }

  Object.values(AUTH_STORAGE_KEYS).forEach((key) => {
    window.localStorage.removeItem(key);
  });
  window.dispatchEvent(new Event('app:auth-session-changed'));
};

const isUnauthorizedCode = (status, code) =>
  status === 401 ||
  code === 401 ||
  code === 40100 ||
  code === '401' ||
  code === '40100';

const notifyUnauthorized = ({
  status = 401,
  code = 401,
  message = '登录状态已失效，请重新登录',
} = {}) => {
  if (!isClient || unauthorizedCooldownTimer) {
    return;
  }

  window.dispatchEvent(
    new CustomEvent(AUTH_EVENTS.unauthorized, {
      detail: {
        status,
        code,
        message,
        happenedAt: Date.now(),
      },
    }),
  );

  unauthorizedCooldownTimer = window.setTimeout(() => {
    unauthorizedCooldownTimer = null;
  }, UNAUTHORIZED_COOLDOWN_MS);
};

const buildSearchParams = (params = {}) => {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }

    if (Array.isArray(value)) {
      value.forEach((item) => {
        if (item !== undefined && item !== null && item !== '') {
          searchParams.append(key, item);
        }
      });
      return;
    }

    searchParams.append(key, value);
  });

  return searchParams;
};

const buildUrl = (url, params) => {
  const searchParams = buildSearchParams(params);
  const requestUrl = isAbsoluteUrl(url) ? url : `${API_BASE_URL}${url}`;
  const queryString = searchParams.toString();

  return queryString ? `${requestUrl}?${queryString}` : requestUrl;
};

const normalizePayload = (payload, status) => {
  if (
    payload &&
    typeof payload === 'object' &&
    ('code' in payload || 'data' in payload || 'message' in payload || 'error' in payload)
  ) {
    return {
      ...payload,
      message: payload.message ?? payload.msg ?? payload.error ?? '',
    };
  }

  return {
    code: status,
    data: payload,
    message: '',
  };
};

const isBusinessSuccess = (code) =>
  code === undefined ||
  code === null ||
  code === 0 ||
  code === 200 ||
  code === '0' ||
  code === '200';

const createRequestError = (message, details = {}) => {
  const error = new Error(message);
  Object.assign(error, details);
  return error;
};

const parseResponse = async (response, responseType) => {
  if (responseType === 'raw') {
    return response;
  }

  if (responseType === 'blob') {
    return response.blob();
  }

  if (responseType === 'text') {
    return response.text();
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') || '';

  if (contentType.includes('application/json')) {
    return response.json();
  }

  return response.text();
};

const resolveMock = async (method, url, { params, data }) => {
  const routeKey = `${method.toUpperCase()} ${url.split('?')[0]}`;
  const handler = mockRouteRegistry[routeKey];

  if (typeof handler !== 'function') {
    throw createRequestError(`Unknown mock route: ${routeKey}`, { routeKey });
  }

  const payload = method.toUpperCase() === 'GET' ? params : data;
  return handler(payload, { method });
};

const request = async (method, url, options = {}) => {
  const {
    params,
    data,
    headers = {},
    responseType = 'json',
    signal,
    skipAuth = false,
  } = options;

  if (shouldUseMockForRoute(url)) {
    return resolveMock(method, url, { params, data });
  }

  const isFormData = typeof FormData !== 'undefined' && data instanceof FormData;
  const isUrlSearchParams =
    typeof URLSearchParams !== 'undefined' && data instanceof URLSearchParams;
  const requestHeaders = new Headers(headers);

  if (!skipAuth && !requestHeaders.has('Authorization')) {
    const token = getStoredAuthToken();

    if (token) {
      requestHeaders.set('Authorization', `Bearer ${token}`);
    }
  }

  let body = data;

  if (data !== undefined && !isFormData && !isUrlSearchParams && responseType !== 'raw') {
    requestHeaders.set('Content-Type', 'application/json');
    body = JSON.stringify(data);
  }

  let response;

  try {
    response = await fetch(buildUrl(url, params), {
      method,
      headers: requestHeaders,
      body: method === 'GET' ? undefined : body,
      signal,
    });
  } catch (error) {
    throw createRequestError(error?.message || 'Network request failed', {
      cause: error,
    });
  }

  const payload = await parseResponse(response, responseType);

  if (responseType !== 'json') {
    if (!response.ok) {
      if (response.status === 401) {
        clearStoredAuthSession();
        notifyUnauthorized({
          status: response.status,
          code: 401,
        });
      }

      throw createRequestError(`Request failed with status ${response.status}`, {
        status: response.status,
        payload,
      });
    }

    return payload;
  }

  const normalizedPayload = normalizePayload(payload, response.status);

  if (!response.ok || !isBusinessSuccess(normalizedPayload.code)) {
    if (isUnauthorizedCode(response.status, normalizedPayload.code)) {
      clearStoredAuthSession();
      notifyUnauthorized({
        status: response.status,
        code: normalizedPayload.code,
        message: normalizedPayload.message || '登录状态已失效，请重新登录',
      });
    }

    throw createRequestError(
      normalizedPayload.message || `Request failed with status ${response.status}`,
      {
        status: response.status,
        code: normalizedPayload.code,
        payload: normalizedPayload,
      },
    );
  }

  return normalizedPayload;
};

request.get = (url, params, options = {}) => request('GET', url, { ...options, params });
request.post = (url, data, options = {}) => request('POST', url, { ...options, data });
request.put = (url, data, options = {}) => request('PUT', url, { ...options, data });
request.delete = (url, params, options = {}) =>
  request('DELETE', url, { ...options, params });

export default request;
