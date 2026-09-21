<template>
  <div class="chat-area" ref="chatContainerRef">
    <div
      v-for="message in chatList"
      :key="message.id"
      class="message-row"
      :class="message.role === 'user' ? 'user-row' : 'agent-row'"
    >
      <div v-if="message.role === 'agent'" class="message-label" :class="{
        'analysis-label': message.source === 'round-feedback',
      }">
        {{ message.source === 'round-feedback' ? '本轮分析' : 'AI 面试官' }}
      </div>
      <div v-else-if="message.source === 'voice'" class="message-label user-label">语音转写</div>

      <div class="bubble" :class="message.role === 'user' ? 'green-bubble' : 'dark-bubble'">
        {{ message.content }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref, watch } from 'vue';

const props = defineProps({
  chatList: {
    type: Array,
    required: true,
  },
});

const chatContainerRef = ref(null);

const scrollToBottom = async () => {
  await nextTick();

  if (chatContainerRef.value) {
    chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight;
  }
};

watch(
  () => props.chatList,
  () => {
    scrollToBottom();
  },
  { deep: true },
);

onMounted(() => {
  scrollToBottom();
});
</script>

<style scoped>
.chat-area {
  min-height: 0;
  min-width: 0;
  flex: 1;
  padding: 0 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 20px;
  overflow-y: auto;
  scrollbar-width: none;
}

.chat-area::-webkit-scrollbar {
  display: none;
}

.message-row {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.user-row {
  align-items: flex-end;
}

.agent-row {
  align-items: flex-start;
}

.message-label {
  font-size: 11px;
  color: #888;
  margin-bottom: 6px;
  margin-left: 4px;
}

.user-label {
  margin-right: 4px;
  margin-left: 0;
}

.analysis-label {
  color: #cdb95f;
}

.bubble {
  box-sizing: border-box;
  overflow-wrap: anywhere;
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  max-width: 85%;
  word-break: break-word;
}

.green-bubble {
  background-color: #8da372;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.dark-bubble {
  background-color: #3f3f3f;
  color: #e0e0e0;
  border-bottom-left-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
</style>
