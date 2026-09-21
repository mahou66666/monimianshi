<template>
  <div class="summary-wrapper">
    <div class="summary-header">
      <button class="icon-btn" type="button" @click="$emit('close')">&#8249;</button>
      <span class="header-title">面试总结</span>
      <div class="header-placeholder"></div>
    </div>

    <div class="summary-content">
      <section class="card">
        <div class="card-title-row">
          <h2 class="card-title">能力象限分析</h2>
        </div>
        <div ref="radarRef" class="radar-chart"></div>
      </section>

      <section class="card">
        <div class="card-title-row">
          <h2 class="card-title">AI 深度评语</h2>
        </div>
        <div class="score-row">
          <div class="progress-ring-wrap">
            <svg class="progress-ring" viewBox="0 0 96 96">
              <circle cx="48" cy="48" r="40" class="ring-bg" />
              <circle
                cx="48"
                cy="48"
                r="40"
                class="ring-progress"
                :style="{ strokeDashoffset: ringDashOffset }"
              />
            </svg>
            <div class="score-center">
              <span class="score-main">{{ overallPercent }}</span>
              <span class="score-sub">综合得分</span>
            </div>
          </div>
          <p class="score-comment">{{ aiComment }}</p>
        </div>
      </section>

      <section class="card">
        <div class="card-title-row">
          <h2 class="card-title">逐题复盘</h2>
        </div>
        <div class="review-list">
          <article v-for="item in reviewItems" :key="item.title" class="review-item">
            <div class="review-head">
              <h3 class="review-title">{{ item.title }}</h3>
              <span class="review-score">{{ item.score.toFixed(1) }} 分</span>
            </div>
            <p class="review-text">
              <span class="review-label">建议：</span>{{ item.advice }}
            </p>
          </article>
        </div>
      </section>

      <section class="card">
        <div class="card-title-row">
          <h2 class="card-title">下一步计划</h2>
        </div>
        <ul class="plan-list">
          <li v-for="item in actionPlans" :key="item" class="plan-item">{{ item }}</li>
        </ul>
      </section>
    </div>

    <div class="bottom-actions">
      <button class="home-btn" type="button" @click="$emit('close')">首页</button>
      <button class="retry-btn" type="button" @click="$emit('restart-interview')">再次对练</button>
    </div>
  </div>
</template>

<script setup>
import * as echarts from 'echarts';
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useInterviewStore } from '@/stores/interview';

defineEmits(['close', 'restart-interview']);

const interviewStore = useInterviewStore();
const radarRef = ref(null);
let chartInstance = null;
let chartResizeObserver = null;

const summary = computed(() => interviewStore.getSummary() || {});
const scoreSummary = computed(() => summary.value.scoreSummary || {});
const RADAR_FALLBACK_LABELS = ['技术基础', '逻辑思维', '沟通表达', '抗压能力', '团队协作'];

const competencyList = computed(() => {
  const raw = scoreSummary.value.competencies;
  if (!Array.isArray(raw) || raw.length === 0) {
    return [
      { competency: '技术基础', avg_score: 8.5, evidence_samples: ['补充底层原理细节与边界条件说明。'] },
      { competency: '逻辑思维', avg_score: 7.8, evidence_samples: ['回答时可先给结论，再拆解推理路径。'] },
      { competency: '沟通表达', avg_score: 9.0, evidence_samples: ['继续保持结构化表达，强化数据支撑。'] },
      { competency: '抗压能力', avg_score: 7.0, evidence_samples: ['高压追问场景下提升方案对比与取舍说明。'] },
      { competency: '团队协作', avg_score: 8.8, evidence_samples: ['增加跨团队协同案例与冲突处理细节。'] },
    ];
  }

  return raw;
});

const overallScore10 = computed(() => {
  const raw = Number(scoreSummary.value.overall_score ?? scoreSummary.value.overallScore ?? 8.2);
  if (Number.isNaN(raw)) {
    return 8.2;
  }

  const normalized = raw > 10 ? raw / 10 : raw;
  return Math.max(0, Math.min(10, normalized));
});

const overallPercent = computed(() => Math.round(overallScore10.value * 10));
const ringLength = 251.2;
const ringDashOffset = computed(() => `${ringLength - (ringLength * overallPercent.value) / 100}`);

const aiComment = computed(() => {
  const finalReport = String(summary.value.finalReport || '').trim();
  if (finalReport) {
    return finalReport.split('\n').filter(Boolean).slice(0, 2).join(' ');
  }

  const reply = String(summary.value.reply || '').trim();
  if (reply) {
    return reply;
  }

  return '整体表现良好，建议持续加强高压场景下的结构化表达与复杂问题拆解能力。';
});

const reviewItems = computed(() =>
  competencyList.value.slice(0, 4).map((item) => ({
    title: item.competency || '综合能力',
    score: Number(item.avg_score ?? 0) || 0,
    advice:
      (Array.isArray(item.evidence_samples) && item.evidence_samples[0]) ||
      '建议继续补充可量化的结果描述，提升回答说服力。',
  })),
);

const actionPlans = computed(() => {
  const risks = scoreSummary.value.risks;
  if (Array.isArray(risks) && risks.length > 0) {
    return risks.slice(0, 3).map((risk) => `围绕「${risk}」进行专项练习并准备实战案例。`);
  }

  return [
    '复习 IoC、AOP 等核心知识，补齐底层原理与边界场景。',
    '准备 2 个可量化成果的项目故事，形成稳定回答模板。',
    '针对高并发与系统设计问题进行限时复盘训练。',
  ];
});

const radarIndicators = computed(() => {
  const names = competencyList.value
    .slice(0, 5)
    .map((item) => String(item.competency || '').trim())
    .filter(Boolean);

  for (const fallbackName of RADAR_FALLBACK_LABELS) {
    if (names.length >= 5) {
      break;
    }
    if (!names.includes(fallbackName)) {
      names.push(fallbackName);
    }
  }

  while (names.length < 5) {
    names.push(`能力维度${names.length + 1}`);
  }

  return names.slice(0, 5).map((name) => ({
    name,
    max: 100,
  }));
});

const radarValues = computed(() => {
  const values = competencyList.value.slice(0, 5).map((item) => {
    const raw = Number(item.avg_score ?? 0) * 10;
    return Math.max(0, Math.min(100, Number.isNaN(raw) ? 0 : raw));
  });

  const fallbackValue = Math.max(60, overallPercent.value || 0);
  while (values.length < 5) {
    values.push(fallbackValue);
  }

  return values.slice(0, 5);
});

const renderRadar = () => {
  if (!radarRef.value) {
    return;
  }

  if (!chartInstance) {
    chartInstance = echarts.init(radarRef.value);
  }

  chartInstance.setOption({
    radar: {
      indicator: radarIndicators.value,
      shape: 'polygon',
      radius: '62%',
      center: ['50%', '55%'],
      axisName: {
        color: '#888888',
        fontSize: 12,
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(255,255,255,0.02)', 'rgba(255,255,255,0.05)'],
        },
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(255,255,255,0.1)',
        },
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(255,255,255,0.1)',
        },
      },
    },
    series: [
      {
        type: 'radar',
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: {
          color: '#e2cd6d',
        },
        areaStyle: {
          color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
            { offset: 0, color: 'rgba(226,205,109,0.1)' },
            { offset: 1, color: 'rgba(226,205,109,0.4)' },
          ]),
        },
        lineStyle: {
          width: 2,
          color: '#e2cd6d',
        },
        data: [{ value: radarValues.value }],
      },
    ],
  });
};

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize();
  }
};

onMounted(() => {
  renderRadar();
  chartResizeObserver = new ResizeObserver(handleResize);
  if (radarRef.value) chartResizeObserver.observe(radarRef.value);
  window.addEventListener('resize', handleResize);
});

watch([radarIndicators, radarValues], () => {
  renderRadar();
});

onBeforeUnmount(() => {
  chartResizeObserver?.disconnect();
  window.removeEventListener('resize', handleResize);
  if (chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
});
</script>

<style scoped>
.summary-wrapper {
  width: 100%;
  max-width: 1200px;
  margin-inline: auto;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #111111;
  color: #ffffff;
  position: relative;
}

.summary-header {
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: rgba(17, 17, 17, 0.9);
  backdrop-filter: blur(10px);
}

.icon-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  color: #ffffff;
  background: #222222;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.header-placeholder {
  width: 40px;
}

.summary-content {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 8px 20px 120px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-content::-webkit-scrollbar {
  display: none;
}

.card {
  min-width: 0;
  flex-shrink: 0;
  overflow-wrap: anywhere;
  background: #222222;
  border-radius: 28px;
  padding: 20px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.radar-chart {
  width: 100%;
  height: 260px;
}

.score-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.progress-ring-wrap {
  width: 96px;
  height: 96px;
  position: relative;
  flex-shrink: 0;
}

.progress-ring {
  width: 96px;
  height: 96px;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: transparent;
  stroke: #333333;
  stroke-width: 6;
}

.ring-progress {
  fill: transparent;
  stroke: #e2cd6d;
  stroke-width: 6;
  stroke-linecap: round;
  stroke-dasharray: 251.2;
  transition: stroke-dashoffset 0.8s ease;
}

.score-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score-main {
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
}

.score-sub {
  margin-top: 4px;
  font-size: 10px;
  color: #e2cd6d;
}

.score-comment {
  margin: 0;
  color: #888888;
  font-size: 13px;
  line-height: 1.6;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.review-item {
  background: #1a1a1a;
  border-radius: 16px;
  padding: 14px;
  border-left: 3px solid #e2cd6d;
}

.review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.review-title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
}

.review-score {
  font-size: 12px;
  color: #67c23a;
  font-weight: 700;
}

.review-text {
  margin: 0;
  color: #888888;
  font-size: 12px;
  line-height: 1.6;
}

.review-label {
  color: #e2cd6d;
  font-weight: 700;
}

.plan-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-item {
  color: #bbbbbb;
  font-size: 14px;
  line-height: 1.5;
  position: relative;
  padding-left: 20px;
}

.plan-item::before {
  content: '●';
  color: #e2cd6d;
  position: absolute;
  left: 0;
  top: 0;
}

.bottom-actions {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 10px;
  padding: 18px 20px 22px;
  background: linear-gradient(to top, #111111 65%, rgba(17, 17, 17, 0));
}

.home-btn {
  width: 72px;
  border: none;
  border-radius: 16px;
  background: #222222;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.retry-btn {
  flex: 1;
  border: none;
  border-radius: 16px;
  background: #e2cd6d;
  color: #111111;
  font-size: 17px;
  font-weight: 700;
  height: 54px;
  cursor: pointer;
}
.score-row { flex-wrap: wrap; }
.progress-ring-wrap { flex-shrink: 0; }
.review-head { flex-wrap: wrap; gap: 8px; }

@media (min-width: 1200px) {
  .summary-content {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-content: start;
    align-items: start;
  }
}
</style>
