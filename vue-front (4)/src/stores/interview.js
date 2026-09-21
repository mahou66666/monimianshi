import { reactive } from 'vue';
import {
  answerInterview,
  getInterviewState,
  startInterview,
  transcribeInterviewAudio,
} from '@/api/interview';
import { closeInterviewWs, isInterviewWsEnabled, sendInterviewWsRequest } from '@/api/interviewWs';
import {
  DEFAULT_INTERVIEW_GREETING,
  INTERVIEW_RECORDING_STATE,
  INTERVIEW_STATUS,
  REQUEST_STATUS,
} from '@/utils/constants';

const wait = (delay) => new Promise((resolve) => setTimeout(resolve, delay));

const ACTIVE_STAGES = new Set([
  INTERVIEW_RECORDING_STATE.requestingPermission,
  INTERVIEW_RECORDING_STATE.recording,
  INTERVIEW_RECORDING_STATE.uploading,
  INTERVIEW_RECORDING_STATE.transcribing,
  INTERVIEW_RECORDING_STATE.replying,
]);

const PROCESSING_STAGES = new Set([
  INTERVIEW_RECORDING_STATE.requestingPermission,
  INTERVIEW_RECORDING_STATE.uploading,
  INTERVIEW_RECORDING_STATE.transcribing,
  INTERVIEW_RECORDING_STATE.replying,
]);

const DEFAULT_META_ERROR = 'Failed to load interview context. Please retry later.';
const DEFAULT_FLOW_ERROR = 'Interview flow failed. Please retry later.';
const INTERVIEW_HISTORY_STORAGE_KEY = 'app_interview_history_cache';
const INTERVIEW_HISTORY_MAX_ITEMS = 50;
const INTERVIEW_HISTORY_MAX_DIALOGUE_ITEMS = 240;
const TARGET_ASR_SAMPLE_RATE = 16000;
const TARGET_ASR_CHANNELS = 1;
const MAX_ASR_DURATION_SECONDS = (() => {
  const configured = Number(import.meta.env.VITE_ASR_MAX_DURATION_SECONDS || 600);
  return Number.isFinite(configured) && configured > 0 ? Math.floor(configured) : 600;
})();
const ASR_SEGMENT_DURATION_SECONDS = (() => {
  const configured = Number(import.meta.env.VITE_ASR_SEGMENT_DURATION_SECONDS || 45);
  return Number.isFinite(configured) && configured > 5 ? Math.floor(configured) : 45;
})();
const ASR_ENABLE_AGGRESSIVE_DSP =
  String(import.meta.env.VITE_ASR_ENABLE_AGGRESSIVE_DSP || 'false') === 'true';
const ASR_PREFER_PCM_RECORDER =
  String(import.meta.env.VITE_ASR_PREFER_PCM_RECORDER || 'false') !== 'false';
const ASR_ENABLE_ECHO_CANCELLATION =
  String(import.meta.env.VITE_ASR_ENABLE_ECHO_CANCELLATION || 'true') === 'true';
const ASR_ENABLE_NOISE_SUPPRESSION =
  String(import.meta.env.VITE_ASR_ENABLE_NOISE_SUPPRESSION || 'true') === 'true';
const ASR_ENABLE_AUTO_GAIN_CONTROL =
  String(import.meta.env.VITE_ASR_ENABLE_AUTO_GAIN_CONTROL || 'true') === 'true';
const LOOPBACK_AUDIO_INPUT_PATTERN =
  /stereo mix|loopback|what u hear|monitor|virtual|vb-audio|cable output|blackhole|soundflower/i;
const MIN_EFFECTIVE_PEAK = 0.003;
const MILD_INPUT_TARGET_PEAK = 0.12;
const MILD_INPUT_MAX_GAIN = 20;
const DEFAULT_MAX_QUESTIONS = (() => {
  const configured = Number(import.meta.env.VITE_INTERVIEW_MAX_QUESTIONS || 6);
  return Number.isFinite(configured) && configured > 0 ? Math.floor(configured) : 6;
})();
const INTERVIEW_WS_DISABLED = String(import.meta.env.VITE_DISABLE_INTERVIEW_WS || 'false') === 'true';

const createInitialState = () => ({
  meta: {
    jobTitle: INTERVIEW_STATUS.loading,
    statusText: INTERVIEW_STATUS.ongoing,
  },
  chatList: [{ ...DEFAULT_INTERVIEW_GREETING }],
  isTalking: false,
  recordingState: INTERVIEW_RECORDING_STATE.idle,
  status: REQUEST_STATUS.idle,
  metaStatus: REQUEST_STATUS.idle,
  statusHint: 'Click mic to start recording.',
  permissionState: 'idle',
  errorMessage: '',
  metaErrorMessage: '',
  pendingTranscript: '',
  interviewCompleted: false,
  summary: null,
});

const state = reactive(createInitialState());

let mediaStream = null;
let mediaRecorder = null;
let recordingMode = 'none';
let recordedChunks = [];
let recordingStartedAt = 0;
let pendingStopResolver = null;
let activeSpeechUtterance = null;
let activeSessionId = 0;
let backendInterviewSessionId = '';
let recordingAudioContext = null;
let recordingSourceNode = null;
let recordingProcessorNode = null;
let recordingSilenceGainNode = null;
let recordingSampleRate = TARGET_ASR_SAMPLE_RATE;
let recordedPcmChunks = [];

const backendInterviewProfile = Object.freeze({
  candidateName: 'Candidate',
  targetIndustry: 'finance',
  jobDescription:
    'Backend service development role with focus on system design, engineering quality, and collaboration.',
  resumeHighlights:
    'Has backend engineering experience with performance optimization and production troubleshooting.',
  maxQuestions: DEFAULT_MAX_QUESTIONS,
});

const normalizeErrorMessage = (error, fallback) =>
  error?.message || error?.payload?.message || error?.payload?.error || error?.payload?.data?.error || fallback;

const safeParseJson = (value, fallback = null) => {
  if (!value) {
    return fallback;
  }

  try {
    return JSON.parse(value);
  } catch (error) {
    return fallback;
  }
};

const loadInterviewHistory = () => {
  if (typeof window === 'undefined') {
    return [];
  }

  const parsed = safeParseJson(window.localStorage.getItem(INTERVIEW_HISTORY_STORAGE_KEY), []);
  return Array.isArray(parsed) ? parsed : [];
};

const saveInterviewHistory = (history) => {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(INTERVIEW_HISTORY_STORAGE_KEY, JSON.stringify(history));
};

const toLimitedText = (value, maxLength = 180) => {
  const normalized = String(value || '').replace(/\s+/g, ' ').trim();
  if (!normalized) {
    return '';
  }
  if (normalized.length <= maxLength) {
    return normalized;
  }
  return `${normalized.slice(0, maxLength)}...`;
};

const resolveInterviewScore = (scoreSummary) => {
  if (typeof scoreSummary === 'number' && Number.isFinite(scoreSummary)) {
    return scoreSummary;
  }

  if (!scoreSummary || typeof scoreSummary !== 'object') {
    return null;
  }

  const candidates = [
    scoreSummary.total,
    scoreSummary.overall,
    scoreSummary.overallScore,
    scoreSummary.score,
    scoreSummary.finalScore,
  ];

  for (const candidate of candidates) {
    const value = Number(candidate);
    if (Number.isFinite(value)) {
      return value;
    }
  }

  return null;
};

const normalizeHistoryDialogueItem = (item) => {
  if (!item || typeof item !== 'object') {
    return null;
  }

  const role = item.role === 'user' ? 'user' : item.role === 'agent' ? 'agent' : '';
  const content = String(item.content || '').trim();

  if (!role || !content) {
    return null;
  }

  return {
    role,
    content,
    source: String(item.source || '').trim(),
  };
};

const sanitizeHistoryDialogue = (dialogue) => {
  if (!Array.isArray(dialogue) || !dialogue.length) {
    return [];
  }

  const normalized = dialogue.map(normalizeHistoryDialogueItem).filter(Boolean);
  if (!normalized.length) {
    return [];
  }

  return normalized.slice(-INTERVIEW_HISTORY_MAX_DIALOGUE_ITEMS);
};

const buildHistoryDialogueSnapshot = () =>
  sanitizeHistoryDialogue(
    state.chatList.map((item) => ({
      role: item.role,
      content: item.content,
      source: item.source || '',
    })),
  );

const upsertInterviewHistoryEntry = (payload = {}) => {
  const sessionId = String(payload.sessionId || '').trim() || `local-${Date.now()}`;
  const history = loadInterviewHistory();
  const existing = history.find((item) => item.id === sessionId || item.sessionId === sessionId) || null;
  const nextDialogue = sanitizeHistoryDialogue(payload.dialogue);

  const entry = {
    id: sessionId,
    sessionId,
    createdAt: payload.createdAt || existing?.createdAt || new Date().toISOString(),
    targetIndustry: payload.targetIndustry || backendInterviewProfile.targetIndustry || '',
    candidateName: payload.candidateName || backendInterviewProfile.candidateName || '',
    status: payload.status || 'completed',
    questionCount: Number(payload.questionCount || 0),
    maxQuestions: Number(payload.maxQuestions || 0),
    score: resolveInterviewScore(payload.scoreSummary),
    summaryText: toLimitedText(
      payload.finalReport || payload.roundFeedback || payload.reply || existing?.summaryText || '',
      220,
    ),
    dialogue: nextDialogue.length ? nextDialogue : sanitizeHistoryDialogue(existing?.dialogue),
  };

  const nextHistory = [entry, ...history.filter((item) => item.id !== entry.id)].slice(
    0,
    INTERVIEW_HISTORY_MAX_ITEMS,
  );
  saveInterviewHistory(nextHistory);
};

const normalizeAgentPayload = (response) => {
  const data = response?.data ?? response ?? {};
  const eventLogFromEnvelope = Array.isArray(response?.eventLog) ? response.eventLog : [];
  const eventLogFromData = Array.isArray(data.event_log)
    ? data.event_log
    : Array.isArray(data.eventLog)
      ? data.eventLog
      : [];
  return {
    sessionId: data.session_id || data.sessionId || '',
    candidateName: data.candidate_name || data.candidateName || backendInterviewProfile.candidateName,
    targetIndustry: data.target_industry || data.targetIndustry || backendInterviewProfile.targetIndustry,
    reply: data.reply || '',
    replyType: data.reply_type || data.replyType || '',
    interviewCompleted: Boolean(data.interview_completed ?? data.interviewCompleted),
    roundFeedback: data.round_feedback || data.roundFeedback || '',
    finalReport: data.final_report || data.finalReport || '',
    status: data.status || '',
    scoreSummary: data.score_summary || data.scoreSummary || null,
    questionCount: Number(data.question_count || data.questionCount || 0),
    maxQuestions: Number(data.max_questions || data.maxQuestions || 0),
    eventLog: eventLogFromEnvelope.length ? eventLogFromEnvelope : eventLogFromData,
  };
};

const updateSummaryState = (payload) => {
  state.interviewCompleted = Boolean(payload.interviewCompleted);
  if (!payload.interviewCompleted) {
    state.summary = null;
    return;
  }

  state.summary = {
    scoreSummary: payload.scoreSummary || null,
    finalReport: payload.finalReport || '',
    reply: payload.reply || '',
    roundFeedback: payload.roundFeedback || '',
    status: payload.status || 'completed',
    questionCount: payload.questionCount || 0,
    maxQuestions: payload.maxQuestions || 0,
    targetIndustry: payload.targetIndustry || backendInterviewProfile.targetIndustry,
    candidateName: payload.candidateName || backendInterviewProfile.candidateName,
    sessionId: payload.sessionId || '',
  };

  upsertInterviewHistoryEntry(payload);
};

const buildAgentReplyText = (payload) => {
  const reply = String(payload.reply || '').trim();
  const roundFeedback = String(payload.roundFeedback || '').trim();
  const finalReport = String(payload.finalReport || '').trim();

  if (payload.replyType === 'report') {
    return [reply, finalReport].filter(Boolean).join('\n\n').trim() || roundFeedback;
  }

  // During interview rounds, display the next question first.
  return reply || roundFeedback;
};

const shouldUseInterviewWs = () => !INTERVIEW_WS_DISABLED && isInterviewWsEnabled();

const buildWsStartPayload = (sessionId = '') => ({
  session_id: sessionId,
  candidate_name: backendInterviewProfile.candidateName,
  target_industry: backendInterviewProfile.targetIndustry,
  job_description: backendInterviewProfile.jobDescription,
  resume_highlights: backendInterviewProfile.resumeHighlights,
  max_questions: backendInterviewProfile.maxQuestions,
});

const requestInterviewStart = async (sessionId = '') => {
  if (shouldUseInterviewWs()) {
    try {
      return await sendInterviewWsRequest('start', buildWsStartPayload(sessionId));
    } catch (error) {
      console.warn('Interview websocket start failed, fallback to HTTP', error);
    }
  }
  return startInterview({
    ...backendInterviewProfile,
    sessionId,
  });
};

const requestInterviewState = async (sessionId) => {
  if (shouldUseInterviewWs()) {
    try {
      return await sendInterviewWsRequest('state', { session_id: sessionId });
    } catch (error) {
      console.warn('Interview websocket state failed, fallback to HTTP', error);
    }
  }
  return getInterviewState({ sessionId });
};

const requestInterviewAnswer = async ({ sessionId, answer }) => {
  if (shouldUseInterviewWs()) {
    try {
      return await sendInterviewWsRequest('answer', {
        session_id: sessionId,
        answer,
      });
    } catch (error) {
      console.warn('Interview websocket answer failed, fallback to HTTP', error);
    }
  }
  return answerInterview({ sessionId, answer });
};

const appendAgentEvents = (payload) => {
  const events = Array.isArray(payload.eventLog) ? payload.eventLog : [];
  if (!events.length) {
    const text = buildAgentReplyText(payload);
    if (text) {
      appendAgentMessage(text, { source: 'text-reply' });
    }
    return text;
  }

  let spokenText = '';
  let appended = 0;
  events.forEach((event) => {
    const kind = String(event?.kind || '').trim();
    const content = String(event?.content || '').trim();
    if (!content) {
      return;
    }

    if (kind === 'round_feedback') {
      appendAgentMessage(content, { source: 'round-feedback' });
      appended += 1;
      return;
    }

    if (kind === 'message') {
      appendAgentMessage(content, { source: 'question' });
      spokenText = content;
      appended += 1;
    }
  });

  if (!appended) {
    const fallbackText = buildAgentReplyText(payload);
    if (fallbackText) {
      appendAgentMessage(fallbackText, { source: 'text-reply' });
      spokenText = fallbackText;
    }
  }

  return spokenText;
};

const clearPendingTranscript = () => {
  state.pendingTranscript = '';
};

const canUseMediaRecorder = () =>
  typeof window !== 'undefined' &&
  typeof navigator !== 'undefined' &&
  !!navigator.mediaDevices?.getUserMedia &&
  typeof MediaRecorder !== 'undefined';

const canUseAudioCapture = () =>
  typeof window !== 'undefined' &&
  typeof navigator !== 'undefined' &&
  !!navigator.mediaDevices?.getUserMedia &&
  (canUseMediaRecorder() || !!getAudioContextCtor());

const canUseSpeechSynthesis = () =>
  typeof window !== 'undefined' &&
  'speechSynthesis' in window &&
  typeof SpeechSynthesisUtterance !== 'undefined';

const getAudioContextCtor = () =>
  (typeof window !== 'undefined' && (window.AudioContext || window.webkitAudioContext)) || null;

const buildAudioCaptureConstraints = (deviceId = '') => ({
  channelCount: TARGET_ASR_CHANNELS,
  sampleRate: TARGET_ASR_SAMPLE_RATE,
  echoCancellation: ASR_ENABLE_ECHO_CANCELLATION,
  noiseSuppression: ASR_ENABLE_NOISE_SUPPRESSION,
  autoGainControl: ASR_ENABLE_AUTO_GAIN_CONTROL,
  ...(deviceId ? { deviceId: { exact: deviceId } } : {}),
});

const isLoopbackLikeAudioInputLabel = (label) =>
  LOOPBACK_AUDIO_INPUT_PATTERN.test(String(label || '').toLowerCase());

const getAudioTrackDeviceId = (stream) => {
  const track = stream?.getAudioTracks?.()?.[0];
  return String(track?.getSettings?.()?.deviceId || '').trim();
};

const findPreferredAudioInputDevice = async (excludeDeviceId = '') => {
  if (
    typeof navigator === 'undefined' ||
    !navigator.mediaDevices ||
    typeof navigator.mediaDevices.enumerateDevices !== 'function'
  ) {
    return null;
  }

  const devices = await navigator.mediaDevices.enumerateDevices();
  const audioInputs = devices.filter((device) => device.kind === 'audioinput');
  if (!audioInputs.length) {
    return null;
  }

  return (
    audioInputs.find((device) => {
      const deviceId = String(device?.deviceId || '').trim();
      if (!deviceId || deviceId === excludeDeviceId) {
        return false;
      }
      return !isLoopbackLikeAudioInputLabel(device.label);
    }) || null
  );
};

const requestMicrophoneStream = (deviceId = '') =>
  navigator.mediaDevices.getUserMedia({
    audio: buildAudioCaptureConstraints(deviceId),
  });

const downmixToMono = (audioBuffer) => {
  const channelCount = audioBuffer.numberOfChannels;
  const frameCount = audioBuffer.length;
  const mono = new Float32Array(frameCount);

  if (channelCount <= 1) {
    mono.set(audioBuffer.getChannelData(0));
    return mono;
  }

  for (let channel = 0; channel < channelCount; channel += 1) {
    const channelData = audioBuffer.getChannelData(channel);
    for (let i = 0; i < frameCount; i += 1) {
      mono[i] += channelData[i];
    }
  }

  for (let i = 0; i < frameCount; i += 1) {
    mono[i] /= channelCount;
  }

  return mono;
};

const resampleLinear = (input, sourceRate, targetRate) => {
  if (sourceRate === targetRate) {
    return input;
  }

  const sampleRateRatio = sourceRate / targetRate;
  const outputLength = Math.max(1, Math.round(input.length / sampleRateRatio));
  const output = new Float32Array(outputLength);

  for (let i = 0; i < outputLength; i += 1) {
    const sourceIndex = i * sampleRateRatio;
    const leftIndex = Math.floor(sourceIndex);
    const rightIndex = Math.min(leftIndex + 1, input.length - 1);
    const weight = sourceIndex - leftIndex;
    output[i] = input[leftIndex] * (1 - weight) + input[rightIndex] * weight;
  }

  return output;
};

const float32ToPcm16 = (samples) => {
  const output = new Int16Array(samples.length);
  for (let i = 0; i < samples.length; i += 1) {
    const normalized = Math.max(-1, Math.min(1, samples[i]));
    output[i] = normalized < 0 ? normalized * 0x8000 : normalized * 0x7fff;
  }
  return output;
};

const calculatePeak = (samples) => {
  let peak = 0;
  for (let i = 0; i < samples.length; i += 1) {
    const value = Math.abs(samples[i]);
    if (value > peak) {
      peak = value;
    }
  }
  return peak;
};

const removeDcOffset = (samples) => {
  if (!samples.length) {
    return samples;
  }
  let sum = 0;
  for (let i = 0; i < samples.length; i += 1) {
    sum += samples[i];
  }
  const mean = sum / samples.length;
  if (Math.abs(mean) < 1e-5) {
    return samples;
  }
  const output = new Float32Array(samples.length);
  for (let i = 0; i < samples.length; i += 1) {
    output[i] = samples[i] - mean;
  }
  return output;
};

const normalizeGain = (samples, targetPeak = 0.85, maxGain = 12) => {
  const peak = calculatePeak(samples);
  if (peak <= 0) {
    return samples;
  }
  const gain = Math.min(maxGain, targetPeak / peak);
  if (gain <= 1.01) {
    return samples;
  }
  const output = new Float32Array(samples.length);
  for (let i = 0; i < samples.length; i += 1) {
    output[i] = Math.max(-1, Math.min(1, samples[i] * gain));
  }
  return output;
};

const trimSilence = (samples, sampleRate) => {
  if (!samples.length) {
    return samples;
  }
  const peak = calculatePeak(samples);
  const threshold = Math.max(0.0025, peak * 0.03);
  let start = 0;
  let end = samples.length - 1;

  while (start < samples.length && Math.abs(samples[start]) < threshold) {
    start += 1;
  }
  while (end > start && Math.abs(samples[end]) < threshold) {
    end -= 1;
  }

  if (start >= end) {
    return samples;
  }

  const pad = Math.floor(sampleRate * 0.08);
  const safeStart = Math.max(0, start - pad);
  const safeEnd = Math.min(samples.length, end + pad);
  return samples.slice(safeStart, safeEnd);
};

const truncateToMaxDuration = (samples, sampleRate, maxSeconds) => {
  const maxSamples = Math.floor(sampleRate * maxSeconds);
  if (samples.length <= maxSamples) {
    return samples;
  }
  return samples.slice(0, maxSamples);
};

const encodeWavPcm16 = (pcmSamples, sampleRate, channelCount) => {
  const bytesPerSample = 2;
  const byteRate = sampleRate * channelCount * bytesPerSample;
  const blockAlign = channelCount * bytesPerSample;
  const dataByteLength = pcmSamples.length * bytesPerSample;
  const buffer = new ArrayBuffer(44 + dataByteLength);
  const view = new DataView(buffer);

  const writeAscii = (offset, text) => {
    for (let i = 0; i < text.length; i += 1) {
      view.setUint8(offset + i, text.charCodeAt(i));
    }
  };

  writeAscii(0, 'RIFF');
  view.setUint32(4, 36 + dataByteLength, true);
  writeAscii(8, 'WAVE');
  writeAscii(12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, channelCount, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, byteRate, true);
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, 16, true);
  writeAscii(36, 'data');
  view.setUint32(40, dataByteLength, true);

  let offset = 44;
  for (let i = 0; i < pcmSamples.length; i += 1) {
    view.setInt16(offset, pcmSamples[i], true);
    offset += 2;
  }

  return buffer;
};

const parseWavPcm16 = async (wavBlob) => {
  const wavBuffer = await wavBlob.arrayBuffer();
  const view = new DataView(wavBuffer);
  if (view.byteLength < 44) {
    throw new Error('Invalid WAV payload');
  }

  const readAscii = (offset, length) => {
    let text = '';
    for (let i = 0; i < length; i += 1) {
      text += String.fromCharCode(view.getUint8(offset + i));
    }
    return text;
  };

  if (readAscii(0, 4) !== 'RIFF' || readAscii(8, 4) !== 'WAVE') {
    throw new Error('Invalid WAV header');
  }

  let sampleRate = TARGET_ASR_SAMPLE_RATE;
  let channelCount = TARGET_ASR_CHANNELS;
  let bitsPerSample = 16;
  let dataOffset = -1;
  let dataByteLength = 0;

  let cursor = 12;
  while (cursor + 8 <= view.byteLength) {
    const chunkId = readAscii(cursor, 4);
    const chunkSize = view.getUint32(cursor + 4, true);
    const chunkDataOffset = cursor + 8;

    if (chunkId === 'fmt ' && chunkDataOffset + 16 <= view.byteLength) {
      const audioFormat = view.getUint16(chunkDataOffset, true);
      channelCount = view.getUint16(chunkDataOffset + 2, true);
      sampleRate = view.getUint32(chunkDataOffset + 4, true);
      bitsPerSample = view.getUint16(chunkDataOffset + 14, true);
      if (audioFormat !== 1) {
        throw new Error('Unsupported WAV encoding');
      }
    } else if (chunkId === 'data') {
      dataOffset = chunkDataOffset;
      dataByteLength = Math.min(chunkSize, view.byteLength - chunkDataOffset);
      break;
    }

    cursor = chunkDataOffset + chunkSize + (chunkSize % 2);
  }

  if (dataOffset < 0 || dataByteLength <= 0) {
    throw new Error('WAV data chunk is missing');
  }
  if (channelCount !== TARGET_ASR_CHANNELS || bitsPerSample !== 16) {
    throw new Error('WAV must be 16-bit mono PCM');
  }

  const alignedByteLength = dataByteLength - (dataByteLength % 2);
  const pcm16 = new Int16Array(alignedByteLength / 2);
  for (let i = 0; i < pcm16.length; i += 1) {
    pcm16[i] = view.getInt16(dataOffset + (i * 2), true);
  }

  return {
    pcm16,
    sampleRate,
  };
};

const splitWavBlobForAsr = async (wavBlob, maxSegmentSeconds) => {
  if (!Number.isFinite(maxSegmentSeconds) || maxSegmentSeconds <= 0) {
    return [wavBlob];
  }

  const { pcm16, sampleRate } = await parseWavPcm16(wavBlob);
  const samplesPerSegment = Math.max(1, Math.floor(sampleRate * maxSegmentSeconds));
  if (pcm16.length <= samplesPerSegment) {
    return [wavBlob];
  }

  const segmentBlobs = [];
  for (let start = 0; start < pcm16.length; start += samplesPerSegment) {
    const end = Math.min(pcm16.length, start + samplesPerSegment);
    const segmentBuffer = encodeWavPcm16(
      pcm16.subarray(start, end),
      sampleRate,
      TARGET_ASR_CHANNELS,
    );
    segmentBlobs.push(new Blob([segmentBuffer], { type: 'audio/wav' }));
  }
  return segmentBlobs;
};

const mergeFloat32Chunks = (chunks) => {
  if (!Array.isArray(chunks) || !chunks.length) {
    return new Float32Array(0);
  }

  const totalLength = chunks.reduce((sum, chunk) => sum + (chunk?.length || 0), 0);
  const merged = new Float32Array(totalLength);
  let offset = 0;

  chunks.forEach((chunk) => {
    if (!chunk || !chunk.length) {
      return;
    }
    merged.set(chunk, offset);
    offset += chunk.length;
  });

  return merged;
};

const prepareAsrWavFromFloat32 = (sourceSamples, sourceRate) => {
  if (!sourceSamples?.length) {
    throw new Error('Captured audio is empty');
  }

  // Keep raw waveform characteristics by default.
  // Over-aggressive trim/normalize can distort pronunciation and hurt ASR accuracy.
  const resampled = resampleLinear(sourceSamples, sourceRate, TARGET_ASR_SAMPLE_RATE);
  const dcRemoved = removeDcOffset(resampled);
  const baseProcessed = ASR_ENABLE_AGGRESSIVE_DSP
    ? trimSilence(dcRemoved, TARGET_ASR_SAMPLE_RATE)
    : dcRemoved;
  // Always apply a mild auto-gain for quiet inputs so low-volume microphones can still be transcribed.
  const dspProcessed = ASR_ENABLE_AGGRESSIVE_DSP
    ? normalizeGain(baseProcessed)
    : normalizeGain(baseProcessed, MILD_INPUT_TARGET_PEAK, MILD_INPUT_MAX_GAIN);
  const limited = truncateToMaxDuration(
    dspProcessed,
    TARGET_ASR_SAMPLE_RATE,
    MAX_ASR_DURATION_SECONDS,
  );
  const peak = calculatePeak(limited);
  if (peak < MIN_EFFECTIVE_PEAK) {
    throw new Error('Captured audio level is too low');
  }

  const pcm16 = float32ToPcm16(limited);
  const wavBuffer = encodeWavPcm16(pcm16, TARGET_ASR_SAMPLE_RATE, TARGET_ASR_CHANNELS);
  return new Blob([wavBuffer], { type: 'audio/wav' });
};

const convertAudioBlobTo16kMonoWav = async (audioBlob) => {
  const AudioContextCtor = getAudioContextCtor();
  if (!AudioContextCtor) {
    throw new Error('AudioContext is unavailable in this browser');
  }

  const sourceArrayBuffer = await audioBlob.arrayBuffer();
  const audioContext = new AudioContextCtor();

  try {
    const decodedBuffer = await audioContext.decodeAudioData(sourceArrayBuffer.slice(0));
    const mono = downmixToMono(decodedBuffer);
    return prepareAsrWavFromFloat32(mono, decodedBuffer.sampleRate);
  } finally {
    if (typeof audioContext.close === 'function') {
      await audioContext.close();
    }
  }
};

const isProcessing = () => PROCESSING_STAGES.has(state.recordingState);

const setFlowStatus = (status, errorMessage = '') => {
  state.status = status;
  state.errorMessage = errorMessage;
};

const setMetaStatus = (status, errorMessage = '') => {
  state.metaStatus = status;
  state.metaErrorMessage = errorMessage;
};

const stopSpeechPlayback = () => {
  if (!canUseSpeechSynthesis()) {
    activeSpeechUtterance = null;
    return;
  }

  window.speechSynthesis.cancel();
  activeSpeechUtterance = null;
};

const tearDownPcmRecorder = () => {
  if (recordingProcessorNode) {
    recordingProcessorNode.onaudioprocess = null;
    try {
      recordingProcessorNode.disconnect();
    } catch (error) {
      console.warn('Failed to disconnect processor node', error);
    }
  }
  if (recordingSourceNode) {
    try {
      recordingSourceNode.disconnect();
    } catch (error) {
      console.warn('Failed to disconnect source node', error);
    }
  }
  if (recordingSilenceGainNode) {
    try {
      recordingSilenceGainNode.disconnect();
    } catch (error) {
      console.warn('Failed to disconnect gain node', error);
    }
  }

  const activeContext = recordingAudioContext;
  recordingProcessorNode = null;
  recordingSourceNode = null;
  recordingSilenceGainNode = null;
  recordingAudioContext = null;
  recordingSampleRate = TARGET_ASR_SAMPLE_RATE;

  if (
    activeContext &&
    typeof activeContext.close === 'function' &&
    activeContext.state !== 'closed'
  ) {
    activeContext.close().catch((error) => {
      console.warn('Failed to close recording audio context', error);
    });
  }
};

const startPcmRecorder = async (stream) => {
  const AudioContextCtor = getAudioContextCtor();
  if (!AudioContextCtor) {
    throw new Error('AudioContext is unavailable');
  }

  tearDownPcmRecorder();
  recordedPcmChunks = [];
  recordingAudioContext = new AudioContextCtor();

  if (recordingAudioContext.state === 'suspended') {
    await recordingAudioContext.resume();
  }

  if (typeof recordingAudioContext.createScriptProcessor !== 'function') {
    throw new Error('ScriptProcessor is unavailable');
  }

  recordingSampleRate = recordingAudioContext.sampleRate || TARGET_ASR_SAMPLE_RATE;
  recordingSourceNode = recordingAudioContext.createMediaStreamSource(stream);
  recordingProcessorNode = recordingAudioContext.createScriptProcessor(4096, 1, 1);
  recordingSilenceGainNode = recordingAudioContext.createGain();
  recordingSilenceGainNode.gain.value = 0;

  recordingProcessorNode.onaudioprocess = (event) => {
    const channelData = event.inputBuffer?.getChannelData(0);
    if (!channelData || !channelData.length) {
      return;
    }
    const chunkCopy = new Float32Array(channelData.length);
    chunkCopy.set(channelData);
    recordedPcmChunks.push(chunkCopy);
  };

  recordingSourceNode.connect(recordingProcessorNode);
  recordingProcessorNode.connect(recordingSilenceGainNode);
  recordingSilenceGainNode.connect(recordingAudioContext.destination);
  recordingMode = 'pcm';
};

const releaseMediaResources = () => {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.ondataavailable = null;
    mediaRecorder.onstop = null;
    mediaRecorder.onerror = null;

    try {
      mediaRecorder.stop();
    } catch (error) {
      console.warn('Failed to stop active recorder', error);
    }
  }

  mediaRecorder = null;
  recordingMode = 'none';
  recordedChunks = [];
  recordedPcmChunks = [];
  recordingStartedAt = 0;
  pendingStopResolver = null;
  tearDownPcmRecorder();

  if (mediaStream) {
    mediaStream.getTracks().forEach((track) => track.stop());
    mediaStream = null;
  }
};

const setStage = (stage, overrides = {}) => {
  state.recordingState = stage;

  switch (stage) {
    case INTERVIEW_RECORDING_STATE.requestingPermission:
      setFlowStatus(REQUEST_STATUS.loading);
      state.meta.statusText = overrides.statusText ?? INTERVIEW_STATUS.requestingPermission;
      state.statusHint = overrides.hintText ?? 'Requesting microphone permission...';
      state.isTalking = false;
      break;
    case INTERVIEW_RECORDING_STATE.recording:
      setFlowStatus(REQUEST_STATUS.loading);
      state.meta.statusText = overrides.statusText ?? INTERVIEW_STATUS.recording;
      state.statusHint = overrides.hintText ?? 'Recording... click mic again to stop.';
      state.isTalking = true;
      break;
    case INTERVIEW_RECORDING_STATE.uploading:
      setFlowStatus(REQUEST_STATUS.loading);
      state.meta.statusText = overrides.statusText ?? INTERVIEW_STATUS.uploading;
      state.statusHint = overrides.hintText ?? 'Uploading audio...';
      state.isTalking = false;
      break;
    case INTERVIEW_RECORDING_STATE.transcribing:
      setFlowStatus(REQUEST_STATUS.loading);
      state.meta.statusText = overrides.statusText ?? INTERVIEW_STATUS.transcribing;
      state.statusHint = overrides.hintText ?? 'Transcribing...';
      state.isTalking = false;
      break;
    case INTERVIEW_RECORDING_STATE.replying:
      setFlowStatus(REQUEST_STATUS.loading);
      state.meta.statusText = overrides.statusText ?? INTERVIEW_STATUS.replying;
      state.statusHint = overrides.hintText ?? 'AI is replying...';
      state.isTalking = true;
      break;
    case INTERVIEW_RECORDING_STATE.error:
      setFlowStatus(REQUEST_STATUS.error, overrides.errorMessage ?? DEFAULT_FLOW_ERROR);
      state.meta.statusText = overrides.statusText ?? INTERVIEW_STATUS.error;
      state.statusHint = overrides.hintText ?? overrides.errorMessage ?? DEFAULT_FLOW_ERROR;
      state.isTalking = false;
      break;
    default:
      setFlowStatus(overrides.flowStatus ?? REQUEST_STATUS.idle);
      state.meta.statusText = overrides.statusText ?? INTERVIEW_STATUS.ongoing;
      state.statusHint = overrides.hintText ?? 'You can continue by voice or text.';
      state.isTalking = false;
      break;
  }
};

const appendUserMessage = (content, extras = {}) => {
  state.chatList.push({
    id: `user-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    role: 'user',
    content,
    ...extras,
  });
};

const appendAgentMessage = (content, extras = {}) => {
  state.chatList.push({
    id: `agent-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    role: 'agent',
    content,
    ...extras,
  });
};

const speakAgentReply = (text) =>
  new Promise((resolve) => {
    if (!text || !canUseSpeechSynthesis()) {
      resolve(false);
      return;
    }

    stopSpeechPlayback();

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'zh-CN';
    utterance.rate = 1;
    utterance.pitch = 1;
    utterance.onend = () => {
      activeSpeechUtterance = null;
      resolve(true);
    };
    utterance.onerror = () => {
      activeSpeechUtterance = null;
      resolve(false);
    };

    activeSpeechUtterance = utterance;
    window.speechSynthesis.speak(utterance);
  });

const ensureInterviewStarted = async () => {
  if (backendInterviewSessionId) {
    return backendInterviewSessionId;
  }

  const response = await requestInterviewStart('');
  const payload = normalizeAgentPayload(response);
  if (!payload.sessionId) {
    throw new Error('Backend did not return session_id');
  }
  backendInterviewSessionId = payload.sessionId;
  return backendInterviewSessionId;
};

const ensureMicrophonePermission = async () => {
  if (!canUseAudioCapture()) {
    state.permissionState = 'unsupported';
    setStage(INTERVIEW_RECORDING_STATE.error, {
      statusText: INTERVIEW_STATUS.unsupported,
      errorMessage: INTERVIEW_STATUS.unsupported,
      hintText: 'Current browser does not support recording.',
    });
    return false;
  }

  const hasUsableStream =
    mediaStream && mediaStream.getTracks().some((track) => track.readyState === 'live');

  if (hasUsableStream) {
    state.permissionState = 'granted';
    return true;
  }

  setStage(INTERVIEW_RECORDING_STATE.requestingPermission);

  try {
    mediaStream = await requestMicrophoneStream();
    const activeTrack = mediaStream.getAudioTracks()?.[0];
    const activeLabel = String(activeTrack?.label || '').trim();

    if (isLoopbackLikeAudioInputLabel(activeLabel)) {
      const currentDeviceId = getAudioTrackDeviceId(mediaStream);
      const preferredDevice = await findPreferredAudioInputDevice(currentDeviceId);

      if (preferredDevice?.deviceId) {
        try {
          const switchedStream = await requestMicrophoneStream(preferredDevice.deviceId);
          mediaStream.getTracks().forEach((track) => track.stop());
          mediaStream = switchedStream;
          console.info(
            'Switched loopback-like audio input to preferred microphone device:',
            preferredDevice.label || preferredDevice.deviceId,
          );
        } catch (switchError) {
          console.warn(
            'Failed to switch from loopback-like input device; keep original stream:',
            switchError,
          );
        }
      } else {
        console.warn('Current audio input may be loopback/stereo-mix:', activeLabel);
      }
    }

    state.permissionState = 'granted';
    return true;
  } catch (error) {
    console.error('Failed to acquire microphone permission', error);
    state.permissionState = 'denied';
    setStage(INTERVIEW_RECORDING_STATE.error, {
      statusText: INTERVIEW_STATUS.permissionDenied,
      errorMessage: INTERVIEW_STATUS.permissionDenied,
      hintText: 'Please allow microphone permission and retry.',
    });
    return false;
  }
};

const processRecordedAudio = async (audioBlob, durationMs, options = {}) => {
  const { alreadyPreparedWav = false } = options;

  if (!audioBlob || audioBlob.size <= 0) {
    setStage(INTERVIEW_RECORDING_STATE.error, {
      errorMessage: 'No valid audio captured. Please retry.',
      hintText: 'Hold to speak for a bit longer before stopping.',
    });
    return;
  }
  let wavBlob = null;
  try {
    wavBlob = alreadyPreparedWav ? audioBlob : await convertAudioBlobTo16kMonoWav(audioBlob);
  } catch (error) {
    console.error('Failed to convert recording to 16k mono wav', error);
    const rawMessage = String(error?.message || '').toLowerCase();
    const lowLevelLike =
      rawMessage.includes('captured audio level is too low') ||
      rawMessage.includes('captured audio is empty');
    setStage(INTERVIEW_RECORDING_STATE.error, {
      errorMessage: lowLevelLike
        ? 'No valid speech captured. Input level is too low or wrong microphone device is selected.'
        : 'No valid speech captured. Please check microphone input device and volume.',
      hintText: lowLevelLike
        ? 'Please use a real microphone input (avoid Stereo Mix/Loopback), then speak close to mic for 2-5 seconds.'
        : 'Speak close to mic for 2-5 seconds, then retry.',
    });
    return;
  }
  const fileName = `answer-${Date.now()}.wav`;

  setStage(INTERVIEW_RECORDING_STATE.transcribing, {
    hintText: `Transcribing voice (~${Math.max(1, Math.round(durationMs / 1000))}s)...`,
  });

  try {
    const fileNamePrefix = fileName.replace(/\.wav$/i, '');
    const transcribeSegmentBlobs = async (segmentBlobs, hintBuilder) => {
      const transcriptParts = [];
      for (let i = 0; i < segmentBlobs.length; i += 1) {
        const hintText = hintBuilder?.(i + 1, segmentBlobs.length);
        if (hintText) {
          setStage(INTERVIEW_RECORDING_STATE.transcribing, { hintText });
        }
        const segmentFileName =
          segmentBlobs.length > 1 ? `${fileNamePrefix}-part-${i + 1}.wav` : fileName;
        const transcribeResponse = await transcribeInterviewAudio(segmentBlobs[i], segmentFileName);
        const transcribeData = transcribeResponse?.data ?? transcribeResponse ?? {};
        const backendError = String(transcribeData?.error || '').trim();
        if (backendError) {
          throw new Error(backendError);
        }
        const segmentText = String(transcribeData?.text || '').trim();
        if (segmentText) {
          transcriptParts.push(segmentText);
        }
      }
      return transcriptParts;
    };

    const segmentBlobs = await splitWavBlobForAsr(wavBlob, ASR_SEGMENT_DURATION_SECONDS);
    let transcriptParts = await transcribeSegmentBlobs(segmentBlobs, (index, total) =>
      total > 1 ? `Transcribing segment ${index}/${total}...` : '',
    );
    let transcript = transcriptParts.join(' ').replace(/\s+/g, ' ').trim();

    if (!transcript) {
      // If first pass is empty, retry with finer segmentation to reduce single-call ASR timeout risk.
      const retrySegmentSeconds = Math.max(12, Math.floor(ASR_SEGMENT_DURATION_SECONDS / 2));
      if (retrySegmentSeconds < ASR_SEGMENT_DURATION_SECONDS) {
        setStage(INTERVIEW_RECORDING_STATE.transcribing, {
          hintText: 'First pass returned empty text, retrying with smaller segments...',
        });
        const retryBlobs = await splitWavBlobForAsr(wavBlob, retrySegmentSeconds);
        transcriptParts = await transcribeSegmentBlobs(retryBlobs, (index, total) =>
          total > 1 ? `Retry transcribing segment ${index}/${total}...` : '',
        );
        transcript = transcriptParts.join(' ').replace(/\s+/g, ' ').trim();
      }
    }

    if (!transcript) {
      setStage(INTERVIEW_RECORDING_STATE.error, {
        errorMessage: 'No speech detected (or ASR timeout).',
        hintText: 'Please keep each turn within 20-60 seconds and ensure microphone input is active.',
      });
      return;
    }

    state.pendingTranscript = transcript;
    setStage(INTERVIEW_RECORDING_STATE.idle, {
      flowStatus: REQUEST_STATUS.success,
      statusText: INTERVIEW_STATUS.ongoing,
      hintText: 'Transcription ready. Please confirm or edit before sending.',
    });
  } catch (error) {
    console.error('Failed to transcribe voice message', error);
    const rawMessage = String(error?.message || '').toLowerCase();
    const timeoutLike =
      rawMessage.includes('timeout') ||
      rawMessage.includes('timed out');
    const noSpeechLike =
      rawMessage.includes('no speech') ||
      rawMessage.includes('no valid speech') ||
      rawMessage.includes('captured audio level is too low') ||
      rawMessage.includes('check microphone input device') ||
      rawMessage.includes('empty transcription');
    const errorMessage = timeoutLike
      ? 'ASR backend temporarily unavailable. Please retry.'
      : noSpeechLike
        ? 'No speech detected. Please check selected microphone input device.'
        : normalizeErrorMessage(error, 'Audio transcription failed. Please retry.');
    const hintText = noSpeechLike
      ? 'Windows: System > Sound > Input, choose headset mic then retry.'
      : 'You can retry voice input or switch to text mode.';
    setStage(INTERVIEW_RECORDING_STATE.error, {
      errorMessage,
      hintText,
    });
  }
};

const startRecording = async () => {
  if (isProcessing() || state.recordingState === INTERVIEW_RECORDING_STATE.recording) {
    return;
  }

  stopSpeechPlayback();

  const hasPermission = await ensureMicrophonePermission();

  if (!hasPermission || !mediaStream) {
    return;
  }

  recordingStartedAt = Date.now();
  recordedChunks = [];
  recordedPcmChunks = [];

  if (ASR_PREFER_PCM_RECORDER) {
    try {
      await startPcmRecorder(mediaStream);
      setStage(INTERVIEW_RECORDING_STATE.recording);
      return;
    } catch (pcmError) {
      console.warn('PCM recorder init failed, fallback to MediaRecorder', pcmError);
      tearDownPcmRecorder();
      recordingMode = 'none';
    }
  }

  if (canUseMediaRecorder()) {
    mediaRecorder = new MediaRecorder(mediaStream);
    recordingMode = 'media-recorder';

    mediaRecorder.ondataavailable = (event) => {
      if (event.data && event.data.size > 0) {
        recordedChunks.push(event.data);
      }
    };

    mediaRecorder.onerror = () => {
      recordingMode = 'none';
      setStage(INTERVIEW_RECORDING_STATE.error, {
        errorMessage: 'Recording failed. Please retry.',
        hintText: 'Click the mic button to start recording again.',
      });

      if (pendingStopResolver) {
        pendingStopResolver();
        pendingStopResolver = null;
      }
    };

    mediaRecorder.onstop = async () => {
      const mimeType = mediaRecorder?.mimeType || 'audio/webm';
      const audioBlob = new Blob(recordedChunks, { type: mimeType });
      const durationMs = Math.max(0, Date.now() - recordingStartedAt);

      mediaRecorder = null;
      recordingMode = 'none';
      recordedChunks = [];
      recordingStartedAt = 0;

      try {
        await processRecordedAudio(audioBlob, durationMs);
      } finally {
        if (pendingStopResolver) {
          pendingStopResolver();
          pendingStopResolver = null;
        }
      }
    };

    mediaRecorder.start();
    setStage(INTERVIEW_RECORDING_STATE.recording);
    return;
  }

  try {
    await startPcmRecorder(mediaStream);
    setStage(INTERVIEW_RECORDING_STATE.recording);
    return;
  } catch (pcmError) {
    console.warn('PCM recorder init failed', pcmError);
    tearDownPcmRecorder();
    recordingMode = 'none';
    setStage(INTERVIEW_RECORDING_STATE.error, {
      errorMessage: 'Recording failed. Browser recording API unavailable.',
      hintText: 'Please switch to Chrome/Edge and retry.',
    });
  }
};

const stopRecording = async () => {
  if (recordingMode === 'pcm') {
    const durationMs = Math.max(0, Date.now() - recordingStartedAt);
    const capturedChunks = recordedPcmChunks.slice();
    const sourceRate = recordingSampleRate || TARGET_ASR_SAMPLE_RATE;

    recordingStartedAt = 0;
    recordingMode = 'none';
    recordedPcmChunks = [];
    tearDownPcmRecorder();

    try {
      const mergedPcm = mergeFloat32Chunks(capturedChunks);
      const wavBlob = prepareAsrWavFromFloat32(mergedPcm, sourceRate);
      await processRecordedAudio(wavBlob, durationMs, { alreadyPreparedWav: true });
    } catch (error) {
      console.error('Failed to process PCM recording', error);
      const rawMessage = String(error?.message || '').toLowerCase();
      const lowLevelLike =
        rawMessage.includes('captured audio level is too low') ||
        rawMessage.includes('captured audio is empty');
      setStage(INTERVIEW_RECORDING_STATE.error, {
        errorMessage: lowLevelLike
          ? 'No valid speech captured. Input level is too low or wrong microphone device is selected.'
          : normalizeErrorMessage(error, 'No valid speech captured. Please check microphone input device and volume.'),
        hintText: lowLevelLike
          ? 'Please use a real microphone input (avoid Stereo Mix/Loopback), then speak close to mic for 2-5 seconds.'
          : 'Speak close to mic for 2-5 seconds, then retry.',
      });
    }
    return;
  }

  if (!mediaRecorder || mediaRecorder.state !== 'recording') {
    return;
  }

  const stopPromise = new Promise((resolve) => {
    pendingStopResolver = resolve;
  });

  mediaRecorder.stop();
  await stopPromise;
};

const loadMeta = async () => {
  const sessionId = activeSessionId;
  setMetaStatus(REQUEST_STATUS.loading);

  try {
    let response;
    if (backendInterviewSessionId) {
      response = await requestInterviewState(backendInterviewSessionId);
    } else {
      response = await requestInterviewStart('');
    }

    if (sessionId !== activeSessionId) {
      return;
    }

    const payload = normalizeAgentPayload(response);
    if (payload.sessionId) {
      backendInterviewSessionId = payload.sessionId;
    }
    updateSummaryState(payload);

    state.meta.jobTitle = payload.targetIndustry || 'Mock Interview';
    state.meta.statusText = payload.interviewCompleted ? 'Interview Completed' : INTERVIEW_STATUS.ongoing;
    setMetaStatus(REQUEST_STATUS.success);

    state.chatList.splice(0, state.chatList.length);
    const initialReply = payload.reply || DEFAULT_INTERVIEW_GREETING.content;
    appendAgentMessage(initialReply, { source: 'agent' });
    void speakAgentReply(initialReply);

    setStage(INTERVIEW_RECORDING_STATE.idle, {
      flowStatus: REQUEST_STATUS.success,
      statusText: payload.interviewCompleted ? 'Interview Completed' : INTERVIEW_STATUS.ongoing,
      hintText: payload.interviewCompleted
        ? 'Interview completed. You can restart from home.'
        : 'You can continue by voice or text.',
    });
  } catch (error) {
    console.error('Failed to load interview meta', error);
    state.meta.jobTitle = 'Mock Interview';
    state.meta.statusText = INTERVIEW_STATUS.ongoing;
    setMetaStatus(
      REQUEST_STATUS.error,
      normalizeErrorMessage(error, DEFAULT_META_ERROR),
    );
  }
};

const submitAnswerFlow = async (normalizedText, source = 'text') => {
  const sessionId = activeSessionId;
  try {
    await ensureInterviewStarted();
  } catch (error) {
    setStage(INTERVIEW_RECORDING_STATE.error, {
      errorMessage: normalizeErrorMessage(error, 'Session initialization failed.'),
      hintText: 'Please retry later or refresh the page.',
    });
    return;
  }

  appendUserMessage(normalizedText, { source });
  setStage(INTERVIEW_RECORDING_STATE.replying, {
    hintText: 'AI is generating the next question...',
  });

  try {
    const response = await requestInterviewAnswer({
      sessionId: backendInterviewSessionId,
      answer: normalizedText,
    });

    if (sessionId !== activeSessionId) {
      return;
    }

    const payload = normalizeAgentPayload(response);
    updateSummaryState(payload);
    const replyText = appendAgentEvents(payload) || 'Got it.';
    if (payload.interviewCompleted) {
      upsertInterviewHistoryEntry({
        ...payload,
        dialogue: buildHistoryDialogueSnapshot(),
      });
    }

    if (payload.interviewCompleted) {
      state.meta.statusText = 'Interview Completed';
    }

    const hasPlayedVoice = await speakAgentReply(replyText);

    if (sessionId !== activeSessionId) {
      return;
    }

    if (!hasPlayedVoice) {
      await wait(300);
    }

    setStage(INTERVIEW_RECORDING_STATE.idle, {
      flowStatus: REQUEST_STATUS.success,
      statusText: payload.interviewCompleted ? 'Interview Completed' : INTERVIEW_STATUS.ongoing,
      hintText: payload.interviewCompleted
        ? 'Interview completed. You can restart from home.'
        : source === 'voice'
          ? 'Voice answer submitted. Continue with voice or text.'
          : 'Text answer submitted. Continue with voice or text.',
    });
  } catch (error) {
    console.error('Failed to send text message', error);
    setStage(INTERVIEW_RECORDING_STATE.error, {
      errorMessage: normalizeErrorMessage(error, DEFAULT_FLOW_ERROR),
      hintText: 'Please retry this round or try again later.',
    });
  }
};

const sendTextMessage = async (text) => {
  const normalizedText = text?.trim();

  if (!normalizedText || isProcessing() || state.recordingState === INTERVIEW_RECORDING_STATE.recording) {
    return;
  }
  if (String(state.pendingTranscript || '').trim()) {
    setStage(INTERVIEW_RECORDING_STATE.idle, {
      flowStatus: REQUEST_STATUS.success,
      statusText: INTERVIEW_STATUS.ongoing,
      hintText: 'You have a pending voice transcript. Confirm or cancel it first.',
    });
    return;
  }

  stopSpeechPlayback();
  await submitAnswerFlow(normalizedText, 'text');
};

const confirmPendingTranscript = async (rawText) => {
  if (isProcessing() || state.recordingState === INTERVIEW_RECORDING_STATE.recording) {
    return;
  }
  const normalizedText = String(rawText ?? state.pendingTranscript ?? '').trim();
  if (!normalizedText) {
    setStage(INTERVIEW_RECORDING_STATE.error, {
      errorMessage: 'Transcript is empty. Please re-record.',
      hintText: 'Speak clearly for 2-5 seconds, then retry.',
    });
    clearPendingTranscript();
    return;
  }
  clearPendingTranscript();
  stopSpeechPlayback();
  await submitAnswerFlow(normalizedText, 'voice');
};

const cancelPendingTranscript = () => {
  if (isProcessing() || state.recordingState === INTERVIEW_RECORDING_STATE.recording) {
    return;
  }
  clearPendingTranscript();
  setStage(INTERVIEW_RECORDING_STATE.idle, {
    flowStatus: REQUEST_STATUS.success,
    statusText: INTERVIEW_STATUS.ongoing,
    hintText: 'Voice transcript discarded. You can record again or type your answer.',
  });
};

const handleVoiceAction = async () => {
  if (state.recordingState === INTERVIEW_RECORDING_STATE.recording) {
    await stopRecording();
    return;
  }

  if (isProcessing()) {
    return;
  }

  if (String(state.pendingTranscript || '').trim()) {
    setStage(INTERVIEW_RECORDING_STATE.idle, {
      flowStatus: REQUEST_STATUS.success,
      statusText: INTERVIEW_STATUS.ongoing,
      hintText: 'Please confirm or cancel the pending transcript before recording again.',
    });
    return;
  }

  await startRecording();
};

const reset = () => {
  activeSessionId += 1;
  backendInterviewSessionId = '';
  closeInterviewWs();
  stopSpeechPlayback();
  releaseMediaResources();

  const initialState = createInitialState();
  state.meta.jobTitle = initialState.meta.jobTitle;
  state.meta.statusText = initialState.meta.statusText;
  state.chatList.splice(0, state.chatList.length, ...initialState.chatList);
  state.isTalking = initialState.isTalking;
  state.recordingState = initialState.recordingState;
  state.status = initialState.status;
  state.metaStatus = initialState.metaStatus;
  state.statusHint = initialState.statusHint;
  state.permissionState = initialState.permissionState;
  state.errorMessage = initialState.errorMessage;
  state.metaErrorMessage = initialState.metaErrorMessage;
  state.pendingTranscript = initialState.pendingTranscript;
  state.interviewCompleted = initialState.interviewCompleted;
  state.summary = initialState.summary;
};

export const useInterviewStore = () => ({
  state,
  getSummary: () => state.summary,
  getHistoryRecords: () => loadInterviewHistory(),
  reset,
  loadMeta,
  handleVoiceAction,
  sendTextMessage,
  confirmPendingTranscript,
  cancelPendingTranscript,
  isProcessing,
  isStageActive: () => ACTIVE_STAGES.has(state.recordingState),
});

