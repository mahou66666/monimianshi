import { MOCK_DELAY } from '@/utils/constants';

const wait = (delay) => new Promise((resolve) => setTimeout(resolve, delay));

const AUTH_STORAGE_KEYS = Object.freeze({
  userInfo: 'app_auth_user_info',
});

const META_STORAGE_KEYS = Object.freeze({
  lastLoginAt: 'mock_account_last_login_at',
  passwordUpdatedAt: 'mock_account_password_updated_at',
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

const readStorage = (key, fallback = '') => {
  if (!isClient) {
    return fallback;
  }

  const value = window.localStorage.getItem(key);
  return value == null ? fallback : value;
};

const writeStorage = (key, value) => {
  if (!isClient) {
    return;
  }

  window.localStorage.setItem(key, String(value));
};

const normalizePhone = (phone = '') => String(phone).replace(/\D/g, '').slice(-11);

const pad = (value) => String(value).padStart(2, '0');

const formatDateTime = (date = new Date()) =>
  [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join('-') +
  ` ${pad(date.getHours())}:${pad(date.getMinutes())}`;

const nowText = () => formatDateTime(new Date());

const maskPhone = (phone = '') => {
  const normalized = normalizePhone(phone);

  if (!/^1\d{10}$/.test(normalized)) {
    return '未绑定手机号';
  }

  return `${normalized.slice(0, 3)}****${normalized.slice(-4)}`;
};

const deriveEmail = (userInfo, phone) => {
  if (userInfo?.email) {
    return userInfo.email;
  }

  const normalized = normalizePhone(phone);
  return normalized ? `user${normalized.slice(-4)}@example.com` : '未绑定邮箱';
};

const readUserInfo = () => safeParseJson(readStorage(AUTH_STORAGE_KEYS.userInfo), {}) || {};

const ensureSecurityMeta = () => {
  const lastLoginAt = readStorage(META_STORAGE_KEYS.lastLoginAt);
  const passwordUpdatedAt = readStorage(META_STORAGE_KEYS.passwordUpdatedAt);

  const resolvedLastLoginAt = lastLoginAt || nowText();
  const resolvedPasswordUpdatedAt = passwordUpdatedAt || resolvedLastLoginAt;

  if (!lastLoginAt) {
    writeStorage(META_STORAGE_KEYS.lastLoginAt, resolvedLastLoginAt);
  }

  if (!passwordUpdatedAt) {
    writeStorage(META_STORAGE_KEYS.passwordUpdatedAt, resolvedPasswordUpdatedAt);
  }

  return {
    lastLoginAt: resolvedLastLoginAt,
    passwordUpdatedAt: resolvedPasswordUpdatedAt,
  };
};

const touchLastLoginAt = () => {
  writeStorage(META_STORAGE_KEYS.lastLoginAt, nowText());

  if (!readStorage(META_STORAGE_KEYS.passwordUpdatedAt)) {
    writeStorage(META_STORAGE_KEYS.passwordUpdatedAt, nowText());
  }
};

const touchPasswordUpdatedAt = () => {
  writeStorage(META_STORAGE_KEYS.passwordUpdatedAt, nowText());
};

const buildMockUserInfo = (payload = {}) => ({
  userId: 'u1001',
  nickname: '张三',
  phone: normalizePhone(payload.phone) || '13800000000',
});

export const getAccountSecurityInfo = async () => {
  await wait(MOCK_DELAY.fast);

  const userInfo = readUserInfo();
  const phone = normalizePhone(userInfo?.phone) || '13800000000';
  const meta = ensureSecurityMeta();

  return {
    code: 200,
    data: {
      maskedPhone: maskPhone(phone),
      email: deriveEmail(userInfo, phone),
      lastLoginAt: meta.lastLoginAt,
      passwordUpdatedAt: meta.passwordUpdatedAt,
      loginMethods: [
        { key: 'code', label: '验证码登录', enabled: true },
        { key: 'password', label: '密码登录', enabled: true },
      ],
      securityTips: [
        '验证码 5 分钟内有效',
        '建议每月更新一次密码',
        '登录异常时可直接走密码重置流程',
      ],
    },
  };
};

export const sendAccountCode = async (payload = {}) => {
  await wait(MOCK_DELAY.normal);

  return {
    code: 200,
    data: {
      scene: payload.scene || 'login',
      phone: payload.phone || '13800000000',
      countdownSeconds: 60,
      expiresInMinutes: 5,
    },
  };
};

export const loginWithCode = async (payload = {}) => {
  await wait(MOCK_DELAY.normal);
  touchLastLoginAt();

  return {
    code: 200,
    data: {
      loginType: 'code',
      token: 'mock-token-by-code',
      userInfo: buildMockUserInfo(payload),
    },
  };
};

export const loginWithPassword = async (payload = {}) => {
  await wait(MOCK_DELAY.normal);
  touchLastLoginAt();

  return {
    code: 200,
    data: {
      loginType: 'password',
      token: 'mock-token-by-password',
      userInfo: buildMockUserInfo(payload),
    },
  };
};

export const registerAccount = async (payload = {}) => {
  await wait(MOCK_DELAY.normal);
  touchLastLoginAt();

  return {
    code: 200,
    data: {
      registered: true,
      userInfo: buildMockUserInfo(payload),
    },
  };
};

export const resetAccountPassword = async () => {
  await wait(MOCK_DELAY.normal);
  touchPasswordUpdatedAt();

  return {
    code: 200,
    data: {
      reset: true,
      message: '密码已重置，请使用新密码登录',
    },
  };
};

export const logoutAccount = async () => {
  await wait(MOCK_DELAY.fast);

  return {
    code: 200,
    data: {
      loggedOut: true,
    },
  };
};
