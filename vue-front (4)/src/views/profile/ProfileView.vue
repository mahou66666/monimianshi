<template>
  <div class="profile-container">
    <div class="user-header-transparent clickable-card" @click="emit('open-user-info')">
      <div class="avatar-section">
        <div class="avatar-wrapper">
          <img v-if="userState.avatar" :src="userState.avatar" alt="用户头像" class="avatar-image" />
          <svg v-else viewBox="0 0 24 24" fill="currentColor" stroke="none" class="avatar-icon">
            <path d="M17.5 19C19.9853 19 22 16.9853 22 14.5C22 12.1325 20.1772 10.2017 17.8546 10.0152C17.3821 7.16726 14.9351 5 12 5C8.68629 5 6 7.68629 6 11C6 11.0506 6.00063 11.101 6.00188 11.1511C3.76016 11.4116 2 13.3138 2 15.6C2 17.4776 3.52243 19 5.4 19H17.5Z"></path>
          </svg>
        </div>
        <div class="user-info">
          <h2 class="user-name">
            <span class="user-name-text">{{ userState.name }}</span>
            <span class="vip-tag">学生账号</span>
          </h2>
          <p class="user-desc">{{ userState.identity }} | {{ userState.desc }}</p>
        </div>
      </div>
      <button class="edit-btn" type="button" aria-label="编辑个人信息">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
        </svg>
      </button>
    </div>

    <div class="action-grid">
      <div class="grid-card blue-card clickable-card" @click="emit('open-interview-history')">
        <div class="card-icon blue-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
            <polyline points="9 10 12 13 16 9"></polyline>
          </svg>
        </div>
        <div class="card-content">
          <h3>历史面试记录</h3>
          <p class="card-sub">复盘与成长</p>
        </div>
        <div class="bg-watermark">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
        </div>
      </div>

      <div class="grid-card yellow-card clickable-card" @click="emit('open-jd-history')">
        <div class="card-icon yellow-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <rect x="8" y="13" width="8" height="5" rx="1"></rect>
            <path d="M16 13v-1a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v1"></path>
          </svg>
        </div>
        <div class="card-content">
          <h3>历史JD记录</h3>
          <p class="card-sub">岗位匹配分析</p>
        </div>
        <div class="bg-watermark">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
          </svg>
        </div>
      </div>

      <div class="grid-card green-card clickable-card" @click="emit('open-resume-library')">
        <div class="card-icon green-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"></path>
            <polyline points="14.5 2 14.5 7.5 20 7.5"></polyline>
            <circle cx="10" cy="13" r="2.5"></circle>
            <path d="M14 19a4 4 0 0 0-8 0"></path>
          </svg>
        </div>
        <div class="card-content">
          <h3>我的简历</h3>
          <p class="card-sub">多版本管理</p>
        </div>
        <div class="bg-watermark">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"></path>
          </svg>
        </div>
      </div>

      <div class="grid-card dark-card clickable-card" @click="emit('open-settings')">
        <div class="card-icon dark-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="4" y1="21" x2="4" y2="14"></line>
            <line x1="4" y1="10" x2="4" y2="3"></line>
            <line x1="12" y1="21" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12" y2="3"></line>
            <line x1="20" y1="21" x2="20" y2="16"></line>
            <line x1="20" y1="12" x2="20" y2="3"></line>
            <line x1="1" y1="14" x2="7" y2="14"></line>
            <line x1="9" y1="8" x2="15" y2="8"></line>
            <line x1="17" y1="16" x2="23" y2="16"></line>
          </svg>
        </div>
        <div class="card-content">
          <h3>设置</h3>
          <p class="card-sub">账户与偏好</p>
        </div>
        <div class="bg-watermark">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="4" y1="21" x2="4" y2="14"></line>
            <line x1="4" y1="10" x2="4" y2="3"></line>
            <line x1="12" y1="21" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12" y2="3"></line>
            <line x1="20" y1="21" x2="20" y2="16"></line>
            <line x1="20" y1="12" x2="20" y2="3"></line>
            <line x1="1" y1="14" x2="7" y2="14"></line>
            <line x1="9" y1="8" x2="15" y2="8"></line>
            <line x1="17" y1="16" x2="23" y2="16"></line>
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useProfileStore } from '@/stores/profile';
import { syncFromRemoteProfile, userState } from '@/stores/userProfile';

const emit = defineEmits([
  'open-user-info',
  'open-settings',
  'open-interview-history',
  'open-jd-history',
  'open-resume-library',
]);
const profileStore = useProfileStore();

onMounted(async () => {
  const profile = await profileStore.ensureProfile();
  syncFromRemoteProfile(profile);
});
</script>

<style scoped>
.profile-container {
  width: 100%;
  max-width: 1200px;
  min-height: 100%;
  height: auto;
  margin: 0 auto;
  padding: 24px 20px;
  box-sizing: border-box;
  background-image: radial-gradient(circle at 1px 1px, rgba(0, 0, 0, 0.04) 1px, transparent 0);
  background-size: 15px 15px;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.user-header-transparent {
  min-width: 0;
  padding: 10px 4px 30px 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: opacity 0.2s;
}

.clickable-card {
  cursor: pointer;
}

.clickable-card:active {
  opacity: 0.8;
}

.avatar-section {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-wrapper {
  width: 60px;
  height: 60px;
  background-color: #e2cd6d;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-icon {
  width: 30px;
  height: 30px;
}

.user-info {
  min-width: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  min-width: 0;
  overflow: hidden;
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #222;
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name-text {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vip-tag {
  flex-shrink: 0;
  background-color: #2c2c2c;
  color: #e2cd6d;
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 8px;
  font-weight: 700;
}

.user-desc {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin: 0;
  font-size: 12px;
  color: #888;
}

.edit-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border: none;
  background-color: rgba(0, 0, 0, 0.05);
  color: #666;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.action-grid {
  min-width: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.grid-card {
  height: 150px;
  border-radius: 24px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.03);
  transition: transform 0.2s;
}

.grid-card:active {
  transform: scale(0.97);
}

.blue-card {
  background-color: #f0f7f8;
}

.yellow-card {
  background-color: #fef9e7;
}

.green-card {
  background-color: #f2f6ee;
}

.dark-card {
  background-color: #f5f5f5;
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: auto;
  position: relative;
  z-index: 2;
}

.card-icon svg {
  width: 22px;
  height: 22px;
}

.blue-icon {
  background-color: #729ba3;
}

.yellow-icon {
  background-color: #e2cd6d;
}

.green-icon {
  background-color: #8da372;
}

.dark-icon {
  background-color: #3f3f3f;
}

.card-content {
  position: relative;
  z-index: 2;
}

.grid-card h3 {
  margin: 0 0 4px 0;
  font-size: 16px;
  color: #333;
  font-weight: 800;
}

.card-sub {
  margin: 0;
  font-size: 11px;
  color: #a0a0a0;
}

.bg-watermark {
  position: absolute;
  right: -10px;
  bottom: -10px;
  width: 80px;
  height: 80px;
  color: rgba(0, 0, 0, 0.03);
  z-index: 1;
}

.bg-watermark svg {
  width: 100%;
  height: 100%;
}

@media (min-width: 1200px) {
  .profile-container {
    display: grid;
    grid-template-columns: minmax(280px, 0.8fr) minmax(0, 1.2fr);
    align-content: start;
    gap: 24px;
  }

  .user-header-transparent {
    grid-column: 1;
  }

  .action-grid {
    grid-column: 2;
    grid-row: 1;
  }
}
@media (max-width: 319px) {
  .action-grid { grid-template-columns: minmax(0, 1fr); }
  .grid-card { height: auto; min-height: 150px; overflow-wrap: anywhere; }
}
</style>
