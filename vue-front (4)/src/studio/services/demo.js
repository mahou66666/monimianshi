import { roles, getRole, dimensions } from './roles.js';
import { findAudioSample } from './audioSamples.js';
import { resolveSpeechMetrics } from './speechFallback.js';

export const STORAGE_KEY = 'interview-studio:demo:v1:local-learner';
const clone = (value) => JSON.parse(JSON.stringify(value));
const uid = () => globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`;
const levels = ['校招', '初级', '中级'];

export function evaluateAnswer(question, answer) {
  const hits = question.keywords.filter((word) => answer.includes(word));
  const missing = question.keywords.filter((word) => !answer.includes(word));
  // A transparent demo rubric, not an AI judgement or a validated competency measure.
  const score = Math.min(94, 42 + hits.length * 10 + Math.min(12, Math.floor(answer.trim().length / 10)));
  return { score, hits, missing, highlight: hits.length ? `提到了${hits.join('、')}，覆盖了部分考察点。` : '已记录你的回答，可以对照参考思路进一步补充。',
    advice: missing.length ? `建议补充${missing.join('、')}，结合具体场景说明取舍。` : '关键点覆盖较完整，下一步补充边界条件和可验证的结果。' };
}

export function makeReport(session) {
  const reviews = session.answers.map((answer) => {
    const q = session.questions.find((item) => item.id === answer.questionId);
    return { ...answer, question: q.text, topic: q.topic, stage: q.stage, reference: q.sample, lesson: q.lesson, ...evaluateAnswer(q, answer.text) };
  });
  const overall = reviews.length ? Math.round(reviews.reduce((n, r) => n + r.score, 0) / reviews.length) : null;
  const scores = dimensions.map((name, i) => ({ name, value: reviews.length ? Math.round(reviews.reduce((n, r) => n + (i === 0 ? r.score : i === 1 ? Math.min(100, 35 + r.hits.length * 14) : i === 2 ? Math.min(95, 45 + Math.floor(r.text.length / 3)) : 40 + r.hits.length * 13), 0) / reviews.length) : null }));
  const weak = [...reviews].sort((a, b) => a.score - b.score).filter((r, index, all) => all.findIndex((x) => x.topic === r.topic) === index).slice(0, 3);
  const audioReviews = reviews.filter(r => r.inputType === 'demo-voice' && findAudioSample(r.audioSampleId));
  const speech = audioReviews.length ? { available: true, source: 'demo-audio', score: 76, clips: audioReviews.map(r => ({ questionId: r.questionId, topic: r.topic, ...findAudioSample(r.audioSampleId) })), reason: '合成示例音频与预设表达分析，不是你的真实录音或能力评价。' } : { available: false, reason: session.answers.some(a => a.inputType === 'demo-voice') ? '使用了示例转写，未关联有效音频，表达指标未评估。' : '本次未采集真实语音，表达声学指标未评估。' };
  speech.fallback = resolveSpeechMetrics({ hasAudio: speech.available, allowMock: true });
  return { overall, dimensions: scores, reviews, weaknesses: weak.map((r) => ({ topic: r.topic, advice: r.advice, lesson: r.lesson, questionId: r.questionId })),
    speech,
    rubricVersion: 'demo-keyword-v1', source: 'demo' };
}

function buildSession(input, id, date) {
  const role = getRole(input.roleId);
  if (!role) throw new Error('请选择有效岗位。');
  const level = levels.includes(input.level) ? input.level : '校招';
  const selected = input.focusTopic ? role.questions.filter((q) => q.topic === input.focusTopic) : role.questions;
  if (!selected.length) throw new Error('专项主题与岗位不匹配。');
  const questionCount = [2, 4].includes(Number(input.questionCount)) ? Number(input.questionCount) : 4;
  const questions = clone(selected.slice(0, questionCount));
  const jd = String(input.jd || role.jd).slice(0, 12000);
  const resume = input.resumeMode === 'skip' ? '' : String(input.resume || role.resume).slice(0, 12000);
  if (resume && questions[0]?.stage === '项目深挖') questions[0].text = `结合你本次的项目经历，${questions[0].text}`;
  return { id, roleId: role.id, roleName: role.name, level, mode: input.mode === 'simulation' ? 'simulation' : 'coaching',
    source: 'demo', status: 'ready', createdAt: date, completedAt: null, parentPlanId: input.parentPlanId || null,
    context: { jd, resume, resumeMode: input.resumeMode || 'sample', roleSkills: [...role.skills] },
    questions, mainCount: questions.length, answers: [], draft: '', draftInputType: 'text', followupAdded: false, report: null,
    focusTopic: input.focusTopic || '', rubricVersion: 'demo-keyword-v1' };
}

export function createDemoService({ storage, now = () => new Date().toISOString(), id = uid, seed = true } = {}) {
  let warning = '';
  let db;
  const validSession = (s) => s && typeof s.id === 'string' && getRole(s.roleId) && s.source === 'demo' && ['ready', 'in_progress', 'paused', 'completed'].includes(s.status) && s.context && typeof s.context.jd === 'string' && typeof s.context.resume === 'string' && Array.isArray(s.context.roleSkills) && Number.isFinite(Date.parse(s.createdAt)) && (s.status !== 'completed' || Number.isFinite(Date.parse(s.completedAt))) && Array.isArray(s.questions) && s.questions.length > 0 && s.questions.every((q) => q && typeof q.id === 'string' && typeof q.text === 'string' && typeof q.sample === 'string' && typeof q.lesson === 'string' && typeof q.topic === 'string' && Array.isArray(q.keywords) && q.keywords.every(w => typeof w === 'string')) && Array.isArray(s.answers) && s.answers.length <= s.questions.length && s.answers.every((a, i) => a && a.questionId === s.questions[i].id && typeof a.text === 'string');
  try {
    const raw = storage?.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed.version !== 1 || !Array.isArray(parsed.sessions) || !parsed.sessions.every(validSession) || !Array.isArray(parsed.plans) || !parsed.plans.every((p) => p && typeof p.id === 'string' && getRole(p.roleId) && typeof p.lesson === 'string' && typeof p.draft === 'string' && Array.isArray(p.steps) && p.steps.length === 3 && parsed.sessions.some(s => s.id === p.sessionId) && (!p.retestId || parsed.sessions.some(s => s.id === p.retestId)))) throw new Error('invalid');
      db = parsed;
      if (!getRole(db.preference)) db.preference = 'java';
      if (db.preparation && (!getRole(db.preparation.roleId) || typeof db.preparation.jd !== 'string' || typeof db.preparation.resume !== 'string')) db.preparation = null;
      db.sessions.forEach((s) => { if (s.status === 'completed') s.report = makeReport(s); });
    }
  } catch { warning = '本地演示记录无法读取，已载入新的示例。真实服务数据不受影响。'; }
  if (!db) {
    db = { version: 1, sessions: [], plans: [], preference: 'java', preparation: null };
    if (seed) {
      for (const role of roles) {
        for (let attempt = 0; attempt < 3; attempt++) {
          const date = new Date(new Date(now()).getTime() - (9 - attempt * 3) * 86400000).toISOString();
          const s = buildSession({ roleId: role.id }, `example-${role.id}-${attempt}`, date);
          s.answers = s.questions.map((q, i) => ({ questionId: q.id, text: attempt === 2 ? q.sample : `${q.keywords.slice(0, attempt + 1 + (i % 2)).join('、')}是我首先考虑的方向，我会结合项目场景进一步验证。`, inputType: 'text', answeredAt: date }));
          s.status = 'completed'; s.completedAt = date; s.seeded = true; s.report = makeReport(s); db.sessions.push(s);
        }
      }
      const active = buildSession({ roleId: 'java' }, 'example-java-continue', now());
      active.status = 'paused'; active.seeded = true;
      active.answers.push({ questionId: active.questions[0].id, text: active.questions[0].sample, inputType: 'text', answeredAt: now() });
      db.sessions.unshift(active);
    }
  }
  const persist = () => {
    try { if (!storage) throw new Error('unavailable'); storage.setItem(STORAGE_KEY, JSON.stringify(db)); }
    catch { warning = '浏览器存储不可用或空间不足，本次进度仅保存在当前页面；关闭后可能丢失。'; }
  };
  persist();
  const find = (sessionId) => { const s = db.sessions.find((item) => item.id === sessionId); if (!s) throw new Error('找不到这次面试，请从历史记录重新进入。'); return s; };
  const finish = (s) => { s.analysis = { status: 'pending', step: 0, error: '' }; s.status = 'completed'; s.completedAt = now(); s.report = makeReport(s); s.draft = ''; };
  return {
    snapshot: () => ({ ...clone(db), warning }),
    getSession: (sessionId) => clone(find(sessionId)),
    savePreparation(value) { db.preparation = clone(value); persist(); },
    setPreference(roleId) { if (!getRole(roleId)) throw new Error('岗位不存在。'); db.preference = roleId; persist(); },
    createSession(input) {
      const s = buildSession(input, id(), now()); db.sessions.unshift(s); db.preference = s.roleId; db.preparation = null; persist(); return clone(s);
    },
    setInputMode(sessionId, mode) { const s = find(sessionId); if (s.status === 'completed') throw Error('面试已完成。'); s.inputMode = mode === 'demo-audio' ? 'demo-audio' : 'text'; persist(); },
    startSession(sessionId) { const s = find(sessionId); if (s.status !== 'completed') { s.status = 'in_progress'; persist(); } return clone(s); },
    pauseSession(sessionId) { const s = find(sessionId); if (s.status === 'in_progress') { s.status = 'paused'; persist(); } },
    saveDraft(sessionId, draft, inputType = 'text', audioSampleId = null) { const s = find(sessionId); if (s.status !== 'completed') { s.draft = String(draft).slice(0, 8000); s.draftInputType = inputType === 'demo-voice' ? 'demo-voice' : 'text'; s.draftAudioSampleId = inputType === 'demo-voice' && findAudioSample(audioSampleId) ? audioSampleId : null; persist(); } },
    submitAnswer(sessionId, { questionId, text, inputType = 'text', audioSampleId = null }) {
      const s = find(sessionId);
      if (s.status === 'completed') throw new Error('这次面试已完成，请查看报告。');
      const q = s.questions[s.answers.length];
      if (q?.id !== questionId) throw new Error('这一题已提交，请刷新当前题目。');
      const answer = String(text || '').trim();
      if (answer.length < 8) throw new Error('请至少输入 8 个字，描述你的思路。');
      if (answer.length > 8000) throw new Error('回答不能超过 8000 字。');
      const audio = inputType === 'demo-voice' ? findAudioSample(audioSampleId) : null;
      if (audio && audio.roleId !== s.roleId) throw Error('音频与岗位不匹配。');
      s.answers.push({ questionId, text: answer, inputType, audioSampleId: audio?.id || null, originalTranscript: audio?.text || null, answeredAt: now() }); s.draftAudioSampleId = null; s.draft = ''; s.status = 'in_progress';
      const evaluation = evaluateAnswer(q, answer);
      if (!s.followupAdded && q.followup && !q.isFollowup) {
        const followText = evaluation.missing.length ? `你还没有展开「${evaluation.missing[0]}」。${q.followup}` : q.followup;
        s.questions.splice(s.answers.length, 0, { ...clone(q), id: `${q.id}-followup`, parentId: q.id, text: followText, isFollowup: true, followup: '' });
        s.followupAdded = true;
      }
      if (s.answers.length === s.questions.length) finish(s);
      persist(); return clone(s);
    },
    finishSession(sessionId) { const s = find(sessionId); if (!s.answers.length) throw new Error('请先完成至少一道题，再生成报告。'); if (s.status !== 'completed') finish(s); persist(); return clone(s); },
    createPractice(sessionId, topic) {
      const s = find(sessionId); if (!s.report) throw new Error('请先完成面试。');
      const weak = s.report.weaknesses.find((w) => w.topic === topic) || s.report.weaknesses[0];
      if (!weak) throw new Error('暂无可用的练习建议。');
      const existing = db.plans.find((p) => p.sessionId === sessionId && p.topic === weak.topic);
      if (existing) return clone(existing);
      const q = s.questions.find((item) => item.id === weak.questionId);
      const p = { id: id(), sessionId, roleId: s.roleId, level: s.level, topic: weak.topic, lesson: weak.lesson, question: q.text, reference: q.sample,
        steps: [false, false, false], draft: '', status: 'in_progress', createdAt: now(), retestId: null };
      db.plans.unshift(p); persist(); return clone(p);
    },
    updatePractice(planId, patch) {
      const p = db.plans.find((item) => item.id === planId); if (!p) throw new Error('训练计划不存在。');
      if (Array.isArray(patch.steps) && patch.steps.length === 3) p.steps = patch.steps.map(Boolean);
      if (typeof patch.draft === 'string') p.draft = patch.draft.slice(0, 8000);
      p.status = p.steps.every(Boolean) && p.draft.trim().length >= 8 ? 'completed' : 'in_progress'; persist(); return clone(p);
    },
    startRetest(planId) {
      const p = db.plans.find((item) => item.id === planId); if (!p || p.status !== 'completed') throw new Error('请完成学习步骤和练习回答后再测。');
      if (p.retestId) return clone(find(p.retestId));
      const original = find(p.sessionId);
      const s = buildSession({ roleId: p.roleId, level: p.level, focusTopic: p.topic, parentPlanId: p.id, jd: original.context.jd, resume: original.context.resume, resumeMode: original.context.resumeMode }, id(), now());
      db.sessions.unshift(s); p.retestId = s.id; persist(); return clone(s);
    },
    advanceAnalysis(sessionId) { const s = find(sessionId); if (s.status !== 'completed') throw Error('请先结束面试。'); if (!s.analysis) s.analysis = { status: 'pending', step: 0, error: '' }; if (s.analysis.status === 'failed' || s.analysis.status === 'completed') return clone(s); s.analysis.step = Math.min(3, s.analysis.step + 1); s.analysis.status = s.analysis.step === 3 ? 'completed' : 'pending'; persist(); return clone(s); },
    failAnalysis(sessionId) { const s = find(sessionId); if (s.status !== 'completed') throw Error('请先结束面试。'); if (s.analysis?.status === 'completed') return; s.analysis = { status: 'failed', step: s.analysis?.step || 0, error: '已模拟分析中断。回答已保存，可以重试或先查看已有内容报告。' }; persist(); },
    retryAnalysis(sessionId) { const s = find(sessionId); if (s.analysis?.status === 'failed') { s.analysis.status = 'pending'; s.analysis.error = ''; persist(); } },
    reset() { db = { version: 1, sessions: [], plans: [], preference: 'java', preparation: null }; persist(); },
  };
}
