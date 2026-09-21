import { reactive } from 'vue';
import {
  getAccountSecurityInfo,
  loginWithCode,
  loginWithPassword,
  logoutAccount,
  registerAccount,
  resetAccountPassword,
  sendAccountCode,
} from '@/api/account';
import { REQUEST_STATUS } from '@/utils/constants';

const PHONE_PATTERN = /^1\d{10}$/;
const PASSWORD_MIN_LENGTH = 6;

const AUTH_STORAGE_KEYS = Object.freeze({
  token: 'app_auth_token',
  refreshToken: 'app_auth_refresh_token',
  expiresAt: 'app_auth_expires_at',
  userInfo: 'app_auth_user_info',
});

const DEFAULT_ERROR_MESSAGES = Object.freeze({
  security: '账号安全信息加载失败，请稍后重试',
  sendCode: '验证码发送失败，请稍后重试',
  login: '登录失败，请稍后重试',
  register: '注册失败，请稍后重试',
  reset: '重置密码失败，请稍后重试',
  logout: '退出登录失败，请稍后重试',
});

const isClient = typeof window !== 'undefined';

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

const buildExpiresAt = (expiresIn) => {
  const seconds = Number(expiresIn);

  if (!Number.isFinite(seconds) || seconds <= 0) {
    return '';
  }

  return new Date(Date.now() + seconds * 1000).toISOString();
};

const loadPersistedAuthSession = () => {
  if (!isClient) {
    return null;
  }

  const token = window.localStorage.getItem(AUTH_STORAGE_KEYS.token);

  if (!token) {
    return null;
  }

  return {
    token,
    refreshToken: window.localStorage.getItem(AUTH_STORAGE_KEYS.refreshToken) || '',
    expiresAt: window.localStorage.getItem(AUTH_STORAGE_KEYS.expiresAt) || '',
    userInfo: safeParseJson(window.localStorage.getItem(AUTH_STORAGE_KEYS.userInfo), null),
  };
};

const persistAuthSession = (session) => {
  if (!isClient || !session?.token) {
    return;
  }

  window.localStorage.setItem(AUTH_STORAGE_KEYS.token, session.token);

  if (session.refreshToken) {
    window.localStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, session.refreshToken);
  } else {
    window.localStorage.removeItem(AUTH_STORAGE_KEYS.refreshToken);
  }

  if (session.expiresAt) {
    window.localStorage.setItem(AUTH_STORAGE_KEYS.expiresAt, session.expiresAt);
  } else {
    window.localStorage.removeItem(AUTH_STORAGE_KEYS.expiresAt);
  }

  if (session.userInfo) {
    window.localStorage.setItem(AUTH_STORAGE_KEYS.userInfo, JSON.stringify(session.userInfo));
  } else {
    window.localStorage.removeItem(AUTH_STORAGE_KEYS.userInfo);
  }
  window.dispatchEvent(new Event('app:auth-session-changed'));
};

const clearPersistedAuthSession = () => {
  if (!isClient) {
    return;
  }

  Object.values(AUTH_STORAGE_KEYS).forEach((key) => {
    window.localStorage.removeItem(key);
  });
  window.dispatchEvent(new Event('app:auth-session-changed'));
};

const createStatusMap = () => ({
  login: REQUEST_STATUS.idle,
  register: REQUEST_STATUS.idle,
  reset: REQUEST_STATUS.idle,
});

const createErrorMap = () => ({
  login: '',
  register: '',
  reset: '',
});

const createInitialState = () => ({
  loginMode: 'code',
  authSession: loadPersistedAuthSession(),
  securityInfo: null,
  securityStatus: REQUEST_STATUS.idle,
  securityErrorMessage: '',
  sendCodeStatus: createStatusMap(),
  sendCodeErrorMessage: createErrorMap(),
  submitStatus: {
    login: REQUEST_STATUS.idle,
    register: REQUEST_STATUS.idle,
    reset: REQUEST_STATUS.idle,
    logout: REQUEST_STATUS.idle,
  },
  submitErrorMessage: {
    login: '',
    register: '',
    reset: '',
    logout: '',
  },
  countdowns: {
    login: 0,
    register: 0,
    reset: 0,
  },
  feedbackMessage: {
    login: '',
    register: '',
    reset: '',
    logout: '',
  },
  forms: {
    login: {
      phone: '',
      code: '',
      password: '',
    },
    register: {
      phone: '',
      code: '',
      password: '',
      confirmPassword: '',
    },
    reset: {
      phone: '',
      code: '',
      password: '',
      confirmPassword: '',
    },
  },
});

const state = reactive(createInitialState());

const countdownTimers = {
  login: null,
  register: null,
  reset: null,
};

let pendingSecurityPromise = null;

const normalizeErrorMessage = (error, fallback) =>
  error?.message || error?.payload?.message || fallback;

const setSecurityState = (status, errorMessage = '') => {
  state.securityStatus = status;
  state.securityErrorMessage = errorMessage;
};

const setSendCodeState = (scene, status, errorMessage = '') => {
  state.sendCodeStatus[scene] = status;
  state.sendCodeErrorMessage[scene] = errorMessage;
};

const setSubmitState = (scene, status, errorMessage = '', feedbackMessage = '') => {
  state.submitStatus[scene] = status;
  state.submitErrorMessage[scene] = errorMessage;
  state.feedbackMessage[scene] = feedbackMessage;
};

const clearTimer = (scene) => {
  if (countdownTimers[scene]) {
    clearInterval(countdownTimers[scene]);
    countdownTimers[scene] = null;
  }
};

const startCountdown = (scene, seconds = 60) => {
  clearTimer(scene);
  state.countdowns[scene] = seconds;

  countdownTimers[scene] = setInterval(() => {
    if (state.countdowns[scene] <= 1) {
      state.countdowns[scene] = 0;
      clearTimer(scene);
      return;
    }

    state.countdowns[scene] -= 1;
  }, 1000);
};

const validatePhone = (phone) => PHONE_PATTERN.test((phone || '').trim());

const validatePassword = (password) => (password || '').trim().length >= PASSWORD_MIN_LENGTH;

const setLoginMode = (mode) => {
  state.loginMode = mode === 'password' ? 'password' : 'code';
  setSubmitState('login', REQUEST_STATUS.idle);
};

const updateField = (formName, field, value) => {
  if (!state.forms[formName] || !(field in state.forms[formName])) {
    return;
  }

  state.forms[formName][field] = value;

  if (formName === 'login') {
    setSubmitState('login', REQUEST_STATUS.idle);
    setSendCodeState('login', state.sendCodeStatus.login);
  }

  if (formName === 'register') {
    setSubmitState('register', REQUEST_STATUS.idle);
    setSendCodeState('register', state.sendCodeStatus.register);
  }

  if (formName === 'reset') {
    setSubmitState('reset', REQUEST_STATUS.idle);
    setSendCodeState('reset', state.sendCodeStatus.reset);
  }
};

const setAuthSession = (payload = {}) => {
  const token = (payload.token || '').trim();

  if (!token) {
    return null;
  }

  const session = {
    token,
    refreshToken: (payload.refreshToken || '').trim(),
    expiresAt: payload.expiresAt || buildExpiresAt(payload.expiresIn),
    userInfo: payload.userInfo || null,
  };

  state.authSession = session;
  persistAuthSession(session);
  return session;
};

const clearAuthSession = () => {
  state.authSession = null;
  clearPersistedAuthSession();
};

const restoreAuthSession = () => {
  const session = loadPersistedAuthSession();
  state.authSession = session;
  return session;
};

const isLoggedIn = () => Boolean(state.authSession?.token);

const ensureSecurityInfo = async ({ force = false } = {}) => {
  if (!force && state.securityInfo && state.securityStatus === REQUEST_STATUS.success) {
    return state.securityInfo;
  }

  if (!force && pendingSecurityPromise) {
    return pendingSecurityPromise;
  }

  setSecurityState(REQUEST_STATUS.loading);

  pendingSecurityPromise = getAccountSecurityInfo()
    .then((response) => {
      state.securityInfo = response.data;
      setSecurityState(REQUEST_STATUS.success);
      return state.securityInfo;
    })
    .catch((error) => {
      setSecurityState(
        REQUEST_STATUS.error,
        normalizeErrorMessage(error, DEFAULT_ERROR_MESSAGES.security),
      );
      return null;
    })
    .finally(() => {
      pendingSecurityPromise = null;
    });

  return pendingSecurityPromise;
};

const sendCode = async (scene) => {
  const form = state.forms[scene];
  const phone = form?.phone?.trim();

  if (!validatePhone(phone)) {
    const message = '请输入正确的 11 位手机号';
    setSendCodeState(scene, REQUEST_STATUS.error, message);
    throw new Error(message);
  }

  if (state.countdowns[scene] > 0 || state.sendCodeStatus[scene] === REQUEST_STATUS.loading) {
    return null;
  }

  setSendCodeState(scene, REQUEST_STATUS.loading);

  try {
    const response = await sendAccountCode({ phone, scene });
    startCountdown(scene, response.data?.countdownSeconds || 60);
    setSendCodeState(scene, REQUEST_STATUS.success);
    return response.data;
  } catch (error) {
    setSendCodeState(
      scene,
      REQUEST_STATUS.error,
      normalizeErrorMessage(error, DEFAULT_ERROR_MESSAGES.sendCode),
    );
    throw error;
  }
};

const validateLoginSubmission = () => {
  const phone = state.forms.login.phone.trim();

  if (!validatePhone(phone)) {
    return '请输入正确的 11 位手机号';
  }

  if (state.loginMode === 'code' && !state.forms.login.code.trim()) {
    return '请输入验证码';
  }

  if (state.loginMode === 'password' && !validatePassword(state.forms.login.password)) {
    return '密码至少需要 6 位字符';
  }

  return '';
};

const submitLogin = async () => {
  const validationMessage = validateLoginSubmission();

  if (validationMessage) {
    setSubmitState('login', REQUEST_STATUS.error, validationMessage);
    throw new Error(validationMessage);
  }

  setSubmitState('login', REQUEST_STATUS.loading);

  try {
    const payload =
      state.loginMode === 'code'
        ? {
            phone: state.forms.login.phone.trim(),
            code: state.forms.login.code.trim(),
          }
        : {
            phone: state.forms.login.phone.trim(),
            password: state.forms.login.password.trim(),
          };

    const response =
      state.loginMode === 'code'
        ? await loginWithCode(payload)
        : await loginWithPassword(payload);

    setAuthSession(response.data);
    setSubmitState('login', REQUEST_STATUS.success, '', '登录成功，正在进入账户管理');
    return response.data;
  } catch (error) {
    setSubmitState(
      'login',
      REQUEST_STATUS.error,
      normalizeErrorMessage(error, DEFAULT_ERROR_MESSAGES.login),
    );
    throw error;
  }
};

const validateRegisterSubmission = () => {
  const form = state.forms.register;

  if (!validatePhone(form.phone)) {
    return '请输入正确的 11 位手机号';
  }

  if (!form.code.trim()) {
    return '请输入验证码';
  }

  if (!validatePassword(form.password)) {
    return '密码至少需要 6 位字符';
  }

  if (form.password.trim() !== form.confirmPassword.trim()) {
    return '两次输入的密码不一致';
  }

  return '';
};

const submitRegister = async () => {
  const validationMessage = validateRegisterSubmission();

  if (validationMessage) {
    setSubmitState('register', REQUEST_STATUS.error, validationMessage);
    throw new Error(validationMessage);
  }

  setSubmitState('register', REQUEST_STATUS.loading);

  try {
    const form = state.forms.register;
    const response = await registerAccount({
      phone: form.phone.trim(),
      code: form.code.trim(),
      password: form.password.trim(),
    });

    if (response.data?.token) {
      setAuthSession(response.data);
    }

    setSubmitState('register', REQUEST_STATUS.success, '', '注册成功，账户已创建');
    return response.data;
  } catch (error) {
    setSubmitState(
      'register',
      REQUEST_STATUS.error,
      normalizeErrorMessage(error, DEFAULT_ERROR_MESSAGES.register),
    );
    throw error;
  }
};

const validateResetSubmission = () => {
  const form = state.forms.reset;

  if (!validatePhone(form.phone)) {
    return '请输入正确的 11 位手机号';
  }

  if (!form.code.trim()) {
    return '请输入验证码';
  }

  if (!validatePassword(form.password)) {
    return '密码至少需要 6 位字符';
  }

  if (form.password.trim() !== form.confirmPassword.trim()) {
    return '两次输入的密码不一致';
  }

  return '';
};

const submitResetPassword = async () => {
  const validationMessage = validateResetSubmission();

  if (validationMessage) {
    setSubmitState('reset', REQUEST_STATUS.error, validationMessage);
    throw new Error(validationMessage);
  }

  setSubmitState('reset', REQUEST_STATUS.loading);

  try {
    const form = state.forms.reset;
    const response = await resetAccountPassword({
      phone: form.phone.trim(),
      code: form.code.trim(),
      password: form.password.trim(),
    });

    setSubmitState(
      'reset',
      REQUEST_STATUS.success,
      '',
      response.data?.message || '密码已重置',
    );
    return response.data;
  } catch (error) {
    setSubmitState(
      'reset',
      REQUEST_STATUS.error,
      normalizeErrorMessage(error, DEFAULT_ERROR_MESSAGES.reset),
    );
    throw error;
  }
};

const submitLogout = async () => {
  setSubmitState('logout', REQUEST_STATUS.loading);

  try {
    const response = await logoutAccount();
    clearAuthSession();
    setSubmitState('logout', REQUEST_STATUS.success, '', '已退出当前账户');
    return response.data;
  } catch (error) {
    if (error?.status === 401 || error?.code === 401 || error?.code === '401') {
      clearAuthSession();
    }

    setSubmitState(
      'logout',
      REQUEST_STATUS.error,
      normalizeErrorMessage(error, DEFAULT_ERROR_MESSAGES.logout),
    );
    throw error;
  }
};

const resetFeedback = (scene) => {
  setSubmitState(scene, REQUEST_STATUS.idle);
};

const resetAll = () => {
  Object.keys(countdownTimers).forEach(clearTimer);

  const initialState = createInitialState();
  state.loginMode = initialState.loginMode;
  state.authSession = initialState.authSession;
  state.securityInfo = initialState.securityInfo;
  state.securityStatus = initialState.securityStatus;
  state.securityErrorMessage = initialState.securityErrorMessage;
  state.sendCodeStatus = initialState.sendCodeStatus;
  state.sendCodeErrorMessage = initialState.sendCodeErrorMessage;
  state.submitStatus = initialState.submitStatus;
  state.submitErrorMessage = initialState.submitErrorMessage;
  state.countdowns = initialState.countdowns;
  state.feedbackMessage = initialState.feedbackMessage;
  state.forms = initialState.forms;
};

export const useAccountStore = () => ({
  state,
  setLoginMode,
  updateField,
  ensureSecurityInfo,
  sendCode,
  submitLogin,
  submitRegister,
  submitResetPassword,
  submitLogout,
  setAuthSession,
  clearAuthSession,
  restoreAuthSession,
  isLoggedIn,
  resetFeedback,
  resetAll,
});
