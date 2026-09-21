<template>
  <header class="page-header">
    <button class="back-btn" @click="$emit('back')" aria-label="返回">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="15 18 9 12 15 6"></polyline>
      </svg>
    </button>

    <h1>{{ title }}</h1>

    <button
      v-if="action !== 'none'"
      class="action-btn"
      :class="{ filled: action === 'add' }"
      @click="$emit('action')"
      :aria-label="actionLabel"
    >
      <svg v-if="action === 'history'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path>
        <polyline points="3 3 3 8 8 8"></polyline>
        <path d="M12 7v5l2.5 2.5"></path>
      </svg>

      <svg v-else-if="action === 'download'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
        <polyline points="7 10 12 15 17 10"></polyline>
        <line x1="12" y1="15" x2="12" y2="3"></line>
      </svg>

      <svg v-else-if="action === 'add'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <line x1="12" y1="5" x2="12" y2="19"></line>
        <line x1="5" y1="12" x2="19" y2="12"></line>
      </svg>
    </button>

    <span v-else class="header-spacer"></span>
  </header>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  action: {
    type: String,
    default: 'none',
  },
  actionLabel: {
    type: String,
    default: '操作',
  },
});

defineEmits(['back', 'action']);
</script>

<style scoped>
.page-header {
  flex-shrink: 0;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0 18px;
}

.page-header h1 {
  min-width: 0;
  overflow-wrap: anywhere;
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #2c2c2c;
  flex: 1;
  text-align: center;
}

.back-btn,
.action-btn {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.back-btn {
  background: #ffffff;
  color: #333;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.action-btn {
  background: transparent;
  color: #a2a7af;
}

.action-btn.filled {
  background: #2c2c2c;
  color: #ffffff;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
}

.back-btn svg,
.action-btn svg {
  width: 18px;
  height: 18px;
}

.header-spacer {
  width: 38px;
}
</style>
