export const RESUME_SESSION_KEY = 'app_resume_session_v1';
export function readResumeSession(storage, owner, now = Date.now()) {
  try {
    const value = JSON.parse(storage.getItem(RESUME_SESSION_KEY));
    if (!owner || value?.owner !== owner || value.version !== 1 || !Number.isFinite(value.savedAt) || now - value.savedAt > 86400000) return null;
    return value;
  } catch { return null; }
}
export function saveResumeSession(storage, owner, state) {
  try {
    if (!owner) { storage.removeItem(RESUME_SESSION_KEY); return; }
    const complete = state.status.analysis === 'success' && state.analysisResult && state.uploadResult?.resumeId;
    storage.setItem(RESUME_SESSION_KEY, JSON.stringify({version:1, owner, savedAt:Date.now(), targetJdText:state.targetJdText,
      uploadResult: complete ? {resumeId:state.uploadResult.resumeId,fileName:state.uploadResult.fileName} : null,
      analysisResult: complete ? state.analysisResult : null}));
  } catch { /* Storage may be unavailable or full; keep the current page usable. */ }
}
