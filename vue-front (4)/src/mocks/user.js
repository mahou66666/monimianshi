import { MOCK_DELAY } from '@/utils/constants';

const wait = (delay) => new Promise((resolve) => setTimeout(resolve, delay));

const profileState = {
  userId: 'u1001',
  nickname: '张三',
  avatarUrl: '',
  phone: '13800000000',
  email: 'test@example.com',
  gender: '男',
  identity: '学生',
  gradYear: '2027年',
  wechat: '',
  birthday: '2000-01-01',
  desc: '熟悉 Vue.js 与 Spring Boot',
  stats: {
    resumeCount: 5,
    jdAnalysisCount: 3,
    interviewCount: 6,
  },
};

export const getUserProfile = async () => {
  await wait(MOCK_DELAY.fast);

  return {
    code: 200,
    data: { ...profileState },
  };
};

export const updateUserProfile = async (payload = {}) => {
  await wait(MOCK_DELAY.fast);

  if (payload.nickname !== undefined) {
    profileState.nickname = payload.nickname || '';
  }
  if (payload.avatarUrl !== undefined) {
    profileState.avatarUrl = payload.avatarUrl || '';
  }
  if (payload.phone !== undefined) {
    profileState.phone = payload.phone || '';
  }
  if (payload.gender !== undefined) {
    profileState.gender = payload.gender || '';
  }
  if (payload.identity !== undefined) {
    profileState.identity = payload.identity || '';
  }
  if (payload.gradYear !== undefined) {
    profileState.gradYear = payload.gradYear || '';
  }
  if (payload.wechat !== undefined) {
    profileState.wechat = payload.wechat || '';
  }
  if (payload.birthday !== undefined) {
    profileState.birthday = payload.birthday || '';
  }

  return {
    code: 200,
    data: { ...profileState },
  };
};

export const uploadUserAvatar = async () => {
  await wait(MOCK_DELAY.fast);

  profileState.avatarUrl = `https://dummyimage.com/120x120/e2cd6d/ffffff&text=A-${Date.now()}`;

  return {
    code: 200,
    data: {
      avatarUrl: profileState.avatarUrl,
    },
  };
};
