<template>
  <div class="resume-result">
    <ResumePageHeader
      title="AI简历优化"
      action="download"
      action-label="下载"
      @back="$emit('back')"
    />

    <section v-if="status === REQUEST_STATUS.loading" class="state-card loading-card">
      <h2>AI 正在分析简历</h2>
      <p>我们正在结合简历内容和目标岗位生成优化建议，请稍候。</p>
    </section>

    <section v-else-if="status === REQUEST_STATUS.error" class="state-card error-card">
      <h2>分析失败</h2>
      <p>{{ errorMessage || '请稍后重试' }}</p>
      <button type="button" class="retry-btn" @click="resumeStore.ensureAnalysisResult()">重新分析</button>
    </section>

    <section v-else-if="status === REQUEST_STATUS.idle" class="state-card idle-card">
      <h2>还没有分析结果</h2>
      <p>先上传一份简历并开始智能分析，我们会在这里展示评分和优化建议。</p>
    </section>

    <template v-else>
      <section class="score-card">
        <div class="score-info">
          <p class="score-label">{{ analysisResult.scoreLabel || '规则参考分（非 AI 评分）' }}</p>
          <div class="score-value">
            <span class="score">{{ analysisResult.score }}</span>
            <span class="total">/{{ analysisResult.total }}</span>
          </div>
          <div class="badge-list">
            <span v-for="badge in analysisResult.badges" :key="badge" class="badge">
              {{ badge }}
            </span>
          </div>
        </div>
        <div class="score-icon">
          <div class="icon-ring">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 17l6-6 4 4 7-7"></path>
              <polyline points="14 7 20 7 20 13"></polyline>
            </svg>
          </div>
        </div>
      </section>

      <section v-if="analysisResult.fallback" class="state-card fallback-notice" role="status">
        <h2>AI 改写未完成</h2>
        <p>{{ analysisResult.fallbackMessage }}</p>
        <p>参考分由内容完整度等规则计算，改写降级时扣 8 分；不代表模型对简历能力的评分。</p>
      </section>

      <section class="info-card">
        <header class="info-header">
          <div class="info-title">
            <span class="doc-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
              </svg>
            </span>
            <h2>{{ workSection.title }}</h2>
          </div>
          <span class="status-tag">{{ workSection.status }}</span>
          <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </header>
        <div class="divider"></div>
        <div class="info-block">
          <p class="block-title">原文解析</p>
          <div class="text-chip">{{ workSection.originalText }}</div>
        </div>
        <div class="info-block">
          <div class="block-title with-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 18h6"></path>
              <path d="M10 22h4"></path>
              <path d="M12 2a7 7 0 0 0-4 12c.6.5 1 1.2 1 2h6c0-.8.4-1.5 1-2a7 7 0 0 0-4-12z"></path>
            </svg>
            优化建议（含规则建议）
          </div>
          <div class="pill-list">
            <span v-for="suggestion in workSection.suggestions" :key="suggestion" class="pill">
              {{ suggestion }}
            </span>
          </div>
        </div>
        <button type="button" class="ghost-btn">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3v18"></path>
            <path d="M3 12h18"></path>
          </svg>
          {{ workSection.actionLabel }}
        </button>
      </section>

      <section class="info-card">
        <header class="info-header">
          <div class="info-title">
            <span class="doc-icon green">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 12l2 2 4-4"></path>
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
              </svg>
            </span>
            <h2>{{ projectSection.title }}</h2>
          </div>
          <span class="status-tag done">{{ projectSection.status }}</span>
          <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </header>
        <div class="divider"></div>
        <p class="block-title">{{ analysisResult.fallback ? '兜底内容（非 AI 改写结果）' : '改写结果预览' }}</p>
        <div class="edit-box">{{ projectSection.optimizedText }}</div>
        <div class="card-actions">
          <button type="button" class="text-btn">{{ projectSection.secondaryActionLabel }}</button>
          <button type="button" class="apply-btn">{{ projectSection.primaryActionLabel }}</button>
        </div>
      </section>

      <section class="info-card">
        <header class="info-header">
          <div class="info-title">
            <span class="doc-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
              </svg>
            </span>
            <h2>{{ educationSection.title }}</h2>
          </div>
          <span class="status-tag">{{ educationSection.status }}</span>
          <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </header>
      </section>

      <button type="button" class="final-btn" :disabled="!analysisResult.fallback || isRegenerating" @click="handleFinalAction">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 11 12 14 22 4"></polyline>
          <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path>
        </svg>
        {{ isRegenerating ? '正在重新生成…' : analysisResult.finalActionLabel }}
      </button>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { REQUEST_STATUS } from '@/utils/constants';
import ResumePageHeader from './components/ResumePageHeader.vue';
import { useResumeStore } from '@/stores/resume';
import { presentResumeAnalysis } from '@/utils/resumePresentation';

defineEmits(['back']);

const resumeStore = useResumeStore();
const isRegenerating = ref(false);

const status = computed(() => resumeStore.state.status.analysis);
const errorMessage = computed(() => resumeStore.state.errorMessage.analysis);
const analysisResult = computed(
  () =>
    presentResumeAnalysis(resumeStore.state.analysisResult ?? {
      score: 0,
      total: 100,
      badges: [],
      sections: [],
      finalActionLabel: '生成最终简历',
    }),
);

const workSection = computed(
  () =>
    analysisResult.value.sections.find((section) => section.id === 'work-experience') ?? {
      title: '工作经历',
      status: '待优化',
      originalText: '',
      suggestions: [],
      actionLabel: '一键智能重写',
    },
);

const projectSection = computed(
  () =>
    analysisResult.value.sections.find((section) => section.id === 'project-experience') ?? {
      title: '项目经历',
      status: '已优化',
      optimizedText: '',
      primaryActionLabel: '应用修改',
      secondaryActionLabel: '撤销还原',
    },
);

const educationSection = computed(
  () =>
    analysisResult.value.sections.find((section) => section.id === 'education') ?? {
      title: '教育背景',
      status: '待优化',
    },
);

const handleFinalAction = async () => {
  if (!analysisResult.value.fallback || isRegenerating.value) {
    return;
  }

  isRegenerating.value = true;
  try {
    await resumeStore.analyzeCurrentResume(
      resumeStore.state.targetJdText,
      resumeStore.state.uploadResult,
    );
  } catch {
    // The store exposes the error state and message to the page.
  } finally {
    isRegenerating.value = false;
  }
};

onMounted(() => {
  resumeStore.ensureAnalysisResult();
});
</script>

<style scoped>
.resume-result {
  width: 100%;
  max-width: 1200px;
  min-height: 100%;
  height: auto;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  padding: 10px 16px 20px;
  box-sizing: border-box;
  background: #eaddd3;
  overflow-y: auto;
}

.resume-result :deep(.page-header) {
  min-width: 0;
}

.resume-result :deep(.page-header h1) {
  min-width: 0;
  overflow-wrap: anywhere;
}

.state-card {
  min-width: 0;
  margin-top: 16px;
  padding: 24px 20px;
  border-radius: 26px;
  background: #fff;
  box-shadow: 0 10px 18px rgba(0, 0, 0, 0.08);
}

.state-card h2 {
  margin: 0 0 10px;
  font-size: 20px;
  color: #2f2f2f;
}

.state-card p {
  overflow-wrap: anywhere;
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #6b6b6b;
}

.loading-card {
  background: #2f3231;
}

.loading-card h2,
.loading-card p {
  color: #f6f1e7;
}

.error-card {
  background: #fff4f4;
}

.idle-card {
  background: #fffaf0;
}

.retry-btn {
  max-width: 100%;
  white-space: normal;
  overflow-wrap: anywhere;
  margin-top: 16px;
  border: none;
  border-radius: 999px;
  background: #2f2f2f;
  color: #fff;
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 600;
}

.score-card {
  min-width: 0;
  background: #2f3231;
  color: #f6f1e7;
  border-radius: 26px;
  padding: 20px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 12px 20px rgba(0, 0, 0, 0.2);
}

.score-label {
  overflow-wrap: anywhere;
  margin: 0 0 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.score-info {
  min-width: 0;
}

.score-value {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 12px;
}

.score {
  font-size: 42px;
  font-weight: 700;
  color: #f5dd7a;
}

.total {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 600;
}

.badge-list {
  display: flex;
  flex-wrap: wrap;
  min-width: 0;
  gap: 10px;
}

.badge {
  max-width: 100%;
  overflow-wrap: anywhere;
  font-size: 12px;
  padding: 6px 12px;
  border-radius: 12px;
  font-weight: 600;
  background: #3c3f3e;
  color: #fff;
}

.score-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-ring {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  border: 3px solid #f5dd7a;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f5dd7a;
}

.icon-ring svg {
  width: 28px;
  height: 28px;
}

.info-card {
  min-width: 0;
  background: #fff;
  border-radius: 26px;
  padding: 18px;
  margin-top: 18px;
  box-shadow: 0 10px 18px rgba(0, 0, 0, 0.08);
}

.info-header {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.info-title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.info-title h2 {
  min-width: 0;
  overflow-wrap: anywhere;
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #2f2f2f;
}

.doc-icon {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 18px;
  background: #f3f2ee;
  color: #8b887f;
  display: flex;
  align-items: center;
  justify-content: center;
}

.doc-icon.green {
  background: #eef3e4;
  color: #8ea06c;
}

.doc-icon svg {
  width: 18px;
  height: 18px;
}

.status-tag {
  max-width: 100%;
  overflow-wrap: anywhere;
  padding: 4px 10px;
  border-radius: 12px;
  background: #f0f0f0;
  font-size: 12px;
  color: #7a7a7a;
  font-weight: 600;
}

.status-tag.done {
  background: #eef3e4;
  color: #7a8c52;
}

.chevron {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  color: #9a9a9a;
}

.divider {
  height: 1px;
  background: #efefef;
  margin: 14px 0 16px;
}

.info-block + .info-block {
  margin-top: 18px;
}

.block-title {
  margin: 0 0 10px;
  font-size: 13px;
  color: #8a8579;
  font-weight: 600;
}

.block-title.with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}

.block-title.with-icon svg {
  width: 16px;
  height: 16px;
  color: #7a8c52;
}

.text-chip,
.edit-box {
  max-width: 100%;
  box-sizing: border-box;
  overflow-wrap: anywhere;
  word-break: break-word;
  background: #f8f7f2;
  border-radius: 16px;
  padding: 14px;
  font-size: 13px;
  line-height: 1.7;
  color: #4d4a44;
}

.pill-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.pill {
  max-width: 100%;
  overflow-wrap: anywhere;
  padding: 8px 12px;
  border-radius: 999px;
  background: #eef3e4;
  color: #7a8c52;
  font-size: 12px;
  font-weight: 600;
}

.ghost-btn {
  min-width: 0;
  white-space: normal;
  overflow-wrap: anywhere;
  margin-top: 16px;
  width: 100%;
  border: none;
  border-radius: 16px;
  background: #f6f2e9;
  color: #4d4a44;
  padding: 14px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
}

.ghost-btn svg {
  width: 16px;
  height: 16px;
}

.card-actions {
  min-width: 0;
  flex-wrap: wrap;
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.text-btn,
.apply-btn {
  min-width: 0;
  max-width: 100%;
  white-space: normal;
  overflow-wrap: anywhere;
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 700;
}

.text-btn {
  background: #f0f0f0;
  color: #7a7a7a;
}

.apply-btn {
  background: #2f3231;
  color: #f6f1e7;
}

.final-btn {
  min-width: 0;
  white-space: normal;
  overflow-wrap: anywhere;
  margin-top: 20px;
  margin-bottom: 6px;
  width: 100%;
  border: none;
  border-radius: 20px;
  background: #2f3231;
  color: #f6f1e7;
  padding: 16px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
}

.final-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.final-btn svg {
  width: 18px;
  height: 18px;
}

@media (min-width: 1200px) {
  .resume-result {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-content: start;
    gap: 18px;
  }

  .resume-result :deep(.page-header),
  .state-card,
  .score-card,
  .final-btn {
    grid-column: 1 / -1;
  }

  .state-card,
  .info-card,
  .final-btn {
    margin-top: 0;
  }
}
</style>
