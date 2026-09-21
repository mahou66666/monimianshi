import { reactive } from 'vue';

const DEFAULT_PROFILE_STATE = {
  avatar: '',
  name: '',
  gender: '',
  identity: '',
  gradYear: '',
  wechat: '',
  phone: '',
  birthday: '',
  email: '',
  desc: '',
};

export const userState = reactive({ ...DEFAULT_PROFILE_STATE });

const hasOwn = (target, key) => Object.prototype.hasOwnProperty.call(target, key);

const assignIfDefined = (targetKey, source, sourceKey) => {
  if (!hasOwn(source, sourceKey)) {
    return;
  }

  userState[targetKey] = source[sourceKey] ?? '';
};

export const syncFromRemoteProfile = (profile) => {
  if (!profile || typeof profile !== 'object') {
    return;
  }

  assignIfDefined('avatar', profile, 'avatarUrl');
  assignIfDefined('name', profile, 'nickname');
  assignIfDefined('gender', profile, 'gender');
  assignIfDefined('identity', profile, 'identity');
  assignIfDefined('gradYear', profile, 'gradYear');
  assignIfDefined('wechat', profile, 'wechat');
  assignIfDefined('phone', profile, 'phone');
  assignIfDefined('birthday', profile, 'birthday');
  assignIfDefined('email', profile, 'email');
  assignIfDefined('desc', profile, 'desc');
};

export const resetUserProfileState = () => {
  Object.assign(userState, DEFAULT_PROFILE_STATE);
};
