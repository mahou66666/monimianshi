<template>
  <nav class="bottom-nav" aria-label="主导航">
    <button
      type="button"
      class="nav-item"
      :class="{ active: currentTab === 'home' }"
      aria-label="首页"
      :aria-current="currentTab === 'home' ? 'page' : undefined"
      @click="switchTab('home')"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        class="nav-svg"
        aria-hidden="true"
      >
        <rect x="3" y="3" width="7" height="7"></rect>
        <rect x="14" y="3" width="7" height="7"></rect>
        <rect x="14" y="14" width="7" height="7"></rect>
        <rect x="3" y="14" width="7" height="7"></rect>
      </svg>
      <span class="nav-label">首页</span>
    </button>

    <button
      type="button"
      class="nav-item"
      :class="{ active: currentTab === 'upload' }"
      aria-label="上传"
      :aria-current="currentTab === 'upload' ? 'page' : undefined"
      @click="switchTab('upload')"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        class="nav-svg"
        aria-hidden="true"
      >
        <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" />
      </svg>
      <span class="nav-label">上传</span>
    </button>

    <button
      type="button"
      class="nav-item"
      :class="{ active: currentTab === 'profile' }"
      aria-label="我的"
      :aria-current="currentTab === 'profile' ? 'page' : undefined"
      @click="switchTab('profile')"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        class="nav-svg"
        aria-hidden="true"
      >
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
        <circle cx="12" cy="7" r="4"></circle>
      </svg>
      <span class="nav-label">我的</span>
    </button>
  </nav>
</template>

<script setup>
const props = defineProps({
  currentTab: {
    type: String,
    default: 'home',
  },
});

const emit = defineEmits(['change']);

const switchTab = (tabName) => {
  if (props.currentTab === tabName) {
    return;
  }

  emit('change', tabName);
};
</script>

<style scoped>
.bottom-nav {
  width: 100%;
  flex-shrink: 0;
  background-color: var(--shell-background, #eaddd3);
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px calc(12px + env(safe-area-inset-bottom, 0px));
  box-sizing: border-box;
  border-top: 1px solid rgba(0, 0, 0, 0.03);
}

.nav-item {
  width: 72px;
  height: 58px;
  flex: 0 1 72px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 0;
  border: 0;
  color: #a0a0a0;
  background: transparent;
  border-radius: 16px;
  cursor: pointer;
  font: inherit;
  transition: all 0.3s ease;
}

.nav-svg {
  width: 24px;
  height: 24px;
}

.nav-label {
  font-size: 12px;
  line-height: 1;
}

.nav-item.active {
  background-color: white;
  color: #2c2c2c;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

@media (min-width: 768px) {
  .bottom-nav {
    order: -1;
    width: auto;
    align-self: center;
    margin: 16px auto 0;
    padding: 8px;
    border: 1px solid rgba(0, 0, 0, 0.03);
    border-radius: 24px;
  }

  .nav-item {
    width: 84px;
  }
}
</style>
