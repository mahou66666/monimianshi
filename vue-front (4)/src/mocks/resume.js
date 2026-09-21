import { MOCK_DELAY } from '@/utils/constants';

const wait = (delay) => new Promise((resolve) => setTimeout(resolve, delay));

const resolveVariant = (fileName = '') => {
  const extension = fileName.split('.').pop()?.toLowerCase();
  return extension === 'doc' || extension === 'docx' ? 'blue' : 'pink';
};

const createLibraryEntry = ({
  id,
  fileName,
  sizeBytes,
  updatedAt,
  isDefault = false,
}) => ({
  id,
  name: fileName,
  sizeBytes,
  sizeInMb: Number((sizeBytes / 1024 / 1024).toFixed(1)),
  updatedAt,
  variant: resolveVariant(fileName),
  actionLabel: '继续润色',
  isDefault,
});

const toHistoryEntry = (entry) => ({
  id: entry.id,
  name: entry.name,
  sizeBytes: entry.sizeBytes,
  sizeInMb: entry.sizeInMb,
  updatedAt: entry.updatedAt,
  variant: entry.variant,
});

const uploadGuide = {
  uploadTitle: '点击或拖拽上传',
  uploadDescription: '支持 PDF、DOC、DOCX 格式（最大 10MB）',
  jdPlaceholder: '粘贴目标岗位的 JD（Job Description），AI 会针对性提取关键词并优化你的简历内容。',
  agentTitle: 'AI 简历优化引擎',
  agentDescription:
    '上传后，AI 将从专业度、排版结构和 JD 匹配度三个维度进行诊断，并给出一键优化建议。',
};

const resumeAnalysisResult = {
  score: 82,
  total: 100,
  badges: ['排版良好', 'JD 匹配度：75%'],
  sections: [
    {
      id: 'work-experience',
      title: '工作经历',
      status: '待优化',
      originalText: '负责公司前端开发，做了几个 Vue 项目，修复了一些 bug。',
      suggestions: ['缺少量化数据', '动词不够专业', '未体现业务价值'],
      actionLabel: '一键智能重写',
    },
    {
      id: 'project-experience',
      title: '项目经历',
      status: '已优化',
      optimizedText:
        '使用 Uni-app + Vue.js 开发电商跨端小程序，日活跃用户突破 5 万，通过优化骨架屏与长列表渲染，将首屏加载时间降低至 1.2 秒。',
      primaryActionLabel: '应用修改',
      secondaryActionLabel: '撤销还原',
    },
    {
      id: 'education',
      title: '教育背景',
      status: '待优化',
    },
  ],
  finalActionLabel: '生成最终简历',
};

const resumeLibrary = {
  defaultResume: null,
  history: [],
};

export const getResumeUploadGuide = async () => {
  await wait(MOCK_DELAY.fast);

  return {
    code: 200,
    data: uploadGuide,
  };
};

export const uploadResume = async (formData) => {
  await wait(MOCK_DELAY.normal);

  const selectedFile = formData?.get?.('file');
  const resumeId = `r${Date.now()}`;
  const fileName = selectedFile?.name ?? '前端开发工程师_原始简历.pdf';
  const sizeBytes = Number(selectedFile?.size) || Math.round(2.1 * 1024 * 1024);
  const updatedAt = new Date().toISOString();
  const previousDefault = resumeLibrary.defaultResume;
  const defaultEntry = createLibraryEntry({
    id: resumeId,
    fileName,
    sizeBytes,
    updatedAt,
    isDefault: true,
  });
  const sectionCounts = {
    WORK_EXPERIENCE: 1,
    INTERNSHIP_EXPERIENCE: 1,
    PROJECT_EXPERIENCE: 1,
    EDUCATION: 1,
    SKILLS: 1,
    SELF_EVALUATION: 1,
  };

  resumeLibrary.defaultResume = defaultEntry;
  resumeLibrary.history = [
    ...(previousDefault && previousDefault.id !== resumeId ? [toHistoryEntry(previousDefault)] : []),
    ...resumeLibrary.history.filter((item) => item.id !== resumeId && item.id !== previousDefault?.id),
  ].slice(0, 10);

  return {
    code: 200,
    data: {
      resumeId,
      fileName,
      fragmentCount: Object.values(sectionCounts).reduce((sum, value) => sum + Number(value || 0), 0),
      sectionCounts,
      parseStatus: 'success',
      optimizeStatus: 'success',
      sizeBytes,
      sizeInMb: defaultEntry.sizeInMb,
      updatedAt,
      variant: defaultEntry.variant,
      actionLabel: defaultEntry.actionLabel,
    },
  };
};

export const analyzeResume = async () => {
  await wait(MOCK_DELAY.normal);

  return {
    code: 200,
    data: resumeAnalysisResult,
  };
};

export const getResumeLibrary = async () => {
  await wait(MOCK_DELAY.fast);

  return {
    code: 200,
    data: resumeLibrary,
  };
};
