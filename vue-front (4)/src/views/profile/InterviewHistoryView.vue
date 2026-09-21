<template>
  <div class="history-page">
    <ResumePageHeader title="历史面试记录" action="none" @back="$emit('back')" />

    <section v-if="historyList.length === 0" class="empty-card">
      <h2>暂无面试记录</h2>
      <p>完成面试后，系统会自动记录在这里，支持查看该轮对话内容。</p>
    </section>

    <section v-else class="list">
      <article
        v-for="item in historyList"
        :key="item.id"
        class="item-card"
        role="button"
        tabindex="0"
        @click="openDetail(item)"
        @keydown.enter.prevent="openDetail(item)"
      >
        <div class="item-head">
          <h3>{{ item.targetIndustry || 'Mock Interview' }}</h3>
          <span class="time">{{ formatDateTime(item.createdAt) }}</span>
        </div>
        <p class="meta">
          <span>状态：{{ item.status || 'completed' }}</span>
          <span>轮次：{{ item.questionCount || 0 }} / {{ item.maxQuestions || 0 }}</span>
          <span v-if="item.score !== null && item.score !== undefined">得分：{{ item.score }}</span>
        </p>
        <p class="summary">{{ item.summaryText || '本轮暂无总结文本。' }}</p>
        <p class="action-hint">点击查看对话详情</p>
      </article>
    </section>

    <div v-if="selectedItem" class="detail-mask" @click.self="closeDetail">
      <section class="detail-panel">
        <header class="detail-head">
          <div class="detail-title-wrap">
            <h3>{{ selectedItem.targetIndustry || 'Mock Interview' }}</h3>
            <p>{{ formatDateTime(selectedItem.createdAt) }}</p>
          </div>
          <button type="button" class="close-btn" @click="closeDetail">关闭</button>
        </header>

        <div class="detail-body">
          <template v-if="selectedDialogue.length">
            <article
              v-for="(turn, index) in selectedDialogue"
              :key="`${selectedItem.id}-${index}`"
              class="turn"
              :class="turn.role === 'user' ? 'turn-user' : 'turn-agent'"
            >
              <p class="turn-role">{{ turn.role === 'user' ? '你' : 'AI 面试官' }}</p>
              <p class="turn-content">{{ turn.content }}</p>
            </article>
          </template>

          <p v-else class="empty-dialogue">该历史记录暂无逐轮对话内容（旧记录可能未保存）。</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import ResumePageHeader from '@/views/resume/components/ResumePageHeader.vue';
import { useInterviewStore } from '@/stores/interview';
import { formatDateTime } from '@/utils/format';

defineEmits(['back']);

const interviewStore = useInterviewStore();
const selectedItem = ref(null);

const historyList = computed(() =>
  interviewStore
    .getHistoryRecords()
    .slice()
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
);

const selectedDialogue = computed(() => {
  const current = selectedItem.value;
  if (!current) {
    return [];
  }

  const rows = Array.isArray(current.dialogue) ? current.dialogue : [];
  const normalizedRows = rows.filter((item) => {
    const role = String(item?.role || '').trim();
    const content = String(item?.content || '').trim();
    return (role === 'user' || role === 'agent') && Boolean(content);
  });

  if (normalizedRows.length) {
    return normalizedRows;
  }

  const fallbackSummary = String(current.summaryText || '').trim();
  if (fallbackSummary) {
    return [{ role: 'agent', content: fallbackSummary }];
  }

  return [];
});

const openDetail = (item) => {
  selectedItem.value = item || null;
};

const closeDetail = () => {
  selectedItem.value = null;
};
</script>

<style scoped>
.history-page {
  width: 100%;
  max-width: 1200px;
  min-height: 100%;
  height: auto;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  padding: 10px 16px 24px;
  box-sizing: border-box;
  overflow-y: auto;
  background: #eaddd3;
}

.empty-card {
  margin-top: 16px;
  padding: 24px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.72);
}

.empty-card h2 {
  margin: 0 0 10px;
  font-size: 18px;
  color: #2f2f2f;
}

.empty-card p {
  margin: 0;
  color: #666;
  font-size: 14px;
  line-height: 1.7;
}

.list {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
}

.item-card {
  min-width: 0;
  border-radius: 18px;
  background: #fff;
  padding: 14px 14px 12px;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.06);
  cursor: pointer;
}

.item-head {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.item-head h3 {
  min-width: 0;
  flex: 1 1 12rem;
  margin: 0;
  font-size: 15px;
  color: #242424;
  overflow-wrap: anywhere;
}

.time {
  min-width: 0;
  max-width: 100%;
  flex: 0 1 auto;
  font-size: 12px;
  color: #888;
  overflow-wrap: anywhere;
  white-space: normal;
}

.meta {
  margin: 8px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: #6e6e6e;
  overflow-wrap: anywhere;
}

.summary {
  overflow-wrap: anywhere;
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #4f4f4f;
}

.action-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #8a8a8a;
}

.detail-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  box-sizing: border-box;
  z-index: 50;
  overflow: auto;
}

.detail-panel {
  width: min(100%, 800px);
  max-height: calc(100dvh - 32px);
  min-height: 0;
  box-sizing: border-box;
  border-radius: 20px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-head {
  min-width: 0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 14px 12px;
  border-bottom: 1px solid #efefef;
  background: #fafafa;
}

.detail-title-wrap {
  min-width: 0;
  flex: 1 1 auto;
}

.detail-title-wrap h3 {
  min-width: 0;
  overflow-wrap: anywhere;
  margin: 0;
  font-size: 16px;
  color: #232323;
}

.detail-title-wrap p {
  overflow-wrap: anywhere;
  margin: 2px 0 0;
  font-size: 12px;
  color: #7c7c7c;
}

.close-btn {
  flex-shrink: 0;
  border: none;
  border-radius: 10px;
  padding: 8px 12px;
  font-size: 12px;
  color: #333;
  background: #ececec;
  cursor: pointer;
}

.detail-body {
  min-height: 0;
  flex: 1 1 auto;
  padding: 12px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.turn {
  min-width: 0;
  border-radius: 12px;
  padding: 10px;
}

.turn-user {
  background: #f3f7ff;
}

.turn-agent {
  background: #f7f7f7;
}

.turn-role {
  margin: 0 0 6px;
  font-size: 12px;
  color: #666;
}

.turn-content {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #2f2f2f;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.empty-dialogue {
  overflow-wrap: anywhere;
  margin: 8px 0;
  font-size: 13px;
  color: #666;
}

.history-page :deep(.page-header) {
  min-width: 0;
}

.history-page :deep(.page-header h1) {
  min-width: 0;
  overflow-wrap: anywhere;
}

@media (min-width: 768px) {
  .list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
