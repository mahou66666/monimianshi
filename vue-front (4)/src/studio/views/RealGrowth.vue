<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { realRequest } from '../services/real.js';

const route = useRoute();
const growth = ref(null);
const loading = ref(true);
const error = ref('');
const unauthorized = ref(false);

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

const roleRows = computed(() => {
  const roles = Array.isArray(growth.value?.roles) ? growth.value.roles : [];
  return roles.map((role) => ({
    roleId: role?.roleId || 'unknown',
    points: Array.isArray(role?.points) ? role.points.slice().sort((a, b) => {
      const left = timeValue(a?.at);
      const right = timeValue(b?.at);
      return left - right;
    }) : [],
  }));
});

function roleLabel(roleId) {
  return roleLabels[roleId] || roleLabels.unknown;
}

function timeValue(value) {
  if (value === null || value === undefined || value === '') return 0;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 0 : date.getTime();
}

function scoreValue(value) {
  if (value === null || value === undefined || value === '') return null;
  const score = Number(value);
  return Number.isFinite(score) && score >= 0 && score <= 10 ? score : null;
}

function scoreLabel(value) {
  const score = scoreValue(value);
  return score === null ? '未评分' : `${score}/10`;
}

function barWidth(value) {
  const score = scoreValue(value);
  return score === null ? '0%' : `${score * 10}%`;
}

function countLabel(value) {
  if (value === null || value === undefined || value === '') return '未记录';
  const count = Number(value);
  return Number.isFinite(count) && count >= 0 ? String(count) : '未记录';
}

function durationLabel(value) {
  if (value === null || value === undefined || value === '') return '未记录';
  const milliseconds = Number(value);
  if (!Number.isFinite(milliseconds) || milliseconds < 0) return '未记录';
  const seconds = Math.floor(milliseconds / 1000);
  if (seconds < 60) return `${seconds} 秒`;
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return rest ? `${minutes} 分 ${rest} 秒` : `${minutes} 分钟`;
}

function recordedDurationLabel(value, timedAnswerCount) {
  if (timedAnswerCount !== null && timedAnswerCount !== undefined && timedAnswerCount !== '' && Number(timedAnswerCount) === 0) return '未记录';
  return durationLabel(value);
}

function dateLabel(value) {
  if (!value) return '时间未记录';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '时间未记录';
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(date);
}

async function load() {
  loading.value = true;
  error.value = '';
  unauthorized.value = false;
  try {
    growth.value = await realRequest('/interviews/growth');
  } catch (err) {
    error.value = err?.message || '服务返回异常，请稍后重试。';
    unauthorized.value = err?.status === 401;
    growth.value = null;
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="page-heading real-page-heading">
    <div>
      <p class="eyebrow">GROWTH</p>
      <h1>成长记录</h1>
      <p class="muted">按岗位查看完成记录与已保存的评分时间序列。</p>
    </div>
  </div>

  <p v-if="error" class="error" role="alert">{{ error }}</p>
  <div v-if="unauthorized" class="notice real-login-notice">
    登录状态已过期。<RouterLink :to="loginTarget" class="text-link">登录账户后返回本页 →</RouterLink>
  </div>
  <section v-if="loading" class="card real-state" aria-live="polite">正在加载成长数据…</section>

  <template v-else-if="growth">
    <section class="real-growth-metrics">
      <article class="card growth-metric"><span>完成记录（含专项再练）</span><strong>{{ countLabel(growth.completedCount) }}</strong><small>其中 {{ countLabel(growth.practiceCount) }} 次为专项再练</small></article>
      <article class="card growth-metric"><span>专项再练完成</span><strong>{{ countLabel(growth.practiceCount) }}</strong><small>单独统计，不混入普通同岗趋势</small></article>
      <article class="card growth-metric"><span>可见页面累计计时</span><strong>{{ recordedDurationLabel(growth.recordedDurationMs, growth.timedAnswerCount) }}</strong><small>{{ countLabel(growth.timedAnswerCount) }} 个回答有计时</small></article>
    </section>

    <section v-if="roleRows.length" class="real-growth-roles">
      <article v-for="role in roleRows" :key="role.roleId" class="card real-role-trend">
        <div class="section-heading"><div><h2>{{ roleLabel(role.roleId) }}</h2><p class="small muted">评分时间序列（0–10）</p></div><span class="badge">{{ role.points.length }} 个点</span></div>
        <div v-if="role.points.length" class="growth-points">
          <div v-for="point in role.points" :key="point.id || `${role.roleId}-${point.at}`" class="growth-point">
            <div class="growth-point-meta"><span>{{ dateLabel(point.at) }}</span><strong v-if="scoreValue(point.score) !== null">{{ scoreLabel(point.score) }}</strong><span v-else class="small muted">未评分</span></div>
            <div class="growth-bar" aria-hidden="true"><span :style="{ width: barWidth(point.score) }"></span></div>
          </div>
        </div>
        <div v-else class="empty role-empty"><p>该岗位暂无可展示的评分点。</p></div>
      </article>
    </section>
    <section v-else class="card empty real-state">
      <h2>暂无岗位评分时间序列</h2>
      <p>完成一场面试并保存评分后，这里会展示对应岗位的记录。</p>
      <RouterLink class="button primary" to="/interviews/real">开始面试</RouterLink>
    </section>
  </template>

  <section v-else-if="!loading" class="card empty real-state">
    <h2>暂无成长数据</h2>
    <RouterLink class="button secondary" to="/interviews/real">开始面试</RouterLink>
  </section>
</template>

<style scoped>
.real-page-heading{align-items:flex-start}.real-login-notice{margin-bottom:18px}.real-login-notice .text-link{padding:0}.real-state{margin-top:8px}.real-growth-metrics{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px;margin-bottom:18px}.growth-metric{display:flex;flex-direction:column;gap:8px}.growth-metric>span{font-size:12px;color:var(--muted)}.growth-metric strong{font-size:31px;line-height:1.25;color:#9b8338}.growth-metric small{font-size:11px;color:var(--muted)}.real-growth-roles{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px}.real-role-trend{min-width:0}.growth-points{display:flex;flex-direction:column;gap:16px}.growth-point-meta{display:flex;align-items:center;justify-content:space-between;gap:14px;font-size:12px;color:var(--muted)}.growth-point-meta strong{font-size:15px;color:#9b8338}.growth-bar{height:9px;border-radius:999px;background:#edf0e5;overflow:hidden;margin-top:7px}.growth-bar span{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#d4b258,#9c8135);min-width:0}.role-empty{padding:25px 10px}.role-empty p{margin:0}@media(max-width:900px){.real-growth-roles{grid-template-columns:1fr}}@media(max-width:767px){.real-page-heading{display:block}.real-growth-metrics{grid-template-columns:1fr;gap:12px}.growth-metric{padding:18px}.growth-metric strong{font-size:28px}.real-growth-roles{gap:14px}.real-role-trend{padding:20px 18px}}
</style>
