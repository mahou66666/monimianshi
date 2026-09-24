<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { realRequest } from '../services/real.js';

const route = useRoute();

const roleLabels = {
  java: 'Java 后端工程师',
  web: 'Web 前端工程师',
  algorithm: '算法工程师',
  testing: '测试工程师',
  unknown: '未指定岗位',
};
const roleOptions = Object.entries(roleLabels);
const kindLabels = { interview: '正式面试', practice: '专项再练' };
const statusLabels = {
  completed: '已完成',
  ready: '待回答',
  busy: '处理中',
  uncertain: '待核对',
  in_progress: '进行中',
  pending: '进行中',
};

const rows = ref([]);
const selectedRole = ref('all');
const loading = ref(true);
const error = ref('');
const unauthorized = ref(false);

const loginTarget = computed(() => ({
  path: '/login',
  query: { returnTo: route.fullPath },
}));

function timeValue(value) {
  if (value === null || value === undefined || value === '') return 0;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 0 : date.getTime();
}

function normalizeRows(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.rows)) return payload.rows;
  return [];
}

const visibleRows = computed(() => rows.value
  .filter((row) => selectedRole.value === 'all' || (row.roleId || 'unknown') === selectedRole.value)
  .slice()
  .sort((a, b) => {
    const left = timeValue(a.completedAt ?? a.created_at);
    const right = timeValue(b.completedAt ?? b.created_at);
    return right - left;
  }));

function roleLabel(roleId) {
  return roleLabels[roleId] || roleLabels.unknown;
}

function kindLabel(kind) {
  return kindLabels[kind] || '记录';
}

function statusLabel(status) {
  return statusLabels[status] || status || '状态未提供';
}

function isCompleted(row) {
  return row?.status === 'completed' || Boolean(row?.completedAt);
}

function reportTarget(row) {
  if (!row?.id) return '/interviews/real';
  return isCompleted(row)
    ? `/interviews/real/${encodeURIComponent(row.id)}/report`
    : { path: '/interviews/real', query: { id: row.id } };
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

function scoreLabel(value) {
  if (value === null || value === undefined || value === '') return '未评分';
  const score = Number(value);
  return Number.isFinite(score) && score >= 0 && score <= 10 ? `${score}/10` : '未评分';
}

async function load() {
  loading.value = true;
  error.value = '';
  unauthorized.value = false;
  try {
    rows.value = normalizeRows(await realRequest('/interviews'));
  } catch (err) {
    error.value = err?.message || '服务返回异常，请稍后重试。';
    unauthorized.value = err?.status === 401;
    rows.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="page-heading real-page-heading">
    <div>
      <p class="eyebrow">INTERVIEW HISTORY</p>
      <h1>面试记录</h1>
      <p class="muted">查看账户保存的面试与专项再练。</p>
    </div>
    <div class="real-history-actions">
      <RouterLink class="button primary" to="/interviews/real">新面试</RouterLink>
    </div>
  </div>

  <p v-if="error" class="error" role="alert">{{ error }}</p>
  <div v-if="unauthorized" class="notice real-login-notice">
    <RouterLink :to="loginTarget" class="text-link">登录账户后查看记录 →</RouterLink>
  </div>

  <section v-if="loading" class="card real-state" aria-live="polite">正在加载面试记录…</section>

  <template v-else>
    <div class="real-history-toolbar">
      <label>岗位筛选
        <select v-model="selectedRole">
          <option value="all">全部岗位</option>
          <option v-for="[id, label] in roleOptions" :key="id" :value="id">{{ label }}</option>
        </select>
      </label>
      <span class="muted small">{{ visibleRows.length }} 条记录</span>
      
    </div>

    <section v-if="visibleRows.length" class="card real-history-list">
      <article v-for="row in visibleRows" :key="row.id" class="real-history-row">
        <div class="real-history-main">
          <div class="real-history-title">
            <span class="real-role-dot" aria-hidden="true"></span>
            <div>
              <h2>{{ roleLabel(row.roleId) }}</h2>
              <p class="small muted">{{ kindLabel(row.kind) }} · {{ dateLabel(row.created_at) }}</p>
            </div>
          </div>
          <p class="small muted">
            {{ statusLabel(row.status) }} · {{ row.turnCount ?? '—' }} 轮 · {{ durationLabel(row.recordedDurationMs) }}
          </p>
        </div>
        <div class="real-history-side">
          <strong v-if="row.overallScore !== null && row.overallScore !== undefined" class="record-score">{{ scoreLabel(row.overallScore) }}</strong>
          <span v-else class="small muted">未评分</span>
          <RouterLink class="button secondary" :to="reportTarget(row)">
            {{ isCompleted(row) ? '查看报告' : '继续面试' }} →
          </RouterLink>
        </div>
      </article>
    </section>

    <section v-else class="card empty real-state">
      <h2>{{ rows.length ? '当前岗位暂无记录' : '还没有面试记录' }}</h2>
      <p>{{ rows.length ? '换一个岗位筛选，或开始一次新的面试。' : '完成一次面试后，记录会出现在这里。' }}</p>
      <RouterLink class="button primary" to="/interviews/real">开始面试</RouterLink>
    </section>
  </template>
</template>

<style scoped>
.real-page-heading{align-items:flex-start}.real-history-actions{display:flex;align-items:center;gap:12px;flex-shrink:0}.real-history-toolbar{display:flex;align-items:flex-end;gap:20px;flex-wrap:wrap;margin-bottom:20px}.real-history-toolbar label{min-width:210px;margin:0}.real-history-toolbar>.text-link{margin-left:auto}.real-history-list{padding-top:8px;padding-bottom:8px}.real-history-row{display:flex;justify-content:space-between;align-items:center;gap:20px;padding:20px 0;border-bottom:1px solid var(--line)}.real-history-row:last-child{border-bottom:0}.real-history-main{min-width:0}.real-history-title{display:flex;align-items:center;gap:12px}.real-history-title h2{font-size:17px}.real-history-title p{margin:3px 0}.real-role-dot{display:inline-block;width:10px;height:10px;border-radius:50%;background:#d5ad4b;box-shadow:0 0 0 5px #f7f0d8;flex-shrink:0}.real-history-side{display:flex;align-items:center;gap:18px;flex-shrink:0}.real-history-side .record-score{font-size:22px;color:#9b8338;white-space:nowrap}.real-state{margin-top:8px}.real-login-notice{margin-bottom:18px}.real-login-notice .text-link{padding:0}@media(max-width:767px){.real-page-heading{display:block}.real-history-actions{margin-top:14px;justify-content:space-between}.real-history-toolbar{align-items:stretch;gap:10px}.real-history-toolbar label{flex:1;min-width:160px}.real-history-toolbar>.text-link{margin-left:0;width:100%}.real-history-row{align-items:flex-start;flex-direction:column;gap:13px;padding:20px 0}.real-history-side{width:100%;justify-content:space-between}.real-history-side .button{min-height:42px}.real-history-list{padding-top:8px}}
</style>
