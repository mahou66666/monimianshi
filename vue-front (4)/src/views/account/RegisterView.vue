<template>
  <AuthShell
    title="注册"
    description="先用手机号完成账号创建，后续可以继续补充更多个人资料和安全设置。"
    @back="$emit('back')"
  >
    <div class="auth-form-stack">
      <label class="auth-field">
        <input
          :value="accountState.forms.register.phone"
          class="auth-input"
          type="tel"
          placeholder="输入手机号"
          maxlength="11"
          @input="updateField('phone', $event.target.value)"
        />
      </label>

      <label class="auth-field auth-field--with-action">
        <input
          :value="accountState.forms.register.code"
          class="auth-input"
          type="text"
          placeholder="输入验证码"
          maxlength="6"
          @input="updateField('code', $event.target.value)"
        />
        <button
          class="auth-inline-button"
          type="button"
          :disabled="isCodeButtonDisabled"
          @click="handleSendCode"
        >
          {{ codeButtonText }}
        </button>
      </label>

      <label class="auth-field">
        <input
          :value="accountState.forms.register.password"
          class="auth-input"
          type="password"
          placeholder="设置密码"
          @input="updateField('password', $event.target.value)"
        />
      </label>

      <label class="auth-field">
        <input
          :value="accountState.forms.register.confirmPassword"
          class="auth-input"
          type="password"
          placeholder="确认密码"
          @input="updateField('confirmPassword', $event.target.value)"
        />
      </label>

      <p v-if="activeFeedback" class="auth-feedback" :class="{ 'auth-feedback--error': hasError }">
        {{ activeFeedback }}
      </p>

      <button class="auth-primary-button" type="button" :disabled="isSubmitting" @click="handleRegister">
        {{ isSubmitting ? '注册中...' : '注册' }}
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

const emit = defineEmits(['back', 'open-login', 'registered']);

const accountStore = useAccountStore();
const accountState = accountStore.state;

const isSubmitting = computed(() => accountState.submitStatus.register === REQUEST_STATUS.loading);
const hasError = computed(() => accountState.submitStatus.register === REQUEST_STATUS.error);
const activeFeedback = computed(
  () =>
    accountState.submitErrorMessage.register ||
    accountState.feedbackMessage.register ||
    accountState.sendCodeErrorMessage.register,
);

const codeButtonText = computed(() => {
  if (accountState.sendCodeStatus.register === REQUEST_STATUS.loading) {
    return '发送中';
  }

  if (accountState.countdowns.register > 0) {
    return `${accountState.countdowns.register}s`;
  }

  return '获取验证码';
});

const isCodeButtonDisabled = computed(
  () =>
    accountState.sendCodeStatus.register === REQUEST_STATUS.loading ||
    accountState.countdowns.register > 0,
);

const updateField = (field, value) => {
  accountStore.updateField('register', field, value);
};

const handleSendCode = async () => {
  try {
    await accountStore.sendCode('register');
  } catch (error) {
    console.error('发送注册验证码失败', error);
  }
};

const handleRegister = async () => {
  try {
    await accountStore.submitRegister();
    emit('registered');
  } catch (error) {
    console.error('注册失败', error);
  }
};
</script>
