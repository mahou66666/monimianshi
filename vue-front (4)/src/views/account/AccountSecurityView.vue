<template>
  <AuthShell
    title="账户与安全"
    description="集中查看手机号、登录方式和密码状态，这里也作为后续设置、绑定和退出登录的承载页。"
    title-variant="compact"
    :show-agreement="false"
    @back="$emit('back')"
  >
    <div v-if="accountState.securityStatus === REQUEST_STATUS.idle" class="security-card">
      <p class="section-eyebrow">账户信息</p>
      <h3 class="section-title">准备同步你的安全设置</h3>
      <p class="section-copy">点击下方按钮后，会拉取登录方式、最近登录时间和已绑定的联系方式。</p>
      <button class="primary-button" type="button" @click="reloadSecurity">立即加载</button>
    </div>

    <div v-else-if="accountState.securityStatus === REQUEST_STATUS.loading" class="security-card">
      <p class="section-eyebrow">同步中</p>
      <h3 class="section-title">正在获取账户安全信息</h3>
      <div class="loading-stack">
        <div class="loading-line loading-line--wide"></div>
        <div class="loading-line loading-line--medium"></div>
        <div class="loading-line loading-line--short"></div>
      </div>
    </div>

    <div
      v-else-if="accountState.securityStatus === REQUEST_STATUS.error"
      class="security-card security-card--error"
    >
      <p class="section-eyebrow">加载失败</p>
      <h3 class="section-title">账户安全信息暂时不可用</h3>
      <p class="section-copy">{{ accountState.securityErrorMessage }}</p>
      <button class="primary-button" type="button" @click="reloadSecurity">重新加载</button>
    </div>

    <div v-else-if="accountState.securityInfo" class="security-layout">
      <section class="security-card summary-card">
        <div class="summary-row">
          <div>
            <p class="section-eyebrow">账号总览</p>
            <h3 class="section-title">{{ accountState.securityInfo.maskedPhone }}</h3>
          </div>
          <span class="status-chip">安全状态正常</span>
        </div>

        <div class="summary-grid">
          <div class="summary-item">
            <span class="summary-label">绑定邮箱</span>
            <span class="summary-value">{{ accountState.securityInfo.email }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">最近登录</span>
            <span class="summary-value">{{ accountState.securityInfo.lastLoginAt }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">密码更新时间</span>
            <span class="summary-value">{{ accountState.securityInfo.passwordUpdatedAt }}</span>
          </div>
        </div>
      </section>

      <section class="security-card">
        <p class="section-eyebrow">登录方式</p>
        <div class="method-tags">
          <span
            v-for="method in accountState.securityInfo.loginMethods"
            :key="method.key"
            class="method-tag"
          >
            {{ method.label }}
          </span>
        </div>
      </section>

      <section class="security-card">
        <p class="section-eyebrow">安全建议</p>
        <div class="tips-list">
          <div
            v-for="tip in accountState.securityInfo.securityTips"
            :key="tip"
            class="tip-item"
          >
            <span class="tip-dot"></span>
            <span>{{ tip }}</span>
          </div>
        </div>
      </section>

      <section class="security-card action-card">
        <button class="action-row" type="button" @click="$emit('open-reset')">
          <span>重置密码</span>
          <span class="action-arrow">→</span>
        </button>
        <button class="action-row" type="button" @click="$emit('open-login')">
          <span>返回登录</span>
          <span class="action-arrow">→</span>
        </button>
      </section>

      <p v-if="logoutFeedback" class="feedback" :class="{ 'feedback--error': logoutHasError }">
        {{ logoutFeedback }}
      </p>

      <button class="primary-button" type="button" :disabled="isLoggingOut" @click="handleLogout">
        {{ isLoggingOut ? '退出中...' : '退出登录' }}
      </button>
    </div>
  </AuthShell>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { REQUEST_STATUS } from '@/utils/constants';
import { useAccountStore } from '@/stores/account';
import AuthShell from './components/AuthShell.vue';

const emit = defineEmits(['back', 'open-reset', 'open-login', 'logged-out']);

const accountStore = useAccountStore();
const accountState = accountStore.state;

const isLoggingOut = computed(() => accountState.submitStatus.logout === REQUEST_STATUS.loading);
const logoutHasError = computed(() => accountState.submitStatus.logout === REQUEST_STATUS.error);
const logoutFeedback = computed(
  () => accountState.submitErrorMessage.logout || accountState.feedbackMessage.logout,
);

const reloadSecurity = () => {
  accountStore.ensureSecurityInfo({ force: true });
};

const handleLogout = async () => {
  try {
    await accountStore.submitLogout();
    emit('logged-out');
  } catch (error) {
    console.error('退出登录失败', error);
  }
};

onMounted(async () => {
  try {
    await accountStore.ensureSecurityInfo({ force: true });
  } catch (error) {
    console.error('加载账号安全信息失败', error);
  }
});
</script>
<style scoped>
.security-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.security-card,
.primary-button,
.action-row {
  border: none;
}

/* 缂╁皬鍗＄墖鐨勫渾瑙掑拰鍐呰竟璺?*/
.security-card {
  border-radius: 16px; /* 鍘?22px */
  padding: 14px; /* 鍘?16px */
  background: rgba(255, 250, 244, 0.92);
  border: 1px solid rgba(220, 208, 194, 0.88);
  box-shadow: 0 8px 16px rgba(124, 93, 62, 0.08);
}

.security-card--error {
  border: 1px solid rgba(198, 74, 74, 0.16);
}

.section-eyebrow {
  margin: 0 0 8px;
  color: #9c8872;
  font-size: 11px;
  letter-spacing: 0.08em;
}

/* 缂╁皬鍖哄潡涓绘爣棰?*/
.section-title {
  margin: 0;
  color: #3a2d22;
  font-size: 18px; /* 鍘?22px */
  line-height: 1.2;
}

.section-copy {
  margin: 8px 0 0;
  color: #8a7a67;
  font-size: 13px;
  line-height: 1.55;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.status-chip {
  height: fit-content;
  border-radius: 999px;
  background: rgba(229, 200, 88, 0.2);
  color: #6d553d;
  font-size: 11px;
  font-weight: 800;
  padding: 7px 10px;
  flex-shrink: 0;
}

.summary-grid {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.summary-item {
  border-radius: 12px;
  padding: 10px 12px;
  background: rgba(255, 247, 238, 0.96);
  border: 1px solid rgba(226, 214, 198, 0.8);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.summary-label {
  color: #9a8876;
  font-size: 13px;
}

.summary-value {
  color: #3f3025;
  font-size: 14px;
  font-weight: 700;
  text-align: right;
  word-break: break-word;
}

.method-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.method-tag {
  padding: 9px 14px;
  border-radius: 999px;
  background: rgba(242, 228, 207, 0.92);
  color: #5f4631;
  border: 1px solid rgba(222, 205, 183, 0.85);
  font-size: 14px;
  font-weight: 800;
}

.tips-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6f6150;
  font-size: 14px;
  line-height: 1.45;
}

.tip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d3b38a;
  flex-shrink: 0;
}

.action-card {
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-row {
  width: 100%;
  border-radius: 12px;
  background: rgba(248, 239, 229, 0.96);
  color: #4d3b2a;
  border: 1px solid rgba(220, 207, 192, 0.86);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.action-arrow {
  font-size: 20px;
  line-height: 1;
}

.feedback {
  margin: 0 2px;
  color: #776655;
  font-size: 13px;
  line-height: 1.45;
}

.feedback--error {
  color: #a14b4b;
}

/* 淇敼椤甸潰鑷甫鐨勬彁浜ゆ寜閽珮搴?*/
.primary-button {
  width: 100%;
  border-radius: 24px;
  background: linear-gradient(180deg, #8c725f, #78614f);
  color: #fff;
  min-height: 48px; /* 鍘?62px锛屾瀬澶у噺灏戜簡楂樺害 */
  font-size: 16px; /* 鍘?18px */
  font-weight: 800;
  box-shadow: 0 6px 16px rgba(104, 76, 50, 0.18);
  cursor: pointer;
}

.primary-button:disabled {
  opacity: 0.72;
  cursor: not-allowed;
}

.loading-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.loading-line {
  height: 14px;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    rgba(235, 225, 214, 0.92),
    rgba(250, 246, 240, 1),
    rgba(235, 225, 214, 0.92)
  );
  background-size: 180% 100%;
  animation: shimmer 1.2s infinite linear;
}

.loading-line--wide {
  width: 100%;
}

.loading-line--medium {
  width: 74%;
}

.loading-line--short {
  width: 48%;
}

@media (max-width: 360px) {
  .summary-row,
  .summary-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-value {
    text-align: left;
  }
}

@media (max-width: 420px) {
  .security-layout {
    gap: 10px;
  }

  .security-card {
    padding: 14px;
    border-radius: 20px;
  }

  .section-eyebrow {
    margin-bottom: 6px;
  }

  .section-title {
    font-size: 20px;
  }

  .section-copy,
  .tip-item,
  .summary-label,
  .summary-value {
    font-size: 13px;
  }

  .summary-grid {
    gap: 8px;
    margin-top: 10px;
  }

  .summary-item {
    padding: 11px 12px;
  }

  .method-tag {
    font-size: 13px;
    padding: 8px 12px;
  }

  .action-row {
    padding: 13px 14px;
    font-size: 15px;
  }

  .primary-button {
    min-height: 58px;
    border-radius: 20px;
    font-size: 17px;
  }
}

@keyframes shimmer {
  0% {
    background-position: 100% 0;
  }

  100% {
    background-position: -100% 0;
  }
}
</style>


