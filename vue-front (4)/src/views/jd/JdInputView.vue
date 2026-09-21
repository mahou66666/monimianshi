<template>
  <div class="jd-input-container">
    <JdPageHeader
      title="JD 智能解析"
      :show-delete="true"
      @back="$emit('back')"
    />

    <div class="info-section">
      <div class="label-row">
        <svg class="file-icon" viewBox="0 0 24 24" fill="none" stroke="#e2cd6d" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
          <line x1="12" y1="11" x2="12" y2="17"></line>
          <line x1="9" y1="14" x2="15" y2="14"></line>
        </svg>
        <h2>粘贴目标岗位详情</h2>
      </div>
      <p class="label-description">
        粘贴完整的招聘要求 (JD)，AI 将为您提取核心技能、分析岗位画像，并可生成针对性的模拟面试题。
      </p>
    </div>

    <section class="input-card">
      <textarea
        v-model="jdText"
        class="jd-textarea"
        placeholder="例如：
岗位职责：
1. 负责公司核心产品的前端研发工作；
2. 参与前端工程化建设...

任职要求：
1. 熟练掌握 Vue3/React 框架；
2. 具备良好的沟通能力..."
      ></textarea>
      <p class="counter">{{ jdText.length }} 字</p>
    </section>

    <div class="examples-row">
      <span class="examples-label">快捷示例：</span>
      <button type="button" class="chip">前端工程师</button>
      <button type="button" class="chip">产品经理</button>
    </div>

    <button class="start-btn" :disabled="!jdText.trim()" @click="handleStart">
      <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="4" y="4" width="16" height="16" rx="2" ry="2"></rect>
        <rect x="9" y="9" width="6" height="6"></rect>
        <line x1="9" y1="1" x2="9" y2="4"></line>
        <line x1="15" y1="1" x2="15" y2="4"></line>
        <line x1="9" y1="20" x2="9" y2="23"></line>
        <line x1="15" y1="20" x2="15" y2="23"></line>
        <line x1="20" y1="9" x2="23" y2="9"></line>
        <line x1="20" y1="14" x2="23" y2="14"></line>
        <line x1="1" y1="9" x2="4" y2="9"></line>
        <line x1="1" y1="14" x2="4" y2="14"></line>
      </svg>
      开始智能解析
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useJdStore } from '@/stores/jd';
import JdPageHeader from './components/JdPageHeader.vue';

const emit = defineEmits(['back', 'start-parse']);

const jdStore = useJdStore();

const jdText = computed({
  get: () => jdStore.state.draftText,
  set: (value) => jdStore.setDraftText(value),
});

const handleStart = () => {
  if (!jdText.value.trim()) {
    return;
  }

  emit('start-parse', jdText.value.trim());
};
</script>

<style scoped>
.jd-input-container {
  width: 100%;
  max-width: 1200px;
  margin-inline: auto;
  padding: 12px 16px;
  box-sizing: border-box;
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.info-section {
  padding: 4px 4px 12px;
}

.label-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.file-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.label-row h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #2c2c2c;
}

.label-description {
  margin: 0 0 0 26px;
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.6;
}

.input-card {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #eadd9b;
  padding: 16px;
  position: relative;
  display: flex;
  flex-direction: column;
}

.jd-textarea {
  width: 100%;
  min-height: clamp(160px, 40dvh, 400px);
  border: none;
  padding: 0;
  box-sizing: border-box;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  background: transparent;
  outline: none;
  font-family: inherit;
}

.jd-textarea::placeholder {
  color: #a2a7af;
}

.counter {
  margin: 0;
  text-align: right;
  font-size: 12px;
  color: #b0b5bd;
  padding-top: 12px;
}

.examples-row {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.examples-label {
  font-size: 13px;
  color: #8c929b;
}

.chip {
  border: none;
  border-radius: 20px;
  background: #ffffff;
  color: #333;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}

.start-btn {
  margin-top: auto;
  width: 100%;
  border: none;
  border-radius: 14px;
  background: #e2e2e2;
  color: #8c929b;
  font-size: 16px;
  font-weight: 600;
  padding: 15px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 20px;
}

.start-btn:not(:disabled) {
  background: #333;
  color: #fff;
}

.btn-icon {
  width: 18px;
  height: 18px;
  color: inherit;
}
</style>
