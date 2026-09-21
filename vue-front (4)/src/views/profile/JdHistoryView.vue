<template>
  <div class="history-page">
    <ResumePageHeader title="历史JD记录" action="none" @back="$emit('back')" />

    <section v-if="historyList.length === 0" class="empty-card">
      <h2>暂无 JD 记录</h2>
      <p>你提交 JD 并完成解析后，这里会自动生成历史台账，方便快速回看。</p>
    </section>

    <section v-else class="list">
      <article v-for="item in historyList" :key="item.id" class="item-card">
        <div class="item-head">
          <h3>{{ item.title || 'JD 解析结果' }}</h3>
          <span class="time">{{ formatDateTime(item.createdAt) }}</span>
        </div>
        <p class="summary">{{ item.summary || '暂无摘要。' }}</p>
        <p v-if="item.jdText" class="jd-text">{{ item.jdText }}</p>
        <p class="meta">
          <span>硬技能：{{ item.hardSkillCount || 0 }}</span>
          <span>软技能：{{ item.softSkillCount || 0 }}</span>
          <span v-if="Array.isArray(item.tags) && item.tags.length">标签：{{ item.tags.join(' / ') }}</span>
        </p>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import ResumePageHeader from '@/views/resume/components/ResumePageHeader.vue';
import { useJdStore } from '@/stores/jd';
import { formatDateTime } from '@/utils/format';

defineEmits(['back']);

const jdStore = useJdStore();

const historyList = computed(() =>
  jdStore
    .getHistoryRecords()
    .slice()
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
);
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

.summary {
  overflow-wrap: anywhere;
  margin: 8px 0 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #4f4f4f;
}

.jd-text {
  overflow-wrap: anywhere;
  margin: 0 0 8px;
  font-size: 12px;
  color: #666;
  line-height: 1.6;
}

.meta {
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: #6e6e6e;
  overflow-wrap: anywhere;
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
