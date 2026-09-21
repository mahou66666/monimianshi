import request from '@/utils/request';

export const getUserProfile = () => request.get('/api/user/profile');

export const updateUserProfile = (data) => request.put('/api/user/profile', data);

export const uploadUserAvatar = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/api/user/profile/avatar', formData);
};
