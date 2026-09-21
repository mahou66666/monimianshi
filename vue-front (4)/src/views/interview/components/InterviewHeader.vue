<template>
  <div class="header-nav">
    <button class="icon-btn" type="button" aria-label="关闭面试" @click="$emit('close')">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="svg-icon">
        <line x1="18" y1="6" x2="6" y2="18"></line>
        <line x1="6" y1="6" x2="18" y2="18"></line>
      </svg>
    </button>

    <div class="title-area">
      <div class="status-title">
        <span class="recording-dot" :class="{ active: isActive }"></span>
        {{ statusText }}
      </div>
      <div class="job-title">{{ jobTitle }}</div>
    </div>

    <div class="icon-btn ghost">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="svg-icon">
        <circle cx="12" cy="12" r="1"></circle>
        <circle cx="19" cy="12" r="1"></circle>
        <circle cx="5" cy="12" r="1"></circle>
      </svg>
    </div>
  </div>
</template>

<script setup>
defineProps({
  jobTitle: {
    type: String,
    default: '加载中...',
  },
  statusText: {
    type: String,
    default: '模拟面试进行中',
  },
  isActive: {
    type: Boolean,
    default: false,
  },
});

defineEmits(['close']);
</script>

<style scoped>
.header-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  flex-shrink: 0;
}

.icon-btn {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  background-color: #3f3f3f;
  border: none;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #ccc;
  cursor: pointer;
}

.icon-btn.ghost {
  cursor: default;
}

.svg-icon {
  width: 20px;
  height: 20px;
}

.title-area {
  text-align: center;
}

.status-title {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  gap: 6px;
}

.recording-dot {
  width: 8px;
  height: 8px;
  background-color: rgba(255, 77, 79, 0.35);
  border-radius: 50%;
  transition: all 0.2s ease;
}

.recording-dot.active {
  background-color: #ff4d4f;
  box-shadow: 0 0 8px rgba(255, 77, 79, 0.6);
}

.job-title {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}
.title-area { min-width: 0; overflow-wrap: anywhere; }
</style>
