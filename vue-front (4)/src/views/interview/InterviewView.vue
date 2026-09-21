<template>
  <div class="interview-wrapper">
    <InterviewHeader
      :job-title="interviewState.meta.jobTitle"
      :status-text="interviewState.meta.statusText"
      :is-active="interviewStore.isStageActive()"
      @close="handleClose"
    />

    <div v-if="activeErrorBanner" class="error-banner">
      <div class="error-copy">
        <p class="error-title">{{ activeErrorBanner.title }}</p>
        <p class="error-message">{{ activeErrorBanner.message }}</p>
      </div>
      <button
        v-if="activeErrorBanner.actionLabel"
        class="error-action"
        type="button"
        @click="handleErrorAction"
      >
        {{ activeErrorBanner.actionLabel }}
      </button>
    </div>

    <div class="sprite-stage">
      <div class="ripple-container" :class="{ talking: interviewState.isTalking }">
        <div class="ripple ring-1"></div>
        <div class="ripple ring-2"></div>
        <div class="ripple ring-3"></div>
      </div>
      <div class="sprite-wrapper">
        <AgentSprite />
      </div>
    </div>

    <InterviewChat :chat-list="interviewState.chatList" />

    <InterviewControls
      :is-talking="interviewState.isTalking"
      :hint-text="interviewState.statusHint"
      :is-busy="interviewStore.isProcessing()"
      :pending-transcript="interviewState.pendingTranscript"
      @toggle-talk="interviewStore.handleVoiceAction"
      @hangup="handleClose"
      @send-text="interviewStore.sendTextMessage"
      @confirm-transcript="interviewStore.confirmPendingTranscript"
      @cancel-transcript="interviewStore.cancelPendingTranscript"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import AgentSprite from '@/components/common/AgentSprite.vue';
import { useInterviewStore } from '@/stores/interview';
import { REQUEST_STATUS } from '@/utils/constants';
import InterviewChat from './components/InterviewChat.vue';
import InterviewControls from './components/InterviewControls.vue';
import InterviewHeader from './components/InterviewHeader.vue';

const emit = defineEmits(['close']);

const router = useRouter();
const interviewStore = useInterviewStore();
const interviewState = interviewStore.state;
let preserveStoreOnUnmount = false;

const activeErrorBanner = computed(() => {
  if (interviewState.status === REQUEST_STATUS.error && interviewState.errorMessage) {
    const isPermissionIssue = interviewState.permissionState === 'denied';
    const isUnsupported = interviewState.permissionState === 'unsupported';

    return {
      title: isPermissionIssue ? '录音权限需要重新开启' : '本轮面试处理失败',
      message: interviewState.errorMessage,
      actionLabel: isUnsupported ? '' : isPermissionIssue ? '重新请求权限' : '再试一次',
      actionType: 'flow',
    };
  }

  if (interviewState.metaStatus === REQUEST_STATUS.error && interviewState.metaErrorMessage) {
    return {
      title: '面试信息加载失败',
      message: interviewState.metaErrorMessage,
      actionLabel: '重新加载',
      actionType: 'meta',
    };
  }

  return null;
});

const handleClose = () => {
  emit('close');
};

const handleErrorAction = () => {
  if (!activeErrorBanner.value) {
    return;
  }

  if (activeErrorBanner.value.actionType === 'meta') {
    interviewStore.loadMeta();
    return;
  }

  interviewStore.handleVoiceAction();
};

onMounted(() => {
  interviewStore.loadMeta();
});

watch(
  () => interviewState.interviewCompleted,
  (completed) => {
    if (!completed) {
      return;
    }
    preserveStoreOnUnmount = true;
    router.push({ name: 'interview-summary' });
  },
);

onBeforeUnmount(() => {
  if (!preserveStoreOnUnmount) {
    interviewStore.reset();
  }
});
</script>

<style scoped>
.interview-wrapper {
  display: grid;
  grid-template-areas: 'header' 'error' 'sprite' 'chat' 'controls';
  grid-template-rows: auto auto clamp(100px, 20dvh, 220px) minmax(140px, 1fr) auto;
  width: 100%;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  padding-inline: max(0px, calc((100% - 1200px) / 2));
  background-color: #2a2a2a;
  color: #fff;
  overflow: auto;
}

.header-nav { grid-area: header; }
.error-banner { grid-area: error; }
.sprite-stage { grid-area: sprite; }
.chat-area { grid-area: chat; }
.bottom-controls { grid-area: controls; }

@media (min-width: 1200px) {
  .interview-wrapper {
    grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
    grid-template-areas: 'header header' 'error error' 'sprite chat' 'sprite controls';
    grid-template-rows: auto auto minmax(180px, 1fr) auto;
  }
}

.error-banner {
  margin: 0 16px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(137, 46, 46, 0.22);
  border: 1px solid rgba(255, 114, 114, 0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.error-copy {
  min-width: 0;
}

.error-title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #ffd6d6;
}

.error-message {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.86);
}

.error-action {
  flex-shrink: 0;
  border: none;
  border-radius: 14px;
  background: #f4dd6a;
  color: #2a2a2a;
  font-size: 12px;
  font-weight: 700;
  padding: 10px 14px;
  cursor: pointer;
}

.sprite-stage {
  flex-shrink: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.ripple-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 200px;
  height: 200px;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.ripple-container.talking {
  opacity: 1;
}

.ripple {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border: 1px solid rgba(232, 206, 104, 0.6);
  border-radius: 50%;
  animation: ripple-anim 2.5s infinite linear;
}

.ring-1 {
  animation-delay: 0s;
}

.ring-2 {
  animation-delay: 0.8s;
}

.ring-3 {
  animation-delay: 1.6s;
}

@keyframes ripple-anim {
  0% {
    width: 140px;
    height: 140px;
    opacity: 1;
  }

  100% {
    width: 320px;
    height: 320px;
    opacity: 0;
  }
}

.sprite-wrapper {
  position: relative;
  z-index: 10;
  width: 180px;
  height: 160px;
}
</style>
