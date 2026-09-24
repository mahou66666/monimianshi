<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { realRequest } from '../services/real.js';
import ExpressionDemo from '../components/ExpressionDemo.vue';
import ReportOverview from '../components/ReportOverview.vue';

const route = useRoute();
const report = ref(null);
const loading = ref(true);
const error = ref('');
const unauthorized = ref(false);
const reviewFilter=ref('all');
const weakTurn=computed(()=>turns.value.filter(canRetry).slice().sort((a,b)=>scoreValue(a.score.score)-scoreValue(b.score.score))[0]||null);
const visibleTurns=computed(()=>turns.value.filter(t=>reviewFilter.value==='all'||(reviewFilter.value==='scored'?hasScore(t):!!t.speechMetrics)));
const scoredCount=computed(()=>turns.value.filter(hasScore).length);

const reportId = computed(() => String(route.params.id || route.params.sessionId || route.query.id || ''));
const loginTarget = computed(() => ({
  path: '/login',
  query: { returnTo: route.fullPath },
}));

const roleLabels = {
  java: 'Java 后端工程师',
  web: 'Web 前端工程师',
  algorithm: '算法工程师',
  testing: '测试工程师',
  unknown: '未指定岗位',
};
const statusLabels = { completed: '已完成', ready: '进行中', pending: '结果待确认', confirmed: '已确认' };

function asObject(value) {
  if (value && typeof value === 'object') return value;
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value);
      return parsed && typeof parsed === 'object' ? parsed : {};
    } catch {
      return {};
    }
  }
  return {};
}

const result = computed(() => asObject(report.value?.result));
const scoreSummary = computed(() => asObject(result.value.score_summary || result.value.scoreSummary));
const finalReport = computed(() => {
  const value = result.value.final_report ?? result.value.finalReport ?? result.value.report;
  if (typeof value === 'string') return value;
  return value && typeof value === 'object' ? JSON.stringify(value, null, 2) : '';
});
const turns = computed(() => (Array.isArray(report.value?.turns) ? report.value.turns : []));
const competencies = computed(() => (Array.isArray(scoreSummary.value.competencies) ? scoreSummary.value.competencies : []));

const overallScore = computed(() => {
  const candidates = [
    scoreSummary.value.overall_score,
    scoreSummary.value.overallScore,
    result.value.overall_score,
    result.value.overallScore,
    report.value?.overallScore,
  ];
  for (const candidate of candidates) {
    const score = Number(candidate);
    if (candidate !== null && candidate !== undefined && candidate !== '' && Number.isFinite(score) && score >= 0 && score <= 10) return score;
  }
  return null;
});

function roleLabel(roleId) {
  return roleLabels[roleId] || roleLabels.unknown;
}

function statusLabel(status) {
  return statusLabels[status] || status || '状态未提供';
}

function scoreValue(score) {
  if (score === null || score === undefined || score === '') return null;
  const value = Number(score);
  return Number.isFinite(value) && value >= 0 && value <= 10 ? value : null;
}

function scoreLabel(score) {
  const value = scoreValue(score);
  return value === null ? '未评分' : `${value}/10`;
}

function hasScore(turn) {
  return turn?.status === 'confirmed' && scoreValue(turn?.score?.score) !== null && Number(turn?.roundNo) !== 0;
}

function canRetry(turn) {
  return report.value?.status === 'completed' && hasScore(turn);
}

function retryTarget(turn) {
  return {
    path: '/interviews/real',
    query: {
      sourceSessionId: report.value?.id || reportId.value,
      sourceRound: turn.roundNo,
    },
  };
}

function formatValue(value, emptyText = '未记录') {
  if (value === null || value === undefined || value === '') return emptyText;
  if (typeof value === 'string') return value;
  try { return JSON.stringify(value, null, 2); } catch { return String(value); }
}

function dateLabel(value) {
  if (!value) return '时间未记录';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '时间未记录';
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(date);
}

function durationLabel(value) {
  if (value === null || value === undefined || value === '') return '时长未记录';
  const milliseconds = Number(value);
  if (!Number.isFinite(milliseconds) || milliseconds < 0) return '时长未记录';
  const seconds = Math.floor(milliseconds / 1000);
  if (seconds < 60) return `${seconds} 秒`;
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return rest ? `${minutes} 分 ${rest} 秒` : `${minutes} 分钟`;
}

function speechMetric(turn, key) {
  const raw=turn?.speechMetrics?.[key];
  if(raw===null||raw===undefined||raw==='')return null;
  const value = Number(raw);
  return Number.isFinite(value) ? value : null;
}

function rateLabel(turn) {
  const value = speechMetric(turn, 'speechRateCharsPerMin');
  return value === null ? '未记录' : `${value} 字/分钟`;
}

function pauseLabel(turn) {
  const count = speechMetric(turn, 'pauseCount');
  const longest = speechMetric(turn, 'longestPauseMs');
  if (count === null) return '未记录';
  return `${count} 次${longest !== null ? ` · 最长 ${Math.round(longest / 100) / 10} 秒` : ''}`;
}

function turnTitle(turn, index) {
  if (Number(turn?.roundNo) === 0) return '自我介绍';
  return `正式题 ${turn?.roundNo ?? index}`;
}

async function reviewRound(round){
  reviewFilter.value='all';
  await nextTick();
  const el=document.getElementById(`review-round-${round}`);
  el?.scrollIntoView({behavior:window.matchMedia('(prefers-reduced-motion: reduce)').matches?'auto':'smooth',block:'start'});
  el?.focus({preventScroll:true});
}

async function load() {
  loading.value = true;
  error.value = '';
  unauthorized.value = false;
  if (!reportId.value) {
    error.value = '缺少真实面试记录 ID。';
    loading.value = false;
    return;
  }
  try {
    report.value = await realRequest(`/interviews/${encodeURIComponent(reportId.value)}/report`);
  } catch (err) {
    error.value = err?.message || '真实服务返回异常，请稍后重试。';
    unauthorized.value = err?.status === 401;
    report.value = null;
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="page-heading real-page-heading">
    <div>
      <p class="eyebrow">看见收获，找到下一步</p>
      <h1>面试报告</h1>
      <p v-if="report" class="muted">{{ roleLabels[report.roleId] && report.roleId!=='unknown' ? roleLabel(report.roleId)+' · ' : '' }}{{ dateLabel(report.createdAt) }}</p>
      <p v-else class="muted">来自真实账户的逐题记录与评分证据。</p>
    </div>
    <div class="real-report-actions">
      <span v-if="report" class="status-badge" :class="'status-'+report.status">{{statusLabel(report.status)}}</span>
      <RouterLink class="text-link" to="/interviews/real/history">返回面试记录 →</RouterLink>
    </div>
  </div>

  <p v-if="error" class="error" role="alert">{{ error }}</p>
  <div v-if="unauthorized" class="notice real-login-notice">
    登录状态已过期。<RouterLink :to="loginTarget" class="text-link">登录后返回本页 →</RouterLink>
  </div>
  <section v-if="loading" class="card real-state" aria-live="polite">正在整理报告…</section>

  <template v-else-if="report">
    <p v-if="report.legacyIncomplete" class="notice legacy-notice">
      这条记录来自较早的存档结构，部分字段没有保存。页面不会为缺失的回答、评分或时长补造内容。
    </p>
    <ReportOverview :score="overallScore" :competencies="competencies" :turns="turns" :completed="report.status==='completed'" :retry-target="weakTurn?retryTarget(weakTurn):null" @review="reviewRound"/>
    <div class="report-quick-facts"><span><strong>{{turns.filter(t=>t.status==='confirmed').length}}</strong> 轮已回答</span><span><strong>{{scoredCount}}</strong> 道已评分正式题</span><span v-if="report.recordedDurationMs!=null">累计作答 {{durationLabel(report.recordedDurationMs)}}</span><a href="#question-review" class="text-link">查看逐题复盘 ↓</a></div>
    <p v-if="report.sourceSessionId" class="small muted">本次为专项再练 · <RouterLink class="text-link" :to="`/interviews/real/${encodeURIComponent(report.sourceSessionId)}/report`">回看原报告 →</RouterLink></p>
    <ExpressionDemo v-if="report.status==='completed'" :session-id="reportId"/>

    <section id="question-review" class="card real-turns-card">
      <div class="section-heading"><div><h2>逐题复盘</h2><p class="small muted">保留面试官问题与原始回答；自我介绍不参与评分。</p></div></div>
      <div class="review-filters" role="group" aria-label="筛选复盘题目"><button v-for="item in [{id:'all',label:'全部问答'},{id:'scored',label:'已评分题目'},{id:'voice',label:'含语音证据'}]" :key="item.id" type="button" :aria-pressed="reviewFilter===item.id" :class="{active:reviewFilter===item.id}" @click="reviewFilter=item.id">{{item.label}}</button></div><p v-if="!visibleTurns.length" class="empty">{{turns.length?'当前筛选下没有记录，请切换查看其他问答。':'这条记录没有保存逐题内容。'}}</p>
      <article v-for="(turn, index) in visibleTurns" :key="`${turn.roundNo ?? index}-${turn.askedAt ?? index}`" class="real-turn" :id="`review-round-${turn.roundNo}`" tabindex="-1">
        <div class="real-turn-heading">
          <div><span class="eyebrow">{{ turnTitle(turn, index) }}</span><h3>{{ ({intro:'开场介绍',technical:'技术面试',domain_expert:'技术面试',hr:'行为面试'})[turn.interviewer] || formatValue(turn.interviewer, '面试官') }}</h3></div>
          <div class="real-turn-status">
            <span class="badge">{{ Number(turn.roundNo) === 0 ? '不评分' : statusLabel(turn.status) }}</span>
            <strong v-if="hasScore(turn)" :class="{ 'weak-score': scoreValue(turn.score.score) < 6 }">{{ scoreLabel(turn.score.score) }}</strong>
            <span v-else-if="Number(turn.roundNo) !== 0 && turn.status === 'pending'" class="small muted">结果待确认</span>
            <span v-else-if="Number(turn.roundNo) !== 0" class="small muted">未评分</span>
          </div>
        </div>
        <div class="real-turn-body">
          <div><h4>问题原文</h4><p class="preserve">{{ formatValue(turn.question, '问题原文未记录') }}</p></div>
          <div><h4>回答原文</h4><p class="preserve answer-review">{{ formatValue(turn.answer, '回答尚未提交') }}</p></div>
        </div>
        <div v-if="turn.speechMetrics" class="turn-speech-metrics">
          <div><span>有效语速</span><strong>{{ rateLabel(turn) }}</strong></div>
          <div><span>停顿</span><strong>{{ pauseLabel(turn) }}</strong></div>
          <div><span>有效说话时长</span><strong>{{ durationLabel(turn.speechMetrics.speechDurationMs) }}</strong></div>
          <p class="small muted">来源：本题录音的 VAD 分段与原始转写。语速和停顿是音频证据，不代表情感或自信度判断。</p>
        </div>
        <p v-if="!turn.speechMetrics" class="small muted speech-unavailable">本题未采集语音指标。</p><div class="real-turn-foot">
          <div class="real-turn-feedback">
            <p v-if="turn.feedback" class="preserve small"><strong>反馈：</strong>{{ formatValue(turn.feedback) }}</p>
            <p v-if="hasScore(turn) && turn.score.competency" class="small"><strong>能力项：</strong>{{ turn.score.competency }}</p>
            <p v-if="hasScore(turn) && turn.score.evidence" class="preserve small"><strong>证据：</strong>{{ turn.score.evidence }}</p>
          </div>
          <div class="real-turn-actions">
            <span class="small muted">{{ dateLabel(turn.askedAt) }} · {{ durationLabel(turn.durationMs) }}</span>
            <RouterLink v-if="canRetry(turn)" class="text-link" :to="retryTarget(turn)">针对这一题再练 →</RouterLink>
          </div>
        </div>
      </article>
      <p class="notice retry-notice">专项再练会携带原题、回答和反馈生成新会话，题目可能调整。面试完成后，已确认且有分数的正式题均可再次练习。</p>
    </section>
  </template>

  <section v-else-if="!loading" class="card empty real-state">
    <h2>暂时无法显示这份报告</h2><p>请稍后重试，或返回面试记录查看其他会话。</p><button v-if="!unauthorized" class="button primary" @click="load">重新加载</button>
    <RouterLink class="button secondary" to="/interviews/real/history">返回面试记录</RouterLink>
  </section>
</template>

<style scoped>
.real-page-heading{align-items:flex-start}.real-report-actions{display:flex;align-items:center;gap:18px;flex-shrink:0}.real-login-notice{margin-bottom:18px}.real-login-notice .text-link{padding:0}.legacy-notice{margin-bottom:20px}.real-report-summary{margin-bottom:20px}.wb-scoring-note{margin-bottom:24px}.real-report-summary .summary-score{min-width:150px}.real-report-summary .summary-score strong{font-size:35px;color:#9b8338}.real-report-summary .summary-score b{display:block;margin-top:8px;color:var(--muted);font-size:14px}.real-report-grid{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:24px;margin-bottom:24px}.real-report-text{font-size:14px;line-height:1.9}.preserve{white-space:pre-wrap;overflow-wrap:anywhere}.evidence-row{padding:17px 0;border-bottom:1px solid var(--line)}.evidence-row:last-child{border-bottom:0}.evidence-heading{margin-bottom:8px}.evidence-heading h3{font-size:15px}.evidence-score{color:#9b8338}.real-report-aside{align-self:start;position:sticky;top:24px}.report-meta{margin:18px 0 24px}.report-meta>div{display:flex;justify-content:space-between;gap:16px;padding:11px 0;border-bottom:1px solid var(--line);font-size:12px}.report-meta dt{color:var(--muted)}.report-meta dd{margin:0;text-align:right;overflow-wrap:anywhere}.report-meta dd .text-link{padding:0;min-height:32px}.real-turns-card{margin-bottom:24px}.real-turn{scroll-margin-top:20px;padding:24px 0;border-bottom:1px solid var(--line)}.real-turn:last-of-type{border-bottom:0}.real-turn-heading{display:flex;justify-content:space-between;align-items:flex-start;gap:18px}.real-turn-heading .eyebrow{margin-bottom:4px}.real-turn-heading h3{font-size:15px}.real-turn-status{display:flex;align-items:center;gap:10px;flex-shrink:0}.real-turn-status strong{font-size:21px;color:#9b8338}.real-turn-status .weak-score{color:#a34e35}.real-turn-body{display:grid;grid-template-columns:1fr 1fr;gap:18px;margin-top:15px}.real-turn-body h4{font-size:12px;color:var(--muted);margin:0 0 8px}.real-turn-body p{margin:0;overflow-wrap:anywhere}.real-turn-feedback{min-width:0}.real-turn-foot{display:flex;justify-content:space-between;align-items:flex-end;gap:18px;margin-top:16px}.real-turn-feedback p{margin:4px 0}.real-turn-actions{display:flex;flex-direction:column;align-items:flex-end;gap:4px;flex-shrink:0}.real-turn-actions .text-link{min-height:36px;padding:0}.retry-notice{margin-top:20px}.real-state{margin-top:8px}@media(max-width:900px){.real-report-grid{grid-template-columns:1fr}.real-report-aside{position:static}.real-turn-foot{align-items:flex-start;flex-direction:column}.real-turn-actions{align-items:flex-start}}@media(max-width:767px){.real-page-heading{display:block}.real-report-actions{margin-top:10px;justify-content:space-between}.real-report-summary{display:block;padding:19px}.real-report-summary .summary-score{padding:0;margin-bottom:13px}.real-report-summary>.button{width:100%;margin-top:10px}.real-turn-heading{gap:10px}.real-turn-status{align-items:flex-end;flex-direction:column;gap:4px}.real-turn-body{grid-template-columns:1fr;gap:14px}.real-turn{scroll-margin-top:20px;padding:20px 0}.real-report-aside{padding:20px 18px}}
.turn-speech-metrics{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin-top:18px;padding:14px;background:#f4f6ed;border-radius:10px}.turn-speech-metrics span{display:block;color:var(--muted);font-size:11px}.turn-speech-metrics strong{display:block;margin-top:5px;font-size:13px}.turn-speech-metrics p{grid-column:1/-1;margin:2px 0}@media(max-width:767px){.turn-speech-metrics{grid-template-columns:1fr}}
.report-quick-facts{display:flex;align-items:center;gap:24px;flex-wrap:wrap;margin:0 0 18px;padding:0 4px;color:var(--muted);font-size:13px}.report-quick-facts strong{color:var(--ink);font-size:20px;margin-right:5px}.report-quick-facts a{margin-left:auto}.retry-competency{font-size:20px;font-weight:600}.real-report-aside .button{margin-top:12px}.review-filters{display:flex;gap:8px;flex-wrap:wrap;margin:12px 0 8px}.review-filters button{border:1px solid var(--line);background:var(--paper);border-radius:8px;padding:8px 14px;min-height:44px;font-size:13px}.review-filters button.active{background:#f7edce;border-color:#c7ac55;color:#584719;font-weight:600}.speech-unavailable{margin-top:14px}#question-review{scroll-margin-top:24px}.real-report-text{font-size:15px}.real-turn-actions .text-link{min-height:44px}@media(max-width:767px){.report-quick-facts{gap:12px}.report-quick-facts a{margin-left:0}.review-filters{gap:6px}.review-filters button{padding:8px 10px}.real-report-grid{gap:16px}.real-report-summary>.button{width:100%}}
</style>
