export const REQUEST_STATUS = Object.freeze({
  idle: 'idle',
  loading: 'loading',
  success: 'success',
  error: 'error',
});

export const INTERVIEW_STATUS = Object.freeze({
  loading: '加载中...',
  ongoing: '模拟面试进行中',
  requestingPermission: '请求权限中...',
  recording: '录音中...',
  uploading: '上传中...',
  transcribing: '识别中...',
  replying: 'AI 回复中...',
  thinking: 'AI 思考中...',
  listening: '倾听中...',
  permissionDenied: '麦克风权限被拒绝',
  unsupported: '当前浏览器不支持录音',
  error: '录音处理失败',
});

export const INTERVIEW_RECORDING_STATE = Object.freeze({
  idle: 'idle',
  requestingPermission: 'requesting-permission',
  recording: 'recording',
  uploading: 'uploading',
  transcribing: 'transcribing',
  replying: 'replying',
  error: 'error',
});

export const MOCK_DELAY = Object.freeze({
  fast: 200,
  normal: 450,
  slow: 900,
});

export const DEFAULT_INTERVIEW_GREETING = Object.freeze({
  id: 1,
  role: 'agent',
  content: '你好，我是 AI 面试官。请先做一个简短的自我介绍。',
});

export const RESUME_UPLOAD = Object.freeze({
  maxSizeBytes: 10 * 1024 * 1024,
  allowedExtensions: ['pdf', 'doc', 'docx'],
  accept:
    '.pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document',
});
