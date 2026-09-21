<template>
  <AuthShell
    title="重置密码"
    title-variant="compact"
    description="通过手机号重置密码，验证码 5 分钟内有效。"
    @back="$emit('back')"
  >
    <div class="auth-form-stack">
      <label class="auth-field">
        <input
          :value="accountState.forms.reset.phone"
          class="auth-input"
          type="tel"
          placeholder="输入手机号"
          maxlength="11"
          @input="updateField('phone', $event.target.value)"
        />
        <span class="auth-field-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">
            <rect x="7" y="2.5" width="10" height="19" rx="2.5"></rect>
            <line x1="10.5" y1="18" x2="13.5" y2="18"></line>
          </svg>
        </span>
      </label>

      <label class="auth-field auth-field--with-action auth-code-field">
        <input
          :value="accountState.forms.reset.code"
          class="auth-input"
          type="text"
          placeholder="输入验证码"
          maxlength="6"
          @input="updateField('code', $event.target.value)"
        />
        <button
          class="auth-inline-button auth-code-button"
          type="button"
          :disabled="isCodeButtonDisabled"
          @click="handleSendCode"
        >
          {{ codeButtonText }}
        </button>
      </label>

      <label class="auth-field">
        <input
          :value="accountState.forms.reset.password"
          class="auth-input"
          type="password"
          placeholder="设置新密码"
          @input="updateField('password', $event.target.value)"
        />
      </label>

      <label class="auth-field">
        <input
          :value="accountState.forms.reset.confirmPassword"
          class="auth-input"
          type="password"
          placeholder="确认新密码"
          @input="updateField('confirmPassword', $event.target.value)"
        />
      </label>

      <p v-if="activeFeedback" class="auth-feedback" :class="{ 'auth-feedback--error': hasError }">
        {{ activeFeedback }}
      </p>

      <button class="auth-primary-button" type="button" :disabled="isSubmitting" @click="handleReset">
        {{ isSubmitting ? '重置中...' : '重置密码' }}
      </button>

      <button class="auth-secondary-button" type="button" @click="$emit('open-login')">返回登录</button>
    </div>
  </AuthShell>
</template>

<script setup>
import { computed } from 'vue';
import { REQUEST_STATUS } from '@/utils/constants';
import { useAccountStore } from '@/stores/account';
import AuthShell from './components/AuthShell.vue';

const emit = defineEmits(['back', 'open-login', 'reset-success']);

const accountStore = useAccountStore();
const accountState = accountStore.state;

const isSubmitting = computed(() => accountState.submitStatus.reset === REQUEST_STATUS.loading);
const hasError = computed(() => accountState.submitStatus.reset === REQUEST_STATUS.error);
const activeFeedback = computed(
  () =>
    accountState.submitErrorMessage.reset ||
    accountState.feedbackMessage.reset ||
    accountState.sendCodeErrorMessage.reset,
);

const codeButtonText = computed(() => {
  if (accountState.sendCodeStatus.reset === REQUEST_STATUS.loading) {
    return '发送中';
  }

  if (accountState.countdowns.reset > 0) {
    return `${accountState.countdowns.reset}s`;
  }

  return '获取验证码';
});

const isCodeButtonDisabled = computed(
  () =>
    accountState.sendCodeStatus.reset === REQUEST_STATUS.loading ||
    accountState.countdowns.reset > 0,
);

const updateField = (field, value) => {
  accountStore.updateField('reset', field, value);
};

const handleSendCode = async () => {
  try {
    await accountStore.sendCode('reset');
  } catch (error) {
    console.error('发送重置密码验证码失败', error);
  }
};

const handleReset = async () => {
  try {
    await accountStore.submitResetPassword();
    emit('reset-success');
  } catch (error) {
    console.error('重置密码失败', error);
  }
};
</script>
