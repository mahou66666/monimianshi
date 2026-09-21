<template>
  <header class="page-header">
    <button
      class="back-btn"
      :class="{ elevated: elevatedBackButton }"
      @click="$emit('back')"
      aria-label="返回"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="15 18 9 12 15 6"></polyline>
      </svg>
    </button>

    <h1>{{ title }}</h1>

    <button v-if="showDelete" class="delete-btn" @click="$emit('action')" aria-label="删除">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="3 6 5 6 21 6"></polyline>
        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
        <line x1="10" y1="11" x2="10" y2="17"></line>
        <line x1="14" y1="11" x2="14" y2="17"></line>
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
  showDelete: {
    type: Boolean,
    default: false,
  },
  elevatedBackButton: {
    type: Boolean,
    default: false,
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
  padding: 6px 0 16px;
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

.back-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: #ffffff;
  color: #333;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.back-btn.elevated {
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.back-btn svg {
  width: 18px;
  height: 18px;
}

.delete-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #a2a7af;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-shrink: 0;
  padding: 0;
}

.delete-btn svg {
  width: 22px;
  height: 22px;
}

.header-spacer {
  width: 34px;
}
</style>
