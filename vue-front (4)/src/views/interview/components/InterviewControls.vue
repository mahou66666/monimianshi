<template>
  <div class="bottom-controls">
    <div class="hint-text">
      {{ hintText }}
    </div>

    <div v-if="pendingTranscript" class="transcript-review">
      <p class="review-title">Review transcription before send</p>
      <textarea
        v-model="pendingTranscriptDraft"
        class="review-textarea"
        :disabled="isBusy"
      />
      <div class="review-actions">
        <button class="review-btn review-cancel" :disabled="isBusy" @click="handleDiscardTranscript">
          Discard
        </button>
        <button
          class="review-btn review-confirm"
          :disabled="!pendingTranscriptDraft.trim() || isBusy"
          @click="handleConfirmTranscript"
        >
          Confirm & Send
        </button>
      </div>
    </div>

    <div class="action-bar" v-if="!isTextMode && !pendingTranscript">
      <button class="side-btn" aria-label="切换文字输入" :disabled="isBusy && !isTalking" @click="isTextMode = true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="4" width="20" height="16" rx="2" ry="2"></rect>
          <line x1="6" y1="8" x2="6.01" y2="8"></line>
          <line x1="10" y1="8" x2="10.01" y2="8"></line>
          <line x1="14" y1="8" x2="14.01" y2="8"></line>
          <line x1="18" y1="8" x2="18.01" y2="8"></line>
          <line x1="8" y1="12" x2="8.01" y2="12"></line>
          <line x1="12" y1="12" x2="12.01" y2="12"></line>
          <line x1="16" y1="12" x2="16.01" y2="12"></line>
          <line x1="7" y1="16" x2="17" y2="16"></line>
        </svg>
      </button>

      <button
        class="mic-btn"
        :aria-label="isTalking ? '停止录音' : '开始录音'"
        :class="{ active: isTalking, busy: isBusy && !isTalking }"
        :disabled="isBusy && !isTalking"
        @click="$emit('toggle-talk')"
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          class="mic-icon"
        >
          <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
          <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
          <line x1="12" y1="19" x2="12" y2="23"></line>
          <line x1="8" y1="23" x2="16" y2="23"></line>
        </svg>
      </button>

      <button class="side-btn hangup-btn" aria-label="结束面试" @click="$emit('hangup')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18.36 6.64a9 9 0 1 1-12.73 0"></path>
          <line x1="12" y1="2" x2="12" y2="12"></line>
        </svg>
      </button>
    </div>

    <div class="text-input-bar" v-else-if="!pendingTranscript">
      <button class="side-btn" aria-label="切换语音输入" :disabled="isBusy" @click="isTextMode = false">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
          <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
          <line x1="12" y1="19" x2="12" y2="23"></line>
          <line x1="8" y1="23" x2="16" y2="23"></line>
        </svg>
      </button>

      <input
        v-model="inputText"
        type="text"
        class="custom-input"
        placeholder="输入你想说的内容..."
        :disabled="isBusy"
        @keyup.enter="handleSend"
      />

      <button class="send-btn" aria-label="发送回答" @click="handleSend" :disabled="!inputText.trim() || isBusy">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13"></line>
          <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  isTalking: {
    type: Boolean,
    default: false,
  },
  hintText: {
    type: String,
    default: '点击麦克风开始录音',
  },
  isBusy: {
    type: Boolean,
    default: false,
  },
  pendingTranscript: {
    type: String,
    default: '',
  },
});

const emit = defineEmits([
  'toggle-talk',
  'hangup',
  'send-text',
  'confirm-transcript',
  'cancel-transcript',
]);

const isTextMode = ref(false);
const inputText = ref('');
const pendingTranscriptDraft = ref('');

const handleSend = () => {
  if (!inputText.value.trim()) {
    return;
  }

  emit('send-text', inputText.value.trim());
  inputText.value = '';
};

const handleConfirmTranscript = () => {
  if (!pendingTranscriptDraft.value.trim()) {
    return;
  }
  emit('confirm-transcript', pendingTranscriptDraft.value.trim());
};

const handleDiscardTranscript = () => {
  pendingTranscriptDraft.value = '';
  emit('cancel-transcript');
};

watch(
  () => props.pendingTranscript,
  (value) => {
    pendingTranscriptDraft.value = String(value || '');
  },
  { immediate: true },
);
</script>

<style scoped>
.bottom-controls {
  min-width: 0;
  padding: 10px 16px calc(20px + env(safe-area-inset-bottom, 0px));
  overflow-wrap: anywhere;
  text-align: center;
  flex-shrink: 0;
  min-height: 120px;
  box-sizing: border-box;
}

.transcript-review {
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: left;
}

.review-title {
  margin: 0;
  font-size: 12px;
  color: #c9c9c9;
}

.review-textarea {
  box-sizing: border-box;
  max-height: 35dvh;
  width: 100%;
  min-height: 76px;
  resize: vertical;
  border: 1px solid #4f4f4f;
  border-radius: 10px;
  background: #3a3a3a;
  color: #fff;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.45;
  outline: none;
}

.review-textarea:focus {
  border-color: #8da372;
}

.review-actions {
  flex-wrap: wrap;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.review-btn {
  border: none;
  border-radius: 10px;
  padding: 9px 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.review-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.review-cancel {
  background: #4f4f4f;
  color: #ddd;
}

.review-confirm {
  background: #8da372;
  color: #fff;
}

.hint-text {
  font-size: 12px;
  color: #999;
  margin-bottom: 16px;
  transition: all 0.3s;
  min-height: 16px;
}

.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: space-around;
  align-items: center;
}

.side-btn {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border: none;
  background: transparent;
  color: #888;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
}

.side-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.side-btn svg {
  width: 24px;
  height: 24px;
}

.hangup-btn {
  color: #ff4d4f;
}

.mic-btn {
  flex-shrink: 0;
  width: 76px;
  height: 76px;
  background-color: #fff;
  border-radius: 50%;
  border: none;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.mic-btn.active {
  background: #e8ce68;
}

.mic-btn.busy {
  background: #f0f0f0;
}

.mic-btn:disabled {
  cursor: not-allowed;
  opacity: 0.8;
}

.mic-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.mic-icon {
  width: 32px;
  height: 32px;
  color: #2c2c2c;
}

.text-input-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.custom-input {
  width: 0;
  min-width: 0;
  flex: 1;
  background-color: #3f3f3f;
  border: 1px solid #4f4f4f;
  color: #fff;
  height: 44px;
  border-radius: 22px;
  padding: 0 16px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.custom-input:focus {
  border-color: #8da372;
}

.custom-input::placeholder {
  color: #888;
}

.send-btn {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  background-color: #8da372;
  color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
}

.send-btn:disabled {
  background-color: #4f4f4f;
  color: #888;
  cursor: not-allowed;
}

.send-btn svg {
  width: 18px;
  height: 18px;
  margin-right: 2px;
  margin-top: 2px;
}
@media (max-width: 319px) {
  .text-input-bar { flex-wrap: wrap; }
  .custom-input { flex-basis: 100%; box-sizing: border-box; }
}
</style>
