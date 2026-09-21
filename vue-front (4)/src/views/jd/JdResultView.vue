<template>
  <div class="jd-result-container">
    <JdPageHeader
      title="JD 智能解析"
      :elevated-back-button="true"
      @back="$emit('back')"
    />

    <section v-if="status === REQUEST_STATUS.loading" class="state-card loading-card">
      <h2>JD 解析中</h2>
      <p>我们正在提取岗位关键词、技能要求和岗位画像，请稍候。</p>
    </section>

    <section v-else-if="status === REQUEST_STATUS.error" class="state-card error-card">
      <h2>解析失败</h2>
      <p>{{ errorMessage || '请稍后重试' }}</p>
      <button type="button" class="retry-btn" @click="jdStore.ensureAnalysisResult()">重新解析</button>
    </section>

    <section v-else-if="status === REQUEST_STATUS.idle" class="state-card idle-card">
      <h2>还没有解析结果</h2>
      <p>先返回上一页粘贴一份 JD，我们就能生成岗位画像和面试重点。</p>
    </section>

    <template v-else>
      <section class="summary-card">
        <div class="summary-tabs">
          <div class="summary-tab active">
            <span class="dot"></span>
            <span>岗位画像</span>
          </div>
          <div class="summary-tab">
            <span class="ai-icon">
              <svg viewBox="0 0 24 24" fill="#a8a3fb" stroke="none">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"></path>
              </svg>
            </span>
            <span>AI 提炼总结</span>
          </div>
        </div>
        <h2>{{ analysisResult.title }}</h2>
        <p>{{ analysisResult.summary }}</p>
      </section>

      <section class="tags-section">
        <div class="section-title">
          <span class="title-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="#5e5b54" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
              <line x1="12" y1="8" x2="12" y2="16"></line>
              <line x1="8" y1="12" x2="16" y2="12"></line>
            </svg>
          </span>
          <span>核心考察标签</span>
        </div>
        <div class="tag-list">
          <span v-for="tag in analysisResult.tags" :key="tag" class="tag-chip">
            <i class="dot"></i>
            {{ tag }}
          </span>
        </div>
      </section>

      <section class="skill-card">
        <div class="card-header">
          <span class="card-icon-svg hard">
            <svg viewBox="0 0 24 24" fill="#ffdede" stroke="none">
              <circle cx="12" cy="12" r="11"></circle>
              <path d="M16 11V13M13 16H11M8 13V11M11 8H13M12 11V13M11 12H13" stroke="#e26d6d" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"></path>
            </svg>
          </span>
          <h3>专业硬技能 (Hard Skills)</h3>
        </div>
        <ul>
          <li v-for="skill in analysisResult.hardSkills" :key="skill">
            <span class="check hard"></span>
            {{ skill }}
          </li>
        </ul>
      </section>

      <section class="skill-card">
        <div class="card-header">
          <span class="card-icon-svg soft">
            <svg viewBox="0 0 24 24" fill="#e5f4ff" stroke="none">
              <circle cx="12" cy="12" r="11"></circle>
              <path d="M8 12.5C8.5 14 10 15 12 15s3.5-1 4-2.5M10 10h.01M14 10h.01" stroke="#5ea6d6" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"></path>
            </svg>
          </span>
          <h3>综合软素质 (Soft Skills)</h3>
        </div>
        <ul>
          <li v-for="skill in analysisResult.softSkills" :key="skill">
            <span class="check soft"></span>
            {{ skill }}
          </li>
        </ul>
      </section>

      <div class="footer-actions">
        <button type="button" class="circle-btn" aria-label="返回" @click="$emit('back')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
        </button>
        <button type="button" class="primary-btn" @click="$emit('start-interview')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a4 4 0 0 1-4 4H7l-4 4V5a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z"></path>
          </svg>
          生成针对性面试题
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { REQUEST_STATUS } from '@/utils/constants';
import { useJdStore } from '@/stores/jd';
import JdPageHeader from './components/JdPageHeader.vue';

defineEmits(['back', 'start-interview']);

const jdStore = useJdStore();

const status = computed(() => jdStore.state.status);
const errorMessage = computed(() => jdStore.state.errorMessage);
const analysisResult = computed(
  () =>
    jdStore.state.analysisResult ?? {
      title: '',
      summary: '',
      tags: [],
      hardSkills: [],
      softSkills: [],
    },
);

onMounted(() => {
  jdStore.ensureAnalysisResult();
});
</script>

<style scoped>
.jd-result-container {
  width: 100%;
  max-width: 1200px;
  min-height: 100%;
  height: auto;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  padding: 10px 16px 16px;
  box-sizing: border-box;
  background-color: transparent;
  background-image: radial-gradient(circle at 1px 1px, rgba(0, 0, 0, 0.04) 1px, transparent 0);
  background-size: 15px 15px;
  background-position: 0 0;
  overflow-y: auto;
}

.jd-result-container :deep(.page-header) {
  min-width: 0;
}

.jd-result-container :deep(.page-header h1) {
  min-width: 0;
  overflow-wrap: anywhere;
}

.state-card {
  min-width: 0;
  margin-top: 16px;
  padding: 24px 20px;
  border-radius: 24px;
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
  background: #2f2f2f;
}

.loading-card h2,
.loading-card p {
  color: #f6f3ea;
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

.summary-card {
  min-width: 0;
  background: #2f2f2f;
  color: #f6f3ea;
  border-radius: 24px;
  padding: 18px 18px 20px;
  box-shadow: 0 10px 18px rgba(0, 0, 0, 0.2);
}

.summary-tabs {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}

.summary-tab {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.6);
  overflow-wrap: anywhere;
  word-break: break-word;
}

.summary-tab.active {
  color: #fff;
}

.summary-tab.active .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #e2cd6d;
  flex-shrink: 0;
}

.summary-tab .ai-icon {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.summary-tab .ai-icon svg {
  width: 100%;
  height: 100%;
}

.summary-card h2 {
  overflow-wrap: anywhere;
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.summary-card p {
  overflow-wrap: anywhere;
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.65);
}

.tags-section {
  min-width: 0;
  margin-top: 20px;
}

.section-title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #5e5b54;
  font-weight: 700;
  font-size: 14px;
}

.title-icon {
  width: 24px;
  height: 24px;
  border-radius: 8px;
  background: #f6f2e9;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}

.title-icon svg {
  width: 18px;
  height: 18px;
}

.tag-list {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-chip {
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  overflow-wrap: anywhere;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 16px;
  background: #fff;
  font-size: 13px;
  color: #4d4a44;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.06);
  font-weight: 600;
}

.tag-chip .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e2cd6d;
  display: inline-block;
}

.skill-card {
  min-width: 0;
  margin-top: 18px;
  background: #fff;
  border-radius: 22px;
  padding: 16px 18px 14px;
  box-shadow: 0 10px 18px rgba(0, 0, 0, 0.08);
}

.card-header {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.card-header h3 {
  min-width: 0;
  overflow-wrap: anywhere;
  margin: 0;
  font-size: 16px;
  color: #3a3a3a;
}

.card-icon-svg {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-icon-svg.hard {
  background: #ffdede;
}

.card-icon-svg.soft {
  background: #e5f4ff;
}

.card-icon-svg svg {
  width: 24px;
  height: 24px;
}

.skill-card ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skill-card li {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}

.check {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: inline-block;
  position: relative;
  flex-shrink: 0;
}

.check::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 3px;
  width: 5px;
  height: 8px;
  border-right: 2px solid #fff;
  border-bottom: 2px solid #fff;
  transform: rotate(45deg);
}

.check.hard {
  background: #e26d6d;
}

.check.soft {
  background: #5ea6d6;
}

.footer-actions {
  min-width: 0;
  display: flex;
  gap: 12px;
  margin-top: 22px;
  margin-bottom: 8px;
}

.circle-btn {
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 50%;
  background: #fff;
  color: #5e5b54;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.08);
}

.circle-btn svg {
  width: 18px;
  height: 18px;
}

.primary-btn {
  min-width: 0;
  white-space: normal;
  overflow-wrap: anywhere;
  flex: 1;
  border: none;
  border-radius: 16px;
  background: #2f2f2f;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.primary-btn svg {
  width: 18px;
  height: 18px;
}

@media (min-width: 1200px) {
  .jd-result-container {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-content: start;
    gap: 18px;
  }

  .jd-result-container :deep(.page-header),
  .state-card,
  .summary-card,
  .tags-section,
  .footer-actions {
    grid-column: 1 / -1;
  }

  .state-card,
  .tags-section,
  .skill-card,
  .footer-actions {
    margin-top: 0;
  }
}
</style>
