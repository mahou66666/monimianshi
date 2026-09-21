<template>
  <div class="settings-container">
    <div class="top-nav">
      <div class="back-btn" @click="emit('back')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-svg">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
      </div>
      <div class="nav-title">设置</div>
      <div class="placeholder-box"></div>
    </div>

    <div class="settings-content">
      <div class="settings-card">
        <div class="list-row border-bottom" @click="emit('open-account-security')">
          <span class="row-label">账户与安全</span>
          <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="arrow-svg">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
        <div class="list-row">
          <span class="row-label">消息通知</span>
          <label class="toggle-switch">
            <input v-model="notificationsEnabled" type="checkbox" />
            <span class="slider"></span>
          </label>
        </div>
      </div>

      <div class="settings-card">
        <div class="list-row border-bottom" @click="clearCache">
          <span class="row-label">清除缓存</span>
          <span class="row-value">{{ cacheSize }}</span>
        </div>
        <div class="list-row border-bottom">
          <span class="row-label">意见反馈</span>
          <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="arrow-svg">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
        <div class="list-row">
          <span class="row-label">关于我们</span>
          <span class="row-value">v1.0.2</span>
        </div>
      </div>

      <div class="settings-card logout-card" @click="handleLogout">
        <span class="logout-text">退出登录</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const emit = defineEmits(['back', 'open-account-security']);

const notificationsEnabled = ref(true);
const cacheSize = ref('12.5MB');

const clearCache = () => {
  cacheSize.value = '0.0MB';
};

const handleLogout = () => {
  console.log('执行退出登录逻辑...');
};
</script>

<style scoped>
.settings-container {
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

.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 4px 24px 4px;
  flex-shrink: 0;
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

.settings-content {
  min-width: 0;
  min-height: 0;
  width: 100%;
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 40px;
}

.settings-content::-webkit-scrollbar {
  display: none;
}

.settings-card {
  width: 100%;
  box-sizing: border-box;
  background-color: #ffffff;
  border-radius: 20px;
  padding: 0 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}

.list-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  cursor: pointer;
}

.border-bottom {
  border-bottom: 1px solid #f8f8f8;
}

.row-label {
  font-size: 15px;
  color: #222;
}

.row-value {
  font-size: 14px;
  color: #999;
}

.arrow-svg {
  width: 16px;
  height: 16px;
}

.logout-card {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 18px 0;
  cursor: pointer;
  margin-top: 8px;
}

.logout-card:active {
  background-color: #fafafa;
}

.logout-text {
  font-size: 16px;
  color: #ff4d4f;
  font-weight: 700;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 26px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: #e5e5e5;
  transition: 0.3s;
  border-radius: 26px;
}

.slider:before {
  position: absolute;
  content: '';
  height: 22px;
  width: 22px;
  left: 2px;
  bottom: 2px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

input:checked + .slider {
  background-color: #222;
}

input:checked + .slider:before {
  transform: translateX(22px);
}
</style>
