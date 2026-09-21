<template>
  <div class="resume-library">
    <ResumePageHeader
      title="我的简历库"
      action="add"
      action-label="上传简历"
      @back="$emit('back')"
      @action="$emit('open-upload')"
    />

    <section v-if="libraryStatus === REQUEST_STATUS.loading" class="state-card loading-card">
      <h2>简历库加载中</h2>
      <p>正在整理你的默认版本和历史记录，请稍候。</p>
    </section>

    <section v-else-if="libraryStatus === REQUEST_STATUS.error" class="state-card error-card">
      <h2>加载失败</h2>
      <p>{{ libraryErrorMessage || '请稍后重试。' }}</p>
      <button type="button" class="state-btn" @click="resumeStore.ensureResumeLibrary()">重新加载</button>
    </section>

    <section v-else-if="isLibraryEmpty" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
          <line x1="12" y1="11" x2="12" y2="17"></line>
          <line x1="9" y1="14" x2="15" y2="14"></line>
        </svg>
      </div>
      <h2>还没有简历记录</h2>
      <p>先上传 PDF、DOC 或 DOCX 简历，分析完成后会自动展示在这里。</p>
      <button type="button" class="empty-action" @click="$emit('open-upload')">上传简历</button>
    </section>

    <template v-else>
      <section v-if="hasDefaultResume" class="section-container">
        <div class="section-title">
          <span class="star-icon">
            <svg viewBox="0 0 24 24" fill="#e2cd6d" stroke="#e2cd6d" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
            </svg>
          </span>
          <h2>默认投递版本</h2>
        </div>

        <div class="primary-card">
          <div
            class="file-info clickable-file"
            role="button"
            tabindex="0"
            @click="previewResume(defaultResume)"
            @keydown.enter.prevent="previewResume(defaultResume)"
            @keydown.space.prevent="previewResume(defaultResume)"
          >
            <div class="file-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#e2cd6d" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
            </div>
            <div class="file-meta">
              <h3>{{ defaultResume.name }}</h3>
              <p>{{ defaultResumeMeta }}</p>
            </div>
          </div>

          <div class="card-actions">
            <button type="button" class="polish-btn">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 20h9"></path>
                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
              </svg>
              {{ defaultResume.actionLabel }}
            </button>
            <button
              type="button"
              class="download-btn"
              aria-label="Download resume"
              @click="downloadResume(defaultResume)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="7 10 12 15 17 10"></polyline>
                <line x1="12" y1="15" x2="12" y2="3"></line>
              </svg>
            </button>
          </div>
        </div>
      </section>

      <section class="section-container">
        <div class="section-title">
          <span class="history-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path>
              <polyline points="3 3 3 8 8 8"></polyline>
              <path d="M12 7v5l2.5 2.5"></path>
            </svg>
          </span>
          <h2>历史版本记录</h2>
        </div>

        <div v-if="hasHistoryRecords" class="history-list">
          <div v-for="item in historyList" :key="item.id" class="history-item">
            <div
              class="item-left clickable-file"
              role="button"
              tabindex="0"
              @click="previewResume(item)"
              @keydown.enter.prevent="previewResume(item)"
              @keydown.space.prevent="previewResume(item)"
            >
              <div class="item-icon" :class="item.variant === 'blue' ? 'bg-blue' : 'bg-pink'">
                <svg viewBox="0 0 24 24" fill="none" :stroke="item.variant === 'blue' ? '#5ea6d6' : '#d86c6c'" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                  <line x1="16" y1="13" x2="8" y2="13"></line>
                  <line x1="16" y1="17" x2="8" y2="17"></line>
                  <polyline points="10 9 9 9 8 9"></polyline>
                </svg>
              </div>
              <div class="item-meta">
                <h3>{{ item.name }}</h3>
                <p>{{ historyItemMeta(item) }}</p>
              </div>
            </div>
            <div class="item-actions">
              <button
                type="button"
                class="icon-btn"
                aria-label="Set as default"
                @click="resumeStore.setDefaultResume(item.id)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
                </svg>
              </button>
              <button
                type="button"
                class="icon-btn"
                aria-label="Delete resume"
                @click="resumeStore.deleteResume(item.id)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="3 6 5 6 21 6"></polyline>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div v-else class="empty-history">
          当前还没有历史版本记录，后续上传的新版本会自动展示在这里。
        </div>

        <p v-if="hasHistoryRecords" class="footer-tip">仅展示最近 10 条历史记录</p>
      </section>
    </template>
    <div
      v-if="previewPanel.visible"
      class="preview-mask"
      role="presentation"
      @click.self="closePreview"
    >
      <div class="preview-dialog" role="dialog" aria-modal="true" aria-label="简历预览">
        <header class="preview-header">
          <h3>{{ previewPanel.title || '简历预览' }}</h3>
          <div class="preview-actions">
            <div
              v-if="previewPanel.mode === 'docx' && !previewPanel.loading && !previewPanel.error"
              class="zoom-toolbar"
            >
              <button type="button" class="zoom-btn" aria-label="Zoom out" @click="zoomOutPreview">
                -
              </button>
              <span class="zoom-text">{{ previewZoomPercent }}</span>
              <button type="button" class="zoom-btn" aria-label="Zoom in" @click="zoomInPreview">
                +
              </button>
              <button type="button" class="zoom-btn zoom-btn-reset" @click="resetPreviewZoom">
                100%
              </button>
            </div>
            <button type="button" class="preview-btn" @click="downloadResume(previewPanel.entry)">
              下载
            </button>
            <button type="button" class="preview-btn preview-btn-close" @click="closePreview">
              关闭
            </button>
          </div>
        </header>

        <div class="preview-body">
          <p v-if="previewPanel.loading" class="preview-state">正在加载预览...</p>
          <p v-else-if="previewPanel.error" class="preview-state">{{ previewPanel.error }}</p>
          <iframe
            v-else-if="previewPanel.mode === 'pdf'"
            :src="previewPanel.pdfUrl"
            class="preview-pdf"
            title="resume-pdf-preview"
          ></iframe>
          <div
            v-else-if="previewPanel.mode === 'docx'"
            class="preview-docx-viewport"
            @wheel="handleDocxZoomWheel"
          >
            <article
              class="preview-docx"
              :style="docxPreviewStyle"
              v-html="previewPanel.docxHtml"
            ></article>
          </div>
          <p v-else class="preview-state">当前文件格式暂不支持可视化预览。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive } from 'vue';
import { REQUEST_STATUS } from '@/utils/constants';
import ResumePageHeader from './components/ResumePageHeader.vue';
import { getResumePreviewResource, openResumeInBrowser } from '@/api/resume';
import { useResumeStore } from '@/stores/resume';
import { formatDate, formatDateTime, formatFileSize, joinMeta } from '@/utils/format';

defineEmits(['back', 'open-upload']);

const resumeStore = useResumeStore();

const libraryStatus = computed(() => resumeStore.state.status.library);
const libraryErrorMessage = computed(() => resumeStore.state.errorMessage.library);
const defaultResume = computed(() => resumeStore.state.library.defaultResume);
const historyList = computed(() => resumeStore.state.library.history);
const hasDefaultResume = computed(() => !!defaultResume.value);
const hasHistoryRecords = computed(() => historyList.value.length > 0);
const isLibraryEmpty = computed(
  () =>
    libraryStatus.value === REQUEST_STATUS.success &&
    !hasDefaultResume.value &&
    !hasHistoryRecords.value,
);

const defaultResumeMeta = computed(() => {
  if (!defaultResume.value) {
    return '';
  }

  return joinMeta(
    formatFileSize(defaultResume.value.sizeInMb, defaultResume.value.sizeBytes),
    `更新于 ${formatDate(defaultResume.value.updatedAt)}`,
  );
});

const historyItemMeta = (item) =>
  joinMeta(
    formatDateTime(item.updatedAt),
    formatFileSize(item.sizeInMb, item.sizeBytes),
  );

const previewPanel = reactive({
  visible: false,
  loading: false,
  error: '',
  title: '',
  mode: '',
  zoom: 1,
  pdfUrl: '',
  docxHtml: '',
  entry: null,
});

const PREVIEW_ZOOM_MIN = 0.7;
const PREVIEW_ZOOM_MAX = 2.4;
const PREVIEW_ZOOM_STEP = 0.1;

const clampPreviewZoom = (value) => {
  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) {
    return 1;
  }
  return Math.min(PREVIEW_ZOOM_MAX, Math.max(PREVIEW_ZOOM_MIN, numericValue));
};

const previewZoomPercent = computed(() => `${Math.round(previewPanel.zoom * 100)}%`);

const docxPreviewStyle = computed(() => ({
  transform: `scale(${previewPanel.zoom})`,
  transformOrigin: 'top center',
  width: `calc(100% / ${previewPanel.zoom})`,
  margin: '0 auto',
}));

const adjustPreviewZoom = (delta) => {
  previewPanel.zoom = clampPreviewZoom(Number((previewPanel.zoom + delta).toFixed(2)));
};

const zoomInPreview = () => {
  adjustPreviewZoom(PREVIEW_ZOOM_STEP);
};

const zoomOutPreview = () => {
  adjustPreviewZoom(-PREVIEW_ZOOM_STEP);
};

const resetPreviewZoom = () => {
  previewPanel.zoom = 1;
};

const resetPreviewPanelContent = () => {
  previewPanel.loading = false;
  previewPanel.error = '';
  previewPanel.mode = '';
  previewPanel.zoom = 1;
  previewPanel.pdfUrl = '';
  previewPanel.docxHtml = '';
};

const revokePreviewObjectUrl = () => {
  if (!previewPanel.pdfUrl) {
    return;
  }

  URL.revokeObjectURL(previewPanel.pdfUrl);
  previewPanel.pdfUrl = '';
};

const closePreview = () => {
  previewPanel.visible = false;
  previewPanel.title = '';
  previewPanel.entry = null;
  revokePreviewObjectUrl();
  resetPreviewPanelContent();
};

const previewResume = async (entryRef) => {
  const entry = entryRef?.value ?? entryRef;
  previewPanel.visible = true;
  previewPanel.loading = true;
  previewPanel.error = '';
  previewPanel.zoom = 1;
  previewPanel.title = entry?.name || '简历预览';
  previewPanel.entry = entry;
  revokePreviewObjectUrl();
  previewPanel.mode = '';
  previewPanel.docxHtml = '';

  const previewResource = await getResumePreviewResource(entry);
  if (!previewResource?.ok) {
    previewPanel.loading = false;
    previewPanel.error =
      previewResource?.message || 'Unable to open this resume right now. Please try again.';
    return;
  }

  const extension = String(previewResource.extension || '').toLowerCase();
  const mimeType = String(previewResource.blob?.type || '').toLowerCase();

  try {
    if (extension === 'pdf' || mimeType.includes('pdf')) {
      previewPanel.pdfUrl = URL.createObjectURL(previewResource.blob);
      previewPanel.mode = 'pdf';
      previewPanel.loading = false;
      return;
    }

    if (extension === 'docx') {
      const mammoth = await import('mammoth/mammoth.browser.js');
      const arrayBuffer = await previewResource.blob.arrayBuffer();
      const rendered = await mammoth.convertToHtml({ arrayBuffer });
      previewPanel.mode = 'docx';
      previewPanel.docxHtml = rendered?.value || '<p>文档内容为空。</p>';
      previewPanel.loading = false;
      return;
    }

    previewPanel.loading = false;
    previewPanel.error = '当前仅支持 PDF 与 DOCX 可视化预览。DOC 请下载后查看。';
  } catch (error) {
    previewPanel.loading = false;
    previewPanel.error = error?.message || 'Failed to render resume preview.';
  }
};

const handleDocxZoomWheel = (event) => {
  if (!(event.ctrlKey || event.metaKey)) {
    return;
  }

  event.preventDefault();
  adjustPreviewZoom(event.deltaY < 0 ? PREVIEW_ZOOM_STEP : -PREVIEW_ZOOM_STEP);
};

const downloadResume = (entryRef) => {
  const entry = entryRef?.value ?? entryRef;
  const result = openResumeInBrowser(entry, { download: true });
  if (!result?.ok) {
    window.alert(result?.message || 'Unable to download this resume right now. Please try again.');
  }
};

onMounted(() => {
  resumeStore.ensureResumeLibrary();
});

onBeforeUnmount(() => {
  revokePreviewObjectUrl();
});
</script>
<style scoped>
.resume-library {
  width: 100%;
  max-width: 1200px;
  margin-inline: auto;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  padding: 10px 16px 24px;
  box-sizing: border-box;
  background: #eaddd3;
  overflow-y: auto;
}

.state-card {
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

.state-btn {
  margin-top: 16px;
  border: none;
  border-radius: 999px;
  background: #2f2f2f;
  color: #fff;
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 600;
}

.section-container {
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.section-title h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #2c2c2c;
}

.star-icon svg {
  width: 18px;
  height: 18px;
}

.history-icon svg {
  width: 16px;
  height: 16px;
  color: #7a8c52;
}

.empty-state {
  margin-top: 12px;
  padding: 36px 24px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 10px 24px rgba(125, 104, 34, 0.08);
  text-align: center;
}

.empty-icon {
  width: 62px;
  height: 62px;
  margin: 0 auto 16px;
  border-radius: 20px;
  background: #fff6d0;
  color: #9b8651;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-icon svg {
  width: 26px;
  height: 26px;
}

.empty-state h2 {
  margin: 0 0 10px;
  font-size: 18px;
  color: #2c2c2c;
}

.empty-state p {
  margin: 0 0 20px;
  font-size: 13px;
  line-height: 1.6;
  color: #766b4f;
}

.empty-action {
  min-width: 132px;
  height: 42px;
  border: none;
  border-radius: 999px;
  background: #2c2c2c;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.primary-card {
  background: #2c2c2c;
  border-radius: 20px;
  padding: 20px 16px 16px;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.clickable-file {
  cursor: pointer;
}

.file-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #404040;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.file-icon svg {
  width: 22px;
  height: 22px;
}

.file-meta {
  min-width: 0;
}

.file-meta h3 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  word-break: break-all;
}

.file-meta p {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.card-actions {
  display: flex;
  gap: 12px;
}

.polish-btn {
  flex: 1;
  height: 44px;
  border: none;
  border-radius: 14px;
  background: #e2cd6d;
  color: #2c2c2c;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.polish-btn svg {
  width: 18px;
  height: 18px;
}

.download-btn {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 14px;
  background: #404040;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.download-btn svg {
  width: 20px;
  height: 20px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  background: #fff;
  border-radius: 20px;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.item-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.item-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-icon.bg-pink {
  background: #ffefef;
}

.item-icon.bg-blue {
  background: #eaf6ff;
}

.item-icon svg {
  width: 20px;
  height: 20px;
}

.item-meta {
  flex: 1;
  min-width: 0;
}

.item-meta h3 {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
  color: #2c2c2c;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-meta p {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #a2a7af;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease, transform 0.2s ease;
}

.icon-btn:hover {
  color: #5f6470;
  transform: scale(1.05);
}

.icon-btn svg {
  width: 20px;
  height: 20px;
}

.empty-history {
  padding: 18px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.55);
  color: #7d7357;
  font-size: 13px;
  line-height: 1.6;
}

.footer-tip {
  text-align: center;
  font-size: 12px;
  color: #b0b0b0;
  margin-top: 24px;
}

.preview-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(18, 18, 18, 0.58);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  box-sizing: border-box;
}

.preview-dialog {
  width: min(1000px, 100%);
  height: min(90dvh, 860px);
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.preview-header {
  flex-shrink: 0;
  flex-wrap: wrap;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #e7e7e7;
}

.preview-header h3 {
  min-width: 0;
  max-height: 3em;
  overflow-y: auto;
  margin: 0;
  font-size: 15px;
  color: #1f1f1f;
  word-break: break-all;
}

.preview-actions {
  flex-wrap: wrap;
  display: flex;
  align-items: center;
  gap: 8px;
}

.zoom-toolbar {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: 2px;
}

.zoom-btn {
  border: none;
  border-radius: 8px;
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  font-size: 13px;
  font-weight: 700;
  color: #2f2f2f;
  background: #efefef;
}

.zoom-btn-reset {
  min-width: 44px;
}

.zoom-text {
  min-width: 44px;
  text-align: center;
  font-size: 12px;
  color: #545454;
}

.preview-btn {
  border: none;
  border-radius: 10px;
  padding: 7px 12px;
  font-size: 13px;
  font-weight: 700;
  color: #1f1f1f;
  background: #e2cd6d;
}

.preview-btn-close {
  background: #efefef;
  color: #404040;
}

.preview-body {
  min-height: 0;
  flex: 1;
  background: #f8f8f8;
  overflow-y: auto;
  overflow-x: hidden;
}

.preview-state {
  margin: 0;
  padding: 20px;
  color: #545454;
  font-size: 14px;
}

.preview-pdf {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
  display: block;
}

.preview-docx {
  min-height: 100%;
  padding: 24px;
  box-sizing: border-box;
  background: #fff;
  color: #1f1f1f;
  line-height: 1.7;
  font-size: 15px;
  overflow-x: hidden;
}

.preview-docx-viewport {
  min-height: 100%;
  width: 100%;
  overflow-x: hidden;
}

.preview-docx :deep(p) {
  margin: 0 0 12px;
}

.preview-docx :deep(*) {
  max-width: 100% !important;
  box-sizing: border-box;
}

.preview-docx :deep(img) {
  max-width: 100% !important;
  height: auto !important;
}

.preview-docx :deep(table) {
  width: 100% !important;
  table-layout: fixed;
  border-collapse: collapse;
}

.preview-docx :deep(td),
.preview-docx :deep(th) {
  word-break: break-word;
  overflow-wrap: anywhere;
}

.preview-docx :deep(pre),
.preview-docx :deep(code),
.preview-docx :deep(span),
.preview-docx :deep(div) {
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

@media (max-width: 640px) {
  .preview-mask {
    padding: 0;
  }

  .preview-dialog {
    width: 100vw;
    height: 100dvh;
    border-radius: 0;
  }

  .preview-header {
    padding: 10px;
  }

  .preview-actions {
    gap: 6px;
  }

  .zoom-text {
    min-width: 38px;
  }
}
.section-container, .primary-card, .history-item { min-width: 0; }
.card-actions { flex-wrap: wrap; }

@media (min-width: 768px) {
  .history-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>

