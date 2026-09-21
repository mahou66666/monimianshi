<template>
  <div class="resume-upload-container">
    <ResumePageHeader
      title="简历智能润色"
      action="history"
      action-label="历史记录"
      @back="$emit('back')"
      @action="$emit('open-history')"
    />

    <section class="section-card upload-section">
      <div class="section-title">
        <span class="index-circle">1</span>
        <h2>上传原版简历</h2>
      </div>

      <div
        class="upload-box"
        :class="{
          selected: !!selectedFile,
          invalid: !!selectedFileError,
          busy: isUploading || isAnalyzing,
        }"
        role="button"
        tabindex="0"
        @click="triggerFileSelect"
        @keydown.enter.prevent="triggerFileSelect"
        @keydown.space.prevent="triggerFileSelect"
      >
        <input
          ref="fileInputRef"
          class="hidden-file-input"
          type="file"
          :accept="resumeAccept"
          @change="handleFileChange"
        />

        <div class="upload-icon-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 16.2A5 5 0 0 0 18 7h-1.26A8 8 0 1 0 4 15.3"></path>
            <polyline points="16 11 12 7 8 11"></polyline>
            <line x1="12" y1="7" x2="12" y2="21"></line>
          </svg>
        </div>

        <template v-if="selectedFile">
          <div class="file-summary">
            <h3>{{ selectedFile.name }}</h3>
            <p>{{ selectedFileMeta }}</p>
          </div>
          <div class="file-actions">
            <button type="button" class="file-action-btn" @click.stop="triggerFileSelect">
              重新选择
            </button>
            <button
              type="button"
              class="file-action-btn secondary"
              :disabled="isUploading || isAnalyzing"
              @click.stop="handleRemoveFile"
            >
              移除
            </button>
          </div>
        </template>

        <template v-else>
          <h3>{{ uploadGuide.uploadTitle }}</h3>
          <p>{{ uploadGuide.uploadDescription }}</p>
        </template>
      </div>

      <p v-if="selectedFileError" class="upload-feedback error">{{ selectedFileError }}</p>
      <p v-else-if="requestErrorMessage" class="upload-feedback error">{{ requestErrorMessage }}</p>
      <p v-else-if="isUploading" class="upload-feedback">简历上传中，请稍候...</p>
      <p v-else-if="isAnalyzing" class="upload-feedback">AI 正在生成优化结果...</p>
      <p v-else-if="guideStatus === REQUEST_STATUS.error" class="upload-feedback">
        上传说明加载失败，当前使用本地默认文案。
      </p>
    </section>

    <section v-if="showStructuredSummary" class="section-card structured-section">
      <div class="section-title">
        <span class="index-circle">1.5</span>
        <h2>结构化切割结果</h2>
      </div>
      <div class="structured-card">
        <div class="structured-head">
          <p>简历已自动切割并入库</p>
          <span class="structured-total">总片段：{{ fragmentCountValue }}</span>
        </div>
        <div class="structured-grid">
          <div class="structured-item">
            <span class="label">教育</span>
            <span class="value">{{ sectionCountMap.EDUCATION || 0 }}</span>
          </div>
          <div class="structured-item">
            <span class="label">项目</span>
            <span class="value">{{ sectionCountMap.PROJECT_EXPERIENCE || 0 }}</span>
          </div>
          <div class="structured-item">
            <span class="label">实习</span>
            <span class="value">{{ sectionCountMap.INTERNSHIP_EXPERIENCE || 0 }}</span>
          </div>
          <div class="structured-item">
            <span class="label">工作</span>
            <span class="value">{{ sectionCountMap.WORK_EXPERIENCE || 0 }}</span>
          </div>
          <div class="structured-item">
            <span class="label">技能</span>
            <span class="value">{{ sectionCountMap.SKILLS || 0 }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="section-card target-section">
      <div class="section-title">
        <span class="index-circle">2</span>
        <h2>对齐目标岗位（选填）</h2>
      </div>
      <div class="input-box">
        <textarea
          v-model="targetJdText"
          class="input-area"
          :placeholder="uploadGuide.jdPlaceholder"
        ></textarea>
        <span class="counter">{{ targetJdText.length }} / 1000</span>
      </div>
    </section>

    <section class="agent-card">
      <div class="agent-icon">
        <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="22" cy="22" r="6" fill="#2f2f2f"></circle>
          <circle cx="46" cy="46" r="6" fill="#2f2f2f"></circle>
          <line x1="20" y1="42" x2="44" y2="20"></line>
          <line x1="12" y1="32" x2="32" y2="52"></line>
          <line x1="32" y1="12" x2="52" y2="32"></line>
        </svg>
      </div>
      <div class="agent-content">
        <h3>{{ uploadGuide.agentTitle }}</h3>
        <p>{{ uploadGuide.agentDescription }}</p>
      </div>
    </section>

    <button
      class="analyze-btn"
      :class="{ active: canStartAnalysis }"
      :disabled="!canStartAnalysis"
      @click="$emit('start-analysis', targetJdText.trim())"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M13 2L3 14h7l-1 8 10-12h-7z"></path>
      </svg>
      {{ actionLabel }}
    </button>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import ResumePageHeader from './components/ResumePageHeader.vue';
import { REQUEST_STATUS, RESUME_UPLOAD } from '@/utils/constants';
import { useResumeStore } from '@/stores/resume';
import { formatFileSizeFromBytes } from '@/utils/format';

defineEmits(['back', 'start-analysis', 'open-history']);

const resumeStore = useResumeStore();
const fileInputRef = ref(null);

const targetJdText = computed({
  get: () => resumeStore.state.targetJdText,
  set: (value) => resumeStore.setTargetJdText(value),
});

const selectedFile = computed(() => resumeStore.state.selectedFile);
const selectedFileError = computed(() => resumeStore.state.selectedFileError);
const uploadStatus = computed(() => resumeStore.state.status.upload);
const analysisStatus = computed(() => resumeStore.state.status.analysis);
const guideStatus = computed(() => resumeStore.state.status.uploadGuide);
const uploadResult = computed(() => resumeStore.state.uploadResult || null);
const requestErrorMessage = computed(
  () => resumeStore.state.errorMessage.upload || resumeStore.state.errorMessage.analysis,
);

const resumeAccept = RESUME_UPLOAD.accept;

const selectedFileMeta = computed(() => {
  if (!selectedFile.value) {
    return '';
  }

  return formatFileSizeFromBytes(selectedFile.value.size);
});

const isUploading = computed(() => uploadStatus.value === REQUEST_STATUS.loading);
const isAnalyzing = computed(() => analysisStatus.value === REQUEST_STATUS.loading);
const sectionCountMap = computed(() => uploadResult.value?.sectionCounts || {});
const fragmentCountValue = computed(() => Number(uploadResult.value?.fragmentCount) || 0);
const showStructuredSummary = computed(
  () =>
    uploadStatus.value === REQUEST_STATUS.success &&
    (fragmentCountValue.value > 0 || Object.keys(sectionCountMap.value).length > 0),
);

const canStartAnalysis = computed(
  () =>
    !!selectedFile.value &&
    !selectedFileError.value &&
    !isUploading.value &&
    !isAnalyzing.value,
);

const actionLabel = computed(() => {
  if (isUploading.value) {
    return '上传中...';
  }

  if (isAnalyzing.value) {
    return '正在生成优化结果...';
  }

  return '开始智能分析';
});

const uploadGuide = computed(
  () =>
    resumeStore.state.uploadGuide ?? {
      uploadTitle: '点击或拖拽上传',
      uploadDescription: '支持 PDF、DOC、DOCX 格式（最大 10MB）',
      jdPlaceholder: '粘贴目标岗位 JD，AI 会给出更有针对性的优化建议。',
      agentTitle: 'AI 简历优化引擎',
      agentDescription: '上传后，系统会自动解析并切割简历内容，再结合 JD 输出优化建议。',
    },
);

const triggerFileSelect = () => {
  if (isUploading.value || isAnalyzing.value) {
    return;
  }

  fileInputRef.value?.click();
};

const handleFileChange = (event) => {
  const nextFile = event.target.files?.[0];

  if (nextFile) {
    resumeStore.selectResumeFile(nextFile);
  }

  event.target.value = '';
};

const handleRemoveFile = () => {
  resumeStore.clearSelectedFile();
};

onMounted(() => {
  resumeStore.ensureUploadGuide();
});
</script>
<style scoped>
.resume-upload-container {
  width: 100%;
  max-width: 1200px;
  margin-inline: auto;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 10px 16px 18px;
  box-sizing: border-box;
  background: #eaddd3;
  overflow-y: auto;
}

.section-card {
  background: transparent;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 22px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.index-circle {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #2f2f2f;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
}

.section-title h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #2c2c2c;
}

.upload-box {
  background: #fff;
  border: 2px dashed #dcdcdc;
  border-radius: 20px;
  padding: 32px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.upload-box.selected {
  border-color: #d7c16a;
  background: #fffdf4;
}

.upload-box.invalid {
  border-color: #d86c6c;
  background: #fff7f7;
}

.upload-box.busy {
  cursor: wait;
}

.hidden-file-input {
  display: none;
}

.upload-icon-wrapper {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: #eef1e6;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}

.upload-icon-wrapper svg {
  width: 24px;
  height: 24px;
  color: #7b8c5c;
}

.upload-box h3 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 700;
  color: #2c2c2c;
}

.upload-box p {
  margin: 0;
  font-size: 13px;
  color: #a2a7af;
  text-align: center;
}

.file-summary {
  text-align: center;
}

.file-summary h3 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 700;
  color: #2c2c2c;
  word-break: break-word;
}

.file-summary p {
  color: #7d8591;
}

.file-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.file-action-btn {
  border: none;
  border-radius: 999px;
  background: #2f2f2f;
  color: #f6f3ea;
  padding: 9px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.file-action-btn.secondary {
  background: #f0f0f0;
  color: #666;
}

.file-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.upload-feedback {
  margin: 10px 6px 0;
  font-size: 12px;
  color: #7b8c5c;
}

.upload-feedback.error {
  color: #d86c6c;
}

.structured-card {
  background: #fff;
  border-radius: 20px;
  padding: 14px 14px 12px;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.04);
}

.structured-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.structured-head p {
  margin: 0;
  color: #3c3c3c;
  font-size: 13px;
  font-weight: 600;
}

.structured-total {
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  color: #6a5a2a;
  background: #f4ecd2;
  font-weight: 700;
}

.structured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(70px, 1fr));
  gap: 8px;
}

.structured-item {
  border-radius: 12px;
  background: #f7f7f7;
  padding: 8px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.structured-item .label {
  font-size: 11px;
  color: #8d8d8d;
}

.structured-item .value {
  font-size: 15px;
  font-weight: 700;
  color: #2f2f2f;
}

.input-box {
  background: #fff;
  border-radius: 20px;
  padding: 18px 18px 22px;
  min-height: 150px;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.04);
  position: relative;
}

.input-area {
  width: 100%;
  min-height: 130px;
  border: none;
  padding: 0;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  background: transparent;
  outline: none;
  font-family: inherit;
}

.input-area::placeholder {
  color: #a2a7af;
}

.counter {
  position: absolute;
  right: 16px;
  bottom: 14px;
  font-size: 12px;
  color: #c9c9c9;
}

.agent-card {
  background: #2f2f2f;
  border-radius: 24px;
  padding: 18px;
  display: flex;
  gap: 14px;
  color: #f6f3ea;
  box-shadow: 0 12px 20px rgba(0, 0, 0, 0.15);
  margin-bottom: 24px;
}

.agent-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #e2cd6d;
  color: #2f2f2f;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.agent-icon svg {
  width: 30px;
  height: 30px;
}

.agent-content h3 {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 700;
}

.agent-content p {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.78);
}

.analyze-btn {
  margin-top: auto;
  width: 100%;
  border: none;
  border-radius: 20px;
  padding: 16px 0;
  background: #cfcfcf;
  color: #8f8f8f;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: inset 0 2px 4px rgba(255, 255, 255, 0.6);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.analyze-btn.active {
  background: #2f2f2f;
  color: #f6f3ea;
  box-shadow: 0 12px 20px rgba(0, 0, 0, 0.2);
}

.analyze-btn.active svg {
  color: #e2cd6d;
}

.analyze-btn svg {
  width: 18px;
  height: 18px;
}
.section-card, .file-summary, .agent-content { min-width: 0; overflow-wrap: anywhere; }
.file-actions, .structured-head { flex-wrap: wrap; }

@media (min-width: 1200px) {
  .resume-upload-container {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: auto auto auto 1fr auto;
    gap: 20px 24px;
    align-content: start;
  }
  .resume-upload-container > :first-child { grid-column: 1 / -1; }
  .upload-section { grid-column: 1; grid-row: 2; }
  .target-section { grid-column: 2; grid-row: 2; }
  .structured-section { grid-column: 1 / -1; grid-row: 3; }
  .agent-card, .analyze-btn { grid-column: 1 / -1; }
  .input-box { flex: 1; }
  .input-area { min-height: 220px; height: 100%; }
}
</style>




