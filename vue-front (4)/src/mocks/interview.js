import { INTERVIEW_STATUS, MOCK_DELAY } from '@/utils/constants';

const wait = (delay) => new Promise((resolve) => setTimeout(resolve, delay));

const keywordReplyMap = [
  {
    keyword: '项目',
    reply: '这个项目经历很有代表性。你在项目中负责的核心模块是什么？遇到过最难的一次技术取舍又是怎么做的？',
  },
  {
    keyword: '性能',
    reply: '如果继续深挖性能优化，你会怎么量化优化前后的结果，并向团队说明这次优化的价值？',
  },
  {
    keyword: '团队',
    reply: '听起来你有不错的协作经验。那在和产品或后端意见不一致时，你通常怎么推动问题落地？',
  },
];

const voiceTurnScripts = [
  {
    userText: '大家好，我有三年前端开发经验，主要负责 Vue3 和 TypeScript 项目开发。',
    agentReplyText: '好的，请介绍一个你最有代表性的项目，并说明你负责的关键模块。',
  },
  {
    userText: '我负责过一个后台管理系统重构，重点做了组件拆分、权限路由和性能优化。',
    agentReplyText: '如果聚焦性能优化，你具体做过哪些措施？最终指标有怎样的改善？',
  },
  {
    userText: '我会先梳理页面瓶颈，再从请求合并、懒加载和渲染优化三个方向逐步推进。',
    agentReplyText: '这个回答方向很好。那你如何向面试官证明这些优化对业务结果真的有帮助？',
  },
];

const resolveReply = (content = '') => {
  const matchedReply = keywordReplyMap.find(({ keyword }) => content.includes(keyword));

  if (matchedReply) {
    return matchedReply.reply;
  }

  return '这个思路不错。继续往下说的话，你会如何拆分复杂模块边界，并保证状态流转清晰可维护？';
};

export const getInterviewMeta = async () => {
  await wait(MOCK_DELAY.fast);

  return {
    code: 200,
    data: {
      jobTitle: '前端开发工程师',
      statusText: INTERVIEW_STATUS.ongoing,
    },
  };
};

export const sendChatMessage = async (data = {}) => {
  await wait(MOCK_DELAY.slow);

  return {
    code: 200,
    data: {
      id: Date.now(),
      role: 'agent',
      content: resolveReply(data.content),
    },
  };
};

export const submitAudioTurn = async (formData) => {
  await wait(MOCK_DELAY.normal);

  const turnNo = Number(formData?.get?.('turnNo') ?? 1);
  const durationMs = Number(formData?.get?.('durationMs') ?? 0);
  const script = voiceTurnScripts[(turnNo - 1) % voiceTurnScripts.length];

  return {
    code: 200,
    data: {
      sessionId: 'mock-session-1',
      turnNo,
      durationMs,
      userText: script.userText,
      agentReplyText: script.agentReplyText,
      replyVoiceText: script.agentReplyText,
      interviewState: 'continue',
    },
  };
};
