<script setup>
import { computed, ref } from 'vue';
const props=defineProps({score:Number,competencies:{type:Array,default:()=>[]},turns:{type:Array,default:()=>[]},completed:Boolean,retryTarget:[Object,String]});
const emit=defineEmits(['review']);
const scoringDialog=ref(null);
const numeric=value=>value!==null&&value!==undefined&&value!==''&&Number.isFinite(Number(value))&&Number(value)>=0&&Number(value)<=10?Number(value):null;
function shortName(name){const names={'团队协作与沟通推动':'协作沟通','机器学习与实验设计':'学习与实验','故障排查与实验设计':'排查与实验','系统设计与工程实现':'设计与实现','沟通协作与执行力':'沟通与执行','系统监控与故障排查':'监控与排查'};return names[name]||name;}
const items=computed(()=>props.competencies.map((c,index)=>({...c,index,name:String(c.competency||'能力项'),score:numeric(c.avg_score)})).filter(c=>c.score!==null));
const ranked=computed(()=>[...items.value].sort((a,b)=>b.score-a.score));
const strengths=computed(()=>ranked.value.filter(c=>c.score>=7).slice(0,2));
const improve=computed(()=>[...ranked.value].reverse().filter(c=>c.score<7).slice(0,2));
const focus=computed(()=>improve.value[0]||ranked.value.at(-1));
const conclusion=computed(()=>props.score===null?'等待评分':!props.completed?'阶段性表现':props.score>=8.5?'表现出色':props.score>=7?'表现良好':props.score>=6?'稳步提升':'建议加强练习');
// Use only observed abilities; never pad missing axes with invented values.
const axes=computed(()=>items.value.slice(0,6));
function point(i,radius){const angle=-Math.PI/2+i*Math.PI*2/axes.value.length;return [180+Math.cos(angle)*radius,160+Math.sin(angle)*radius];}
function polygon(ratio){return axes.value.map((c,i)=>point(i,100*(ratio??c.score/10)).join(',')).join(' ');}
function related(item){return props.turns.find(t=>t.status==='confirmed'&&Number(t.roundNo)!==0&&t.score?.competency===item.name);}
function evidence(item){return Array.isArray(item.evidence_samples)?item.evidence_samples.filter(x=>typeof x==='string'&&x.trim()).join('；'):'';}
</script>
<template>
  <div class="report-tools"><span class="small muted">基于本次已记录回答生成</span><button class="text-link" type="button" @click="scoringDialog.showModal()">评分说明 ⓘ</button></div>
  <dialog ref="scoringDialog" class="scoring-modal" @click.self="scoringDialog.close()">
    <section class="card scoring-dialog" aria-labelledby="scoring-title"><h2 id="scoring-title">评分说明</h2><p>内容分数来自本次问答的能力项评分与证据，统一采用 10 分制。能力画像只展示已有评分，不补齐缺失维度。</p><p>当前服务可能包含未单独标记的规则兜底评分。表达分析中的演示数据不计入总分。</p><p class="small muted">AI 评分用于训练反馈，不代表招聘结果。</p><button class="button primary" type="button" autofocus @click="scoringDialog.close()">知道了</button></section>
  </dialog>
  <div class="overview-grid">
    <section class="card overall-card"><p class="eyebrow">YOUR INTERVIEW REVIEW</p><h2>综合评分</h2><div class="score-ring" :style="{'--progress':`${(score??0)*10}%`}"><div><strong>{{score===null?'—':score.toFixed(1)}}</strong><span>/ 10</span></div></div><h3>{{conclusion}}</h3><p class="small muted">{{score===null?'继续完成面试，积累评分证据。':'从本次回答出发，找到下一次进步的方向。'}}</p></section>
    <section class="card radar-card"><div class="section-heading"><h2>能力画像</h2><span class="small muted">10 分制</span></div>
      <svg v-if="axes.length>=3" viewBox="0 0 360 320" class="ability-radar" role="img" :aria-label="axes.map(c=>`${c.name} ${c.score}分`).join('，')">
        <polygon v-for="level in [0.2,0.4,0.6,0.8,1]" :key="level" :points="polygon(level)" fill="#f8f8f2" stroke="#e0e4d8"/>
        <line v-for="(item,i) in axes" :key="`line-${i}`" x1="180" y1="160" :x2="point(i,100)[0]" :y2="point(i,100)[1]" stroke="#e0e4d8"/>
        <polygon :points="polygon()" fill="#e7c75e55" stroke="#b79738" stroke-width="2.5"/>
        <g v-for="(item,i) in axes" :key="i"><circle :cx="point(i,item.score*10)[0]" :cy="point(i,item.score*10)[1]" r="3.5" fill="#b79738"/><text :x="point(i,126)[0]" :y="point(i,126)[1]" text-anchor="middle" dominant-baseline="middle" fill="#58604f" font-size="11"><title>{{item.name}}</title>{{shortName(item.name).length>8?shortName(item.name).slice(0,7)+'…':shortName(item.name)}}</text></g>
      </svg>
      <div v-else class="radar-empty"><p>{{items.length?'已有能力评分不足三项，暂以条形展示。':'本次尚未形成能力画像。'}}</p><div v-for="item in items" :key="item.index"><span>{{shortName(item.name)}} · {{item.score}}/10</span><progress :value="item.score" max="10"/></div></div>
    </section>
  </div>
  <section class="card diagnosis"><div class="section-heading"><h2>本次诊断</h2><span class="small muted">优势 · 提升 · 下一步</span></div><div class="diagnosis-grid">
    <div class="positive"><h3>核心优势</h3><template v-if="strengths.length"><p v-for="item in strengths" :key="item.index"><strong>✓ {{shortName(item.name)}}</strong><span>{{item.score}} / 10</span></p></template><p v-else class="muted">继续积累回答证据，发现你的优势。</p></div>
    <div class="needs-work"><h3>{{improve.length?'需要提升':'下一步巩固'}}</h3><template v-if="improve.length"><p v-for="item in improve" :key="item.index"><strong>{{shortName(item.name)}}</strong><span>{{item.score}} / 10</span></p></template><p v-else-if="focus"><strong>{{shortName(focus.name)}}</strong><span>本次相对较低项 · {{focus.score}} / 10</span></p><p v-else class="muted">暂无足够评分依据。</p></div>
  </div><div class="next-step"><div><h3>下一步训练</h3><p class="small muted">{{focus?`围绕“${shortName(focus.name)}”复盘回答，补充方案依据、取舍与可验证的结果。`:'完成面试后，可从逐题复盘中选择专项练习。'}}</p></div><RouterLink v-if="retryTarget" class="button primary" :to="retryTarget">针对薄弱题再练 →</RouterLink><RouterLink class="button secondary" to="/interviews/real">重新模拟面试</RouterLink></div></section>
  <section class="evidence-section"><div class="section-heading"><h2>评分证据</h2><span class="small muted">评价有依据，复盘有方向</span></div><div class="evidence-grid"><article v-for="item in items" :key="item.index" class="card evidence-card"><div class="section-heading"><h3 :title="item.name">{{shortName(item.name)}}</h3><strong class="evidence-value">{{item.score}}<small>/10</small></strong></div><p v-if="shortName(item.name)!==item.name" class="small muted">{{item.name}}</p><span class="evidence-label">回答证据</span><p class="evidence-copy">{{evidence(item)||'该能力项暂未保存证据摘要。'}}</p><template v-if="related(item)?.feedback"><span class="evidence-label">本题反馈</span><p class="small">{{related(item).feedback}}</p></template><button v-if="related(item)" type="button" class="text-link" @click="emit('review',related(item).roundNo)">查看对应回答 →</button></article></div><p v-if="!items.length" class="card muted">尚未形成可展示的能力评分，请先查看下方问答记录。</p></section>
</template>
<style scoped>
.report-tools{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}.overview-grid{display:grid;grid-template-columns:minmax(240px,.8fr) minmax(0,1.2fr);gap:24px;margin-bottom:24px}.overall-card{text-align:center;display:flex;flex-direction:column;align-items:center;justify-content:center}.score-ring{width:190px;height:190px;border-radius:50%;background:conic-gradient(#d8b94e var(--progress),#edf0e5 0);padding:10px;margin:24px 0}.score-ring>div{height:100%;background:white;border-radius:50%;display:flex;flex-direction:column;justify-content:center}.score-ring strong{font-size:54px;line-height:1.2;color:#273029}.score-ring span{font-size:14px;color:var(--muted)}.ability-radar{width:100%;max-height:320px;display:block}.radar-empty{min-height:240px;padding:25px 0}.radar-empty progress{display:block;width:100%;accent-color:#d8b94e;margin:12px 0}.diagnosis{margin-bottom:28px}.diagnosis-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px}.diagnosis-grid>div{padding:20px;border-radius:12px}.positive{background:#f0f4eb}.needs-work{background:#fbf3e8}.diagnosis-grid p strong,.diagnosis-grid p span{display:block}.diagnosis-grid p span{font-size:12px;color:var(--muted)}.next-step{display:flex;align-items:center;gap:12px;flex-wrap:wrap;padding-top:22px}.next-step>div{flex:1;min-width:220px}.evidence-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.evidence-card{display:flex;flex-direction:column;min-width:0}.evidence-value{font-size:24px;color:#9b8035;white-space:nowrap}.evidence-value small{font-size:12px}.evidence-label{color:#8a937f;font-size:11px;margin-top:10px}.evidence-copy{font-size:14px;line-height:1.9;overflow-wrap:anywhere}.evidence-card button{align-self:flex-start;margin-top:auto}.scoring-modal{padding:0;border:0;border-radius:18px;width:calc(100% - 32px);max-width:500px;background:transparent}.scoring-modal::backdrop{background:#20291e66}.scoring-dialog{max-width:500px;max-height:85dvh;overflow:auto}.scoring-dialog p{margin:16px 0}.evidence-section{margin-bottom:24px}@media(max-width:767px){.overview-grid,.diagnosis-grid,.evidence-grid{grid-template-columns:1fr;gap:16px}.score-ring{width:160px;height:160px;margin:18px 0}.score-ring strong{font-size:46px}.next-step{align-items:stretch;flex-direction:column}.next-step>div{min-width:0}.ability-radar{max-height:300px}.diagnosis-grid>div{padding:16px}.report-tools>.small{font-size:11px}}
</style>
