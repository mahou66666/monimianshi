<template>
  <div class="auth-page" :class="`auth-page--${titleVariant}`">
    <div class="auth-panel">
      <button v-if="showBack" class="back-button" type="button" @click="$emit('back')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6"></path>
        </svg>
      </button>

      <div class="auth-header" :class="{ 'auth-header--with-back': showBack }">
        <h1 class="auth-title">{{ title }}</h1>

        <div class="brand-row">
          <span class="brand-dot"></span>
          <span class="brand-label">{{ brandLabel }}</span>
        </div>

        <p v-if="description" class="auth-description">{{ description }}</p>
      </div>

      <div class="auth-content">
        <slot />
      </div>

      <div v-if="showAgreement" class="agreement-card">
        登录/注册表示您同意
        <span class="agreement-link">《用户协议》</span>
        和
        <span class="agreement-link">《隐私政策》</span>
        <span class="agreement-dot"></span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
  brandLabel: {
    type: String,
    default: '面试精灵',
  },
  titleVariant: {
    type: String,
    default: 'hero',
  },
  showBack: {
    type: Boolean,
    default: true,
  },
  showAgreement: {
    type: Boolean,
    default: true,
  },
});

defineEmits(['back']);
</script>

<style scoped>
.auth-page {
  --auth-bg: #eaddd3;
  --auth-accent: #e5c858;
  --auth-accent-strong: #d7b74a;
  --auth-border: rgba(205, 182, 145, 0.72);
  --auth-text: #20291a;
  --auth-muted: #7a6f5f;
  --auth-shadow: rgba(141, 107, 53, 0.14);
  --auth-panel-padding-top: 22px;
  --auth-panel-padding-horizontal: 24px;
  --auth-panel-padding-bottom: 20px;
  --auth-title-size: clamp(50px, 13vw, 66px);
  --auth-title-line-height: 0.98;
  --auth-header-gap-top: 24px;
  --auth-brand-margin-top: 22px;
  --auth-brand-dot-size: 22px;
  --auth-brand-size: clamp(24px, 7vw, 31px);
  --auth-description-margin-top: 18px;
  --auth-description-size: 15px;
  --auth-content-margin-top: 24px;
  --auth-content-gap: 18px;
  --auth-mode-height: 60px;
  --auth-mode-font-size: 16px;
  --auth-field-height: 52px;
  --auth-field-radius: 16px;
  --auth-field-padding-left: 24px;
  --auth-field-padding-right: 18px;
  --auth-input-size: 15px;
  --auth-inline-height: 50px;
  --auth-inline-width: 146px;
  --auth-inline-size: 15px;
  --auth-link-size: 15px;
  --auth-primary-height: 52px;
  --auth-primary-size: 18px;
  --auth-secondary-height: 48px;
  --auth-secondary-size: 16px;
  --auth-agreement-font-size: 13px;
  --auth-agreement-padding-y: 15px;
  --auth-agreement-padding-x: 18px;

  flex: 1;
  min-height: 100%;
  padding: clamp(10px, 3vw, 32px);
  box-sizing: border-box;
  background-color: var(--auth-bg);
  background-image: radial-gradient(circle at 1px 1px, rgba(0, 0, 0, 0.04) 1px, transparent 0);
  background-size: 15px 15px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.auth-panel {
  width: 100%;
  max-width: 480px;
  min-width: 0;
  flex: 1;
  min-height: 0;
  padding:
    var(--auth-panel-padding-top)
    var(--auth-panel-padding-horizontal)
    var(--auth-panel-padding-bottom);
  box-sizing: border-box;
  border-radius: 38px;
  border: 1px solid var(--auth-border);
  background:
    radial-gradient(circle at top right, rgba(229, 200, 88, 0.42) 0, rgba(229, 200, 88, 0) 34%),
    radial-gradient(circle at bottom center, rgba(255, 239, 196, 0.58) 0, rgba(255, 239, 196, 0) 46%),
    linear-gradient(180deg, rgba(255, 253, 248, 0.99), rgba(252, 247, 237, 0.97));
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.7),
    0 24px 48px rgba(141, 107, 53, 0.14);
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.auth-panel::before,
.auth-panel::after {
  content: '';
  position: absolute;
  pointer-events: none;
}

.auth-panel::before {
  inset: 0;
  border-radius: inherit;
  box-shadow: inset 0 0 96px rgba(229, 200, 88, 0.08);
}

.auth-panel::after {
  right: 14px;
  bottom: 0;
  width: 42px;
  height: 68px;
  border-radius: 18px 18px 0 0;
  background: linear-gradient(180deg, rgba(255, 251, 244, 0.35), rgba(255, 255, 255, 0.86));
  transform: rotate(18deg) translateY(20px);
  opacity: 0.6;
}

.auth-panel > * {
  position: relative;
  z-index: 1;
}

.back-button {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  border: none;
  background: rgba(255, 250, 243, 0.92);
  color: #20291a;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 22px rgba(122, 93, 49, 0.1);
  cursor: pointer;
}

.back-button svg {
  width: 18px;
  height: 18px;
}

.auth-header {
  min-width: 0;
  flex-shrink: 0;
}

.auth-header--with-back {
  margin-top: var(--auth-header-gap-top);
}

.auth-title {
  margin: 0;
  font-size: var(--auth-title-size);
  line-height: var(--auth-title-line-height);
  color: var(--auth-text);
  font-weight: 900;
  letter-spacing: -0.05em;
  word-break: break-word;
}

.brand-row {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  margin-top: var(--auth-brand-margin-top);
  min-width: 0;
}

.brand-dot {
  width: var(--auth-brand-dot-size);
  height: var(--auth-brand-dot-size);
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #f3dd8a 0, #e5c858 58%, #c79f2f 100%);
  box-shadow: 0 8px 18px rgba(213, 170, 52, 0.32);
}

.brand-label {
  color: #6d5d3c;
  font-size: var(--auth-brand-size);
  font-weight: 800;
}

.auth-description {
  margin: var(--auth-description-margin-top) 0 0;
  max-width: 560px;
  color: #7e7362;
  font-size: var(--auth-description-size);
  line-height: 1.65;
}

.auth-content {
  margin-top: var(--auth-content-margin-top);
  display: flex;
  flex-direction: column;
  gap: var(--auth-content-gap);
  flex: 1;
  min-height: 0;
  min-width: 0;
  overflow: visible;
  overscroll-behavior: contain;
  padding-bottom: calc(28px + env(safe-area-inset-bottom, 0px));
  scrollbar-width: none;
}

@media (min-width: 768px) {
  .auth-page:has(.security-card) .auth-panel { max-width: 720px; }
}

.auth-content::-webkit-scrollbar {
  display: none;
}

.agreement-card {
  margin-top: 14px;
  border-radius: 22px;
  padding: var(--auth-agreement-padding-y) var(--auth-agreement-padding-x);
  background: rgba(255, 250, 244, 0.9);
  color: #7c715f;
  font-size: var(--auth-agreement-font-size);
  line-height: 1.55;
  text-align: center;
  box-shadow: 0 10px 22px rgba(126, 99, 59, 0.08);
}

.agreement-link {
  color: #433825;
  font-weight: 800;
}

.agreement-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e5c858;
  margin-left: 10px;
  vertical-align: middle;
}

:deep(.auth-form-stack) {
  display: flex;
  flex-direction: column;
  gap: var(--auth-content-gap);
}

:deep(.auth-mode-switch) {
  background: rgba(255, 250, 244, 0.92);
  border-radius: 999px;
  padding: 5px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5px;
  border: 1px solid var(--auth-border);
  box-shadow: 0 12px 24px rgba(126, 99, 59, 0.08);
}

:deep(.auth-mode-button),
:deep(.auth-primary-button),
:deep(.auth-secondary-button),
:deep(.auth-inline-button),
:deep(.auth-text-button) {
  border: none;
  cursor: pointer;
}

:deep(.auth-mode-button) {
  min-height: var(--auth-mode-height);
  border-radius: 999px;
  background: transparent;
  color: #5f5546;
  font-size: var(--auth-mode-font-size);
  font-weight: 800;
  padding: 0 16px;
  transition: background-color 0.18s ease, box-shadow 0.18s ease, color 0.18s ease;
}

:deep(.auth-mode-button.active) {
  background: linear-gradient(135deg, var(--auth-accent) 0%, var(--auth-accent-strong) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.55),
    0 8px 18px rgba(213, 170, 52, 0.2);
  color: #3b311d;
}

:deep(.auth-field) {
  width: 100%;
  box-sizing: border-box;
  border-radius: var(--auth-field-radius);
  background: rgba(248, 250, 231, 0.86);
  border: 1px solid rgba(220, 208, 184, 0.92);
  min-height: var(--auth-field-height);
  padding: 0 var(--auth-field-padding-right) 0 var(--auth-field-padding-left);
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 12px 24px rgba(126, 99, 59, 0.08);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

:deep(.auth-field:focus-within) {
  border-color: rgba(229, 200, 88, 0.95);
  box-shadow: 0 14px 28px rgba(229, 200, 88, 0.16);
}

:deep(.auth-field--with-action) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding-right: 12px;
}

:deep(.auth-code-field) {
  gap: 8px;
  padding: 4px 4px 4px var(--auth-field-padding-left);
  overflow: hidden;
}

:deep(.auth-input) {
  width: 100%;
  min-width: 0;
  border: none;
  background: transparent;
  outline: none;
  padding: 0;
  color: #39422f;
  font-size: var(--auth-input-size);
  font-weight: 600;
  font-family: inherit;
}

:deep(.auth-input::placeholder) {
  color: #b7ac9a;
  font-weight: 600;
}

:deep(.auth-field-icon) {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  color: #9b8657;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

:deep(.auth-field-icon svg) {
  width: 100%;
  height: 100%;
}

:deep(.auth-inline-button) {
  min-width: var(--auth-inline-width);
  height: var(--auth-inline-height);
  border-radius: 999px;
  background: linear-gradient(135deg, var(--auth-accent) 0%, var(--auth-accent-strong) 100%);
  color: #3b311d;
  font-size: var(--auth-inline-size);
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
  padding: 0 18px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.55),
    0 8px 18px rgba(213, 170, 52, 0.18);
}

:deep(.auth-code-button) {
  width: min(44%, 176px);
  min-width: 132px;
  height: calc(var(--auth-field-height) - 8px);
  border-radius: calc(var(--auth-field-radius) - 4px);
  padding: 0 16px;
  font-size: 14px;
  letter-spacing: 0.01em;
  justify-self: end;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.58),
    0 6px 14px rgba(213, 170, 52, 0.14);
}

:deep(.auth-inline-button:disabled) {
  opacity: 0.72;
  cursor: not-allowed;
}

:deep(.auth-code-button:disabled) {
  opacity: 1;
  color: rgba(59, 49, 29, 0.72);
}

:deep(.auth-link-row) {
  display: flex;
  align-items: center;
}

:deep(.auth-link-row--end) {
  justify-content: flex-end;
  margin-top: -2px;
}

:deep(.auth-text-button) {
  background: transparent;
  color: #6d6251;
  font-size: var(--auth-link-size);
  font-weight: 700;
  padding: 0;
}

:deep(.auth-feedback) {
  margin: 0 4px;
  color: #6d6251;
  font-size: 13px;
  line-height: 1.5;
}

:deep(.auth-feedback--error) {
  color: #a14b4b;
}

:deep(.auth-primary-button),
:deep(.auth-secondary-button) {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}

:deep(.auth-primary-button) {
  min-height: var(--auth-primary-height);
  border-radius: 28px;
  background: #232820;
  color: #fff;
  font-size: var(--auth-primary-size);
  box-shadow: 0 14px 24px rgba(35, 40, 32, 0.18);
}

:deep(.auth-primary-button:disabled) {
  opacity: 0.72;
  cursor: not-allowed;
}

:deep(.auth-secondary-button) {
  min-height: var(--auth-secondary-height);
  border-radius: 28px;
  background: rgba(255, 248, 240, 0.96);
  color: #524a3b;
  font-size: var(--auth-secondary-size);
  border: 1px solid rgba(198, 191, 184, 0.88);
  box-shadow: 0 12px 20px rgba(126, 99, 59, 0.08);
}

.auth-page--compact {
  --auth-header-gap-top: 12px;
  /* 放弃过大的 clamp 比例，改用更适合内页的固定或小范围字体 */
  --auth-title-size: 28px; 
  --auth-title-line-height: 1.2;
  --auth-brand-margin-top: 8px;
  --auth-brand-dot-size: 14px;
  --auth-brand-size: 16px; 
  --auth-description-margin-top: 8px;
  --auth-description-size: 13px;
  --auth-content-margin-top: 16px;
  --auth-content-gap: 12px;
}

@media (max-width: 420px) {
  .auth-page {
    padding: 10px;
  }

  .auth-panel {
    border-radius: 34px;
    padding: 16px 18px 16px;
  }

  .back-button {
    width: 46px;
    height: 46px;
    border-radius: 15px;
  }

  .auth-header--with-back {
    margin-top: 16px;
  }

  .auth-page {
    --auth-panel-padding-top: 18px;
    --auth-panel-padding-horizontal: 20px;
    --auth-panel-padding-bottom: 16px;
    --auth-title-size: clamp(46px, 12vw, 60px);
    --auth-header-gap-top: 18px;
    --auth-brand-margin-top: 18px;
    --auth-brand-dot-size: 20px;
    --auth-brand-size: clamp(22px, 6.6vw, 28px);
    --auth-description-margin-top: 14px;
    --auth-description-size: 13px;
    --auth-content-margin-top: 18px;
    --auth-content-gap: 14px;
    --auth-mode-height: 56px;
    --auth-mode-font-size: 15px;
    --auth-field-height: 76px;
    --auth-field-radius: 28px;
    --auth-field-padding-left: 20px;
    --auth-field-padding-right: 14px;
    --auth-input-size: 16px;
    --auth-inline-height: 46px;
    --auth-inline-width: 126px;
    --auth-inline-size: 14px;
    --auth-link-size: 14px;
    --auth-primary-height: 74px;
    --auth-primary-size: 20px;
    --auth-secondary-height: 68px;
    --auth-secondary-size: 18px;
    --auth-agreement-font-size: 12px;
    --auth-agreement-padding-y: 13px;
    --auth-agreement-padding-x: 15px;
  }

  :deep(.auth-code-button) {
    width: min(46%, 166px);
    min-width: 120px;
    font-size: 13px;
    padding: 0 12px;
  }

  .auth-page--compact {
    --auth-header-gap-top: 10px;
    --auth-title-size: 24px;
    --auth-title-line-height: 1.06;
    --auth-brand-margin-top: 8px;
    --auth-brand-dot-size: 12px;
    --auth-brand-size: 15px;
    --auth-content-gap: 10px;
    --auth-description-size: 12px;
  }
}
@media (max-width: 319px) {
  .auth-panel { padding-inline: 10px; min-width: 0; overflow-wrap: anywhere; }
  :deep(.auth-mode-switch) { grid-template-columns: minmax(0, 1fr); border-radius: 24px; }
  :deep(.auth-mode-button) { min-width: 0; }
  :deep(.auth-field--with-action) { grid-template-columns: minmax(0, 1fr); }
  :deep(.auth-code-field) { padding: 8px; }
  :deep(.auth-code-field .auth-input) { min-height: 40px; }
  :deep(.auth-code-button) { width: 100%; min-width: 0; white-space: normal; }
}
</style>
