import { reactive } from 'vue';
import { getHomeContent } from '@/api/home';
import { REQUEST_STATUS } from '@/utils/constants';

const DEFAULT_ERROR_MESSAGE = '首页内容加载失败，请稍后重试';

const DEFAULT_ASSISTANT_CARD = Object.freeze({
  tag: '沉浸式对练模式',
  title: '面试助手',
  subtitle: 'AGENT',
  actionLabel: '开始模拟面试',
});

const state = reactive({
  assistantCard: { ...DEFAULT_ASSISTANT_CARD },
  status: REQUEST_STATUS.idle,
  errorMessage: '',
});

let pendingHomePromise = null;

const normalizeErrorMessage = (error, fallback = DEFAULT_ERROR_MESSAGE) =>
  error?.message || error?.payload?.message || fallback;

const setStatus = (status, errorMessage = '') => {
  state.status = status;
  state.errorMessage = errorMessage;
};

const ensureHomeContent = async ({ force = false } = {}) => {
  if (!force && state.status === REQUEST_STATUS.success) {
    return state.assistantCard;
  }

  if (!force && pendingHomePromise) {
    return pendingHomePromise;
  }

  setStatus(REQUEST_STATUS.loading);

  pendingHomePromise = getHomeContent()
    .then((response) => {
      state.assistantCard = {
        ...DEFAULT_ASSISTANT_CARD,
        ...response.data?.assistantCard,
      };
      setStatus(REQUEST_STATUS.success);
      return state.assistantCard;
    })
    .catch((error) => {
      setStatus(REQUEST_STATUS.error, normalizeErrorMessage(error));
      return null;
    })
    .finally(() => {
      pendingHomePromise = null;
    });

  return pendingHomePromise;
};

export const useHomeStore = () => ({
  state,
  ensureHomeContent,
});
