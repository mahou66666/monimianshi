import request from '@/utils/request';

const normalizeStartPayload = (data = {}) => ({
  session_id: data.session_id || data.sessionId || '',
  candidate_name: data.candidate_name || data.candidateName || '',
  target_industry: data.target_industry || data.targetIndustry || '',
  job_description: data.job_description || data.jobDescription || '',
  resume_highlights: data.resume_highlights || data.resumeHighlights || '',
  max_questions: Number(data.max_questions || data.maxQuestions || 3) || 3,
});

const normalizeAnswerPayload = (data = {}) => ({
  session_id: data.session_id || data.sessionId || '',
  answer: data.answer || '',
});

const normalizeStatePayload = (data = {}) => ({
  session_id: data.session_id || data.sessionId || '',
});

export const startInterview = (data) =>
  request.post('/interview/start', normalizeStartPayload(data));

export const answerInterview = (data) =>
  request.post('/interview/answer', normalizeAnswerPayload(data));

export const getInterviewState = (data) =>
  request.post('/interview/state', normalizeStatePayload(data));

export const transcribeInterviewAudio = (audioBlob, fileName = 'answer.wav') => {
  const formData = new FormData();
  formData.append('audio', audioBlob, fileName);
  return request.post('/interview/asr/transcribe', formData);
};
