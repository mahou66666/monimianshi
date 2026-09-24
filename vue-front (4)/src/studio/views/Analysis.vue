<script setup>
import { computed,onMounted,onBeforeUnmount } from 'vue';
import { useRoute,useRouter } from 'vue-router';
import { studio } from '../store.js';
const route=useRoute(),router=useRouter();
const s=computed(()=>studio.state.value.sessions.find(x=>x.id===route.params.sessionId));
let timer;
function run(){clearInterval(timer);timer=setInterval(()=>{if(s.value?.analysis?.status==='failed'){clearInterval(timer);return}if(!s.value||s.value.status!=='completed')return;const next=studio.call('advanceAnalysis',s.value.id);if(next.analysis.status==='completed'){clearInterval(timer);router.replace(`/interviews/${s.value.id}/report`)}},1200)}
function retry(){studio.call('retryAnalysis',s.value.id);run()}
onMounted(()=>{if(s.value?.status==='completed'){if(s.value.analysis?.status==='completed')router.replace(`/interviews/${s.value.id}/report`);else run()}});onBeforeUnmount(()=>clearInterval(timer));
</script>
<template><section v-if="s?.status==='completed'" class="card analysis-page"><img class="fairy-mascot" src="/images/interview-fairy-transparent.png" alt=""/><span class="badge">MOCK 分析流程</span><h1>{{s.analysis?.status==='failed'?'进度已保留，随时可以重试。':'正在整理这次练习的收获。'}}</h1><p class="muted">{{s.roleName}} · {{s.answers.length}} 道回答已保存</p><ol class="analysis-steps"><li v-for="(label,i) in ['内容分析：整理四维评价与回答依据','表达分析：关联示例音频或标记未评估','综合报告：汇总亮点、不足与提升建议']" :key="label" :class="{done:(s.analysis?.step||0)>i}"><span>{{(s.analysis?.step||0)>i?'✓':i+1}}</span>{{label}}</li></ol><p class="notice">演示流程使用本地规则和预设数据，不调用真实大模型或声学分析服务。</p><p v-if="s.analysis?.error" class="error" role="alert">{{s.analysis.error}}</p><div class="action-bar"><button v-if="s.analysis?.status==='failed'" class="button primary" @click="retry">重试分析</button><button v-else class="text-link" @click="studio.call('failAnalysis',s.id)">模拟分析中断</button><RouterLink :to="`/interviews/${s.id}/report`" class="button secondary">查看已有内容报告</RouterLink></div></section><section v-else class="card empty"><h1>请先完成面试</h1><RouterLink to="/interviews">返回面试记录</RouterLink></section></template>
