<template>
  <AuthShell title="登录" title-variant="compact" @back="$emit('back')">
    <div class="auth-form-stack login-form-stack">
      <div class="auth-mode-switch">
        <button
          type="button"
          class="auth-mode-button"
          :class="{ active: accountState.loginMode === 'code' }"
          @click="accountStore.setLoginMode('code')"
        >
          验证码登录
        </button>
        <button
          type="button"
          class="auth-mode-button"
          :class="{ active: accountState.loginMode === 'password' }"
          @click="accountStore.setLoginMode('password')"
        >
          密码登录
        </button>
      </div>

      <label class="auth-field">
        <input
          :value="accountState.forms.login.phone"
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

      <label v-if="accountState.loginMode === 'code'" class="auth-field auth-field--with-action auth-code-field">
        <input
          :value="accountState.forms.login.code"
          class="auth-input"
          type="text"
          placeholder="输入验证码"
          maxlength="6"
          @input="updateField('code', $event.target.value)"
        />
        <button
          class="auth-inline-button auth-code-button"
          type="button"
          :disabled="isCodeButtonDisabled('login')"
          @click="handleSendCode"
        >
          {{ codeButtonText('login') }}
        </button>
      </label>

      <label v-else class="auth-field">
        <input
          :value="accountState.forms.login.password"
          class="auth-input"
          type="password"
          placeholder="输入密码"
          @input="updateField('password', $event.target.value)"
        />
      </label>

      <div class="auth-link-row auth-link-row--end">
        <button class="auth-text-button" type="button" @click="$emit('open-reset')">忘记密码?</button>
      </div>

      <p v-if="activeFeedback" class="auth-feedback" :class="{ 'auth-feedback--error': hasError }">
        {{ activeFeedback }}
      </p>

      <button class="auth-primary-button" type="button" :disabled="isSubmitting" @click="handleLogin">
        {{ isSubmitting ? '登录中...' : '登录' }}
      </button>

      <button class="auth-secondary-button" type="button" @click="$emit('open-register')">注册</button>
    </div>
  </AuthShell>
</template>

<script setup>
import { computed } from 'vue';
import { REQUEST_STATUS } from '@/utils/constants';
import { useAccountStore } from '@/stores/account';
import AuthShell from './components/AuthShell.vue';

const emit = defineEmits(['back', 'open-register', 'open-reset', 'logged-in']);

const accountStore = useAccountStore();
const accountState = accountStore.state;

const isSubmitting = computed(() => accountState.submitStatus.login === REQUEST_STATUS.loading);
const hasError = computed(() => accountState.submitStatus.login === REQUEST_STATUS.error);
const activeFeedback = computed(
  () => accountState.submitErrorMessage.login || accountState.feedbackMessage.login || accountState.sendCodeErrorMessage.login,
);

const updateField = (field, value) => {
  accountStore.updateField('login', field, value);
};

const codeButtonText = (scene) => {
  if (accountState.sendCodeStatus[scene] === REQUEST_STATUS.loading) {
    return '发送中';
  }

  if (accountState.countdowns[scene] > 0) {
    return `${accountState.countdowns[scene]}s`;
  }

  return '获取验证码';
};

const isCodeButtonDisabled = (scene) =>
  accountState.sendCodeStatus[scene] === REQUEST_STATUS.loading || accountState.countdowns[scene] > 0;

const handleSendCode = async () => {
  try {
    await accountStore.sendCode('login');
  } catch (error) {
    console.error('发送登录验证码失败', error);
  }
};

const handleLogin = async () => {
  try {
    await accountStore.submitLogin();
    emit('logged-in');
  } catch (error) {
    console.error('登录失败', error);
  }
};
</script>

<style scoped>
.login-form-stack :deep(.auth-mode-switch) {
  width: 100%;
  box-sizing: border-box;
  padding: 4px;
}

.login-form-stack :deep(.auth-mode-button) {
  min-height: 52px;
  font-size: 15px;
}

@media (max-width: 420px) {
  .login-form-stack :deep(.auth-mode-button) {
    min-height: 48px;
    font-size: 14px;
  }
}
</style>
