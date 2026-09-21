import { MOCK_DELAY } from '@/utils/constants';

const wait = (delay) => new Promise((resolve) => setTimeout(resolve, delay));

export const getHomeContent = async () => {
  await wait(MOCK_DELAY.fast);

  return {
    code: 200,
    data: {
      assistantCard: {
        tag: '沉浸式对练模式',
        title: '面试助手',
        subtitle: 'AGENT',
        actionLabel: '开始模拟面试',
      },
    },
  };
};
