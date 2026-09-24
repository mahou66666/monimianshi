import { ref } from 'vue';
const preferenceKey = 'interview-studio:interviewer-voice';
let initial = false;
try { initial = localStorage.getItem(preferenceKey) === 'on'; } catch {}
export const interviewerVoiceEnabled = ref(initial);
export function setInterviewerVoice(enabled) {
  interviewerVoiceEnabled.value = enabled;
  try { localStorage.setItem(preferenceKey, enabled ? 'on' : 'off'); } catch {}
}

// Isolate browser speech and invalidate callbacks from cancelled/previous questions.
export function createQuestionSpeech(synthesis, Utterance) {
  let generation = 0, current = null;
  const supported = Boolean(synthesis && Utterance);
  function stop() {
    generation++;
    if (current) { current.onstart = current.onend = current.onerror = null; synthesis.cancel(); current = null; }
  }
  function speak(text, handlers = {}) {
    stop();
    if (!supported || !text.trim()) return false;
    const version = generation;
    const utterance = new Utterance(text);
    utterance.lang = 'zh-CN'; utterance.rate = 1; utterance.pitch = 1;
    const voices = synthesis.getVoices();
    utterance.voice = voices.find(v => /^zh[-_]CN$/i.test(v.lang) && v.localService)
      || voices.find(v => /^zh[-_]CN$/i.test(v.lang))
      || voices.find(v => /^zh/i.test(v.lang)) || null;
    current = utterance;
    utterance.onstart = () => { if (version === generation) handlers.start?.(); };
    utterance.onend = () => { if (version === generation) { current = null; handlers.end?.(); } };
    utterance.onerror = event => { if (version === generation) { current = null; handlers.error?.(event.error); } };
    try { synthesis.speak(utterance); return true; }
    catch { current = null; handlers.error?.('unavailable'); return false; }
  }
  return { supported, speak, stop };
}
