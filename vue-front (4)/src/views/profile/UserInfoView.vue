<template>
  <div class="user-info-container">
    <input
      ref="avatarInput"
      type="file"
      accept="image/*"
      class="hidden-input"
      @change="handleAvatarSelected"
    />

    <div class="top-nav">
      <div class="back-btn" @click="emit('back')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-svg">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
      </div>
      <div class="nav-title">个人信息</div>
      <div class="placeholder-box"></div>
    </div>

    <p v-if="saveHint" class="save-hint" :class="{ 'save-hint--error': hasSaveError }">
      {{ saveHint }}
    </p>

    <div class="info-card-wrapper">
      <div class="info-list">
        <div class="list-row border-bottom clickable-row" :class="{ disabled: isSaving }" @click="triggerAvatarUpload">
          <span class="row-label">头像</span>
          <div class="row-value-group">
            <div class="avatar-circle">
              <img v-if="userState.avatar" :src="userState.avatar" alt="用户头像" class="real-avatar" />
              <svg v-else viewBox="0 0 24 24" fill="currentColor" stroke="none" class="avatar-svg">
                <path d="M17.5 19C19.9853 19 22 16.9853 22 14.5C22 12.1325 20.1772 10.2017 17.8546 10.0152C17.3821 7.16726 14.9351 5 12 5C8.68629 5 6 7.68629 6 11C6 11.0506 6.00063 11.101 6.00188 11.1511C3.76016 11.4116 2 13.3138 2 15.6C2 17.4776 3.52243 19 5.4 19H17.5Z"></path>
              </svg>
            </div>
            <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="arrow-svg">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
        </div>

        <div class="list-row border-bottom clickable-row" :class="{ disabled: isSaving }" @click="openModal('name')">
          <span class="row-label">姓名</span>
          <div class="row-value-group">
            <span class="row-value">{{ displayOrUnset(userState.name) }}</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="arrow-svg">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
        </div>

        <div class="list-row border-bottom clickable-row" :class="{ disabled: isSaving }" @click="openModal('gender')">
          <span class="row-label">性别</span>
          <div class="row-value-group">
            <span class="row-value">{{ displayOrUnset(userState.gender) }}</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="arrow-svg">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
        </div>

        <div class="list-row border-bottom clickable-row" :class="{ disabled: isSaving }" @click="openModal('identity')">
          <span class="row-label">我的身份</span>
          <div class="row-value-group">
            <span class="row-value">{{ displayOrUnset(userState.identity) }}</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="arrow-svg">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
        </div>

        <div class="list-row border-bottom clickable-row" :class="{ disabled: isSaving }" @click="openModal('gradYear')">
          <span class="row-label">毕业年份</span>
          <div class="row-value-group">
            <span class="row-value">{{ displayOrUnset(userState.gradYear) }}</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="arrow-svg">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
        </div>

        <div class="list-row border-bottom clickable-row" :class="{ disabled: isSaving }" @click="openModal('wechat')">
          <span class="row-label">微信号</span>
          <div class="row-value-group">
            <span class="row-value">{{ displayOrUnset(userState.wechat) }}</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="arrow-svg">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
        </div>

        <div class="list-row border-bottom clickable-row" :class="{ disabled: isSaving }" @click="openModal('phone')">
          <span class="row-label">手机号</span>
          <div class="row-value-group">
            <span class="row-value">{{ displayOrUnset(userState.phone) }}</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="arrow-svg">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
        </div>

        <div class="list-row clickable-row" :class="{ disabled: isSaving }" @click="openModal('birthday')">
          <span class="row-label">出生日期</span>
          <div class="row-value-group">
            <span class="row-value">{{ displayOrUnset(userState.birthday) }}</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="arrow-svg">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <transition name="fade">
      <div v-if="activeModal" class="modal-overlay" @click.self="closeModal">
        <transition name="zoom">
          <div v-if="activeModal" class="modal-content">
            <div class="modal-header">
              <span class="modal-cancel" @click="closeModal">取消</span>
              <h3 class="modal-title">{{ modalTitle }}</h3>
              <span class="modal-confirm" @click="confirmModal">确定</span>
            </div>

            <div class="modal-body">
              <div v-if="activeModal === 'name'" class="input-group">
                <input v-model="tempValue" type="text" class="modal-input" placeholder="请输入姓名" />
              </div>

              <div v-else-if="['gender', 'identity', 'gradYear'].includes(activeModal)" class="radio-list">
                <div
                  v-for="option in currentOptions"
                  :key="option"
                  class="radio-item"
                  @click="tempValue = option"
                >
                  <span>{{ option }}</span>
                  <svg v-if="tempValue === option" viewBox="0 0 24 24" fill="none" stroke="#8da372" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="check-svg">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                </div>
              </div>

              <div v-else-if="activeModal === 'birthday'" class="input-group">
                <input v-model="tempValue" type="date" class="modal-input native-date-picker" />
              </div>

              <div v-else class="input-group">
                <input
                  v-model="tempValue"
                  type="text"
                  class="modal-input"
                  :placeholder="activeModal === 'phone' ? '请输入新的手机号' : '请输入微信号（可留空）'"
                />
              </div>

              <p v-if="saveError" class="modal-error">{{ saveError }}</p>
            </div>
          </div>
        </transition>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useProfileStore } from '@/stores/profile';
import { syncFromRemoteProfile, userState } from '@/stores/userProfile';
import { REQUEST_STATUS } from '@/utils/constants';

const emit = defineEmits(['back']);
const profileStore = useProfileStore();

const avatarInput = ref(null);
const activeModal = ref('');
const tempValue = ref('');
const saveError = ref('');

const currentYear = new Date().getFullYear();
const gradYearOptions = ['未设置', ...Array.from({ length: 6 }, (_, index) => `${currentYear + index}年`), '其他'];

const optionsMap = {
  gender: ['未设置', '男', '女'],
  identity: ['未设置', '学生', '求职者', '职场人'],
  gradYear: gradYearOptions,
};

const modalTitleMap = {
  name: '修改姓名',
  gender: '选择性别',
  identity: '选择身份',
  gradYear: '选择毕业年份',
  phone: '修改手机号',
  wechat: '修改微信号',
  birthday: '选择出生日期',
};

const payloadKeyMap = {
  name: 'nickname',
  gender: 'gender',
  identity: 'identity',
  gradYear: 'gradYear',
  phone: 'phone',
  wechat: 'wechat',
  birthday: 'birthday',
};

const isSaving = computed(() => profileStore.state.saveStatus === REQUEST_STATUS.loading);
const hasSaveError = computed(() => profileStore.state.saveStatus === REQUEST_STATUS.error);

const saveHint = computed(() => {
  if (profileStore.state.saveStatus === REQUEST_STATUS.loading) {
    return '正在保存...';
  }
  if (profileStore.state.saveStatus === REQUEST_STATUS.error) {
    return profileStore.state.saveErrorMessage || '保存失败，请重试';
  }
  if (profileStore.state.saveStatus === REQUEST_STATUS.success) {
    return '已保存到数据库';
  }
  return '';
});

const modalTitle = computed(() => modalTitleMap[activeModal.value] || '');
const currentOptions = computed(() => optionsMap[activeModal.value] || []);

const displayOrUnset = (value) => {
  if (typeof value !== 'string') {
    return value || '未设置';
  }
  return value.trim() ? value : '未设置';
};

const normalizeValue = (field, value) => {
  const nextValue = (value ?? '').trim();

  if (['gender', 'identity', 'gradYear'].includes(field) && nextValue === '未设置') {
    return '';
  }

  return nextValue;
};

const validateField = (field, value) => {
  if (field === 'name') {
    if (!value) {
      return '姓名不能为空';
    }
    if (value.length > 50) {
      return '姓名不能超过 50 个字符';
    }
    return '';
  }

  if (field === 'phone') {
    if (!/^1\d{10}$/.test(value)) {
      return '请输入正确的 11 位手机号';
    }
    return '';
  }

  if (field === 'wechat' && value.length > 25) {
    return '微信号不能超过 25 个字符';
  }

  if (field === 'gradYear' && value.length > 20) {
    return '毕业年份格式不正确';
  }

  if (field === 'birthday' && value) {
    const birthday = new Date(`${value}T00:00:00`);
    if (Number.isNaN(birthday.getTime())) {
      return '出生日期格式不正确';
    }
    if (birthday > new Date()) {
      return '出生日期不能晚于今天';
    }
  }

  return '';
};

const triggerAvatarUpload = () => {
  if (isSaving.value) {
    return;
  }
  avatarInput.value?.click();
};

const handleAvatarSelected = async (event) => {
  const file = event.target.files?.[0];
  event.target.value = '';

  if (!file || isSaving.value) {
    return;
  }

  if (!file.type.startsWith('image/')) {
    window.alert('请选择图片文件');
    return;
  }

  if (file.size > 2 * 1024 * 1024) {
    window.alert('头像大小不能超过 2MB');
    return;
  }

  try {
    const profile = await profileStore.updateAvatar(file);
    syncFromRemoteProfile(profile);
  } catch (error) {
    window.alert(profileStore.state.saveErrorMessage || error?.message || '头像上传失败');
  }
};

const openModal = (field) => {
  if (isSaving.value) {
    return;
  }

  tempValue.value = userState[field] || '';
  activeModal.value = field;
  saveError.value = '';
};

const closeModal = () => {
  activeModal.value = '';
  saveError.value = '';
};

const confirmModal = async () => {
  const field = activeModal.value;
  if (!field || !payloadKeyMap[field] || isSaving.value) {
    return;
  }

  const normalized = normalizeValue(field, tempValue.value);
  const message = validateField(field, normalized);
  if (message) {
    saveError.value = message;
    return;
  }

  const payload = {
    [payloadKeyMap[field]]: normalized,
  };

  try {
    const profile = await profileStore.updateProfile(payload);
    syncFromRemoteProfile(profile);
    closeModal();
  } catch (error) {
    saveError.value = profileStore.state.saveErrorMessage || error?.message || '保存失败';
  }
};

onMounted(async () => {
  const profile = await profileStore.ensureProfile();
  syncFromRemoteProfile(profile);
});
</script>

<style scoped>
.user-info-container {
  width: 100%;
  max-width: 720px;
  min-height: 100%;
  height: auto;
  margin: 0 auto;
  padding: 16px;
  box-sizing: border-box;
  background-image: radial-gradient(circle at 1px 1px, rgba(0, 0, 0, 0.04) 1px, transparent 0);
  background-size: 15px 15px;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.hidden-input {
  display: none;
}

.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 4px 16px 4px;
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  cursor: pointer;
  color: #333;
}

.icon-svg {
  width: 24px;
  height: 24px;
}

.nav-title {
  font-size: 18px;
  font-weight: 800;
  color: #222;
}

.placeholder-box {
  width: 36px;
}

.save-hint {
  margin: 0 6px 10px;
  font-size: 12px;
  color: #4b6b38;
}

.save-hint--error {
  color: #cb4a4a;
}

.info-card-wrapper {
  min-width: 0;
  min-height: 0;
  background-color: #ffffff;
  border-radius: 24px;
  padding: 10px 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  margin-bottom: 20px;
}

.info-card-wrapper::-webkit-scrollbar {
  display: none;
}

.info-list {
  min-height: 100%;
}

.list-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 0;
}

.border-bottom {
  border-bottom: 1px solid #fcfcfc;
}

.row-label {
  font-size: 15px;
  color: #222;
}

.row-value-group {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.row-value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  color: #888;
  max-width: 180px;
  text-align: right;
}

.arrow-svg {
  width: 16px;
  height: 16px;
}

.clickable-row {
  cursor: pointer;
  transition: opacity 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.clickable-row:active {
  opacity: 0.6;
}

.disabled {
  opacity: 0.6;
  pointer-events: none;
}

.avatar-circle {
  width: 44px;
  height: 44px;
  background-color: #f7e4a1;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7b6ba1;
  overflow: hidden;
}

.avatar-svg {
  width: 24px;
  height: 24px;
}

.real-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  box-sizing: border-box;
  padding: 16px;
  background-color: rgba(0, 0, 0, 0.4);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
}

.modal-content {
  width: min(85%, 520px);
  max-height: calc(100dvh - 32px);
  box-sizing: border-box;
  overflow-y: auto;
  margin: auto;
  background-color: #ffffff;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.modal-title {
  margin: 0;
  font-size: 17px;
  font-weight: 800;
  color: #222;
}

.modal-cancel {
  font-size: 15px;
  color: #999;
  cursor: pointer;
}

.modal-confirm {
  font-size: 15px;
  color: #8da372;
  font-weight: 700;
  cursor: pointer;
}

.input-group {
  background-color: #f5f5f5;
  border-radius: 12px;
  padding: 4px 16px;
}

.modal-input {
  width: 100%;
  border: none;
  background: transparent;
  padding: 12px 0;
  font-size: 16px;
  color: #333;
  outline: none;
}

.native-date-picker {
  font-family: inherit;
}

.radio-list {
  display: flex;
  flex-direction: column;
}

.radio-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 16px;
  color: #333;
  cursor: pointer;
}

.radio-item:last-child {
  border-bottom: none;
}

.check-svg {
  width: 20px;
  height: 20px;
}

.modal-error {
  margin: 12px 0 0;
  color: #cb4a4a;
  font-size: 13px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.zoom-enter-active,
.zoom-leave-active {
  transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.zoom-enter-from,
.zoom-leave-to {
  transform: scale(0.9);
  opacity: 0;
}
</style>
