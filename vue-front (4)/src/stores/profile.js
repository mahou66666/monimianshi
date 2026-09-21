import { reactive } from 'vue';
import { getUserProfile, updateUserProfile, uploadUserAvatar } from '@/api/user';
import { REQUEST_STATUS } from '@/utils/constants';

const DEFAULT_ERROR_MESSAGE = '个人信息加载失败，请稍后重试';
const DEFAULT_SAVE_ERROR_MESSAGE = '个人信息保存失败，请稍后重试';

const state = reactive({
  profile: null,
  status: REQUEST_STATUS.idle,
  errorMessage: '',
  saveStatus: REQUEST_STATUS.idle,
  saveErrorMessage: '',
});

let pendingProfilePromise = null;

const normalizeErrorMessage = (error, fallback = DEFAULT_ERROR_MESSAGE) =>
  error?.message || error?.payload?.message || fallback;

const setStatus = (status, errorMessage = '') => {
  state.status = status;
  state.errorMessage = errorMessage;
};

const setSaveStatus = (status, errorMessage = '') => {
  state.saveStatus = status;
  state.saveErrorMessage = errorMessage;
};

const ensureProfile = async ({ force = false } = {}) => {
  if (!force && state.profile && state.status === REQUEST_STATUS.success) {
    return state.profile;
  }

  if (!force && pendingProfilePromise) {
    return pendingProfilePromise;
  }

  setStatus(REQUEST_STATUS.loading);

  pendingProfilePromise = getUserProfile()
    .then((response) => {
      state.profile = response.data;
      setStatus(REQUEST_STATUS.success);
      return state.profile;
    })
    .catch((error) => {
      setStatus(REQUEST_STATUS.error, normalizeErrorMessage(error));
      return null;
    })
    .finally(() => {
      pendingProfilePromise = null;
    });

  return pendingProfilePromise;
};

const updateProfile = async (payload = {}) => {
  setSaveStatus(REQUEST_STATUS.loading);

  try {
    const response = await updateUserProfile(payload);
    state.profile = response.data;
    setStatus(REQUEST_STATUS.success);
    setSaveStatus(REQUEST_STATUS.success);
    return state.profile;
  } catch (error) {
    const message = normalizeErrorMessage(error, DEFAULT_SAVE_ERROR_MESSAGE);
    setSaveStatus(REQUEST_STATUS.error, message);
    throw error;
  }
};

const updateAvatar = async (file) => {
  setSaveStatus(REQUEST_STATUS.loading);

  try {
    await uploadUserAvatar(file);
    const profile = await ensureProfile({ force: true });
    if (!profile) {
      throw new Error(DEFAULT_SAVE_ERROR_MESSAGE);
    }
    setSaveStatus(REQUEST_STATUS.success);
    return profile;
  } catch (error) {
    const message = normalizeErrorMessage(error, DEFAULT_SAVE_ERROR_MESSAGE);
    setSaveStatus(REQUEST_STATUS.error, message);
    throw error;
  }
};

export const useProfileStore = () => ({
  state,
  ensureProfile,
  updateProfile,
  updateAvatar,
});
