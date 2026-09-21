import request from '@/utils/request';

export const getAccountSecurityInfo = () => request.get('/api/account/security');

export const sendAccountCode = (data) =>
  request.post('/api/account/send-code', data, { skipAuth: true });

export const loginWithCode = (data) =>
  request.post('/api/account/login/code', data, { skipAuth: true });

export const loginWithPassword = (data) =>
  request.post('/api/account/login/password', data, { skipAuth: true });

export const registerAccount = (data) =>
  request.post('/api/account/register', data, { skipAuth: true });

export const resetAccountPassword = (data) =>
  request.post('/api/account/reset-password', data, { skipAuth: true });

export const logoutAccount = () => request.post('/api/account/logout');
