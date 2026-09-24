<script setup>
import { computed } from 'vue';
const props=defineProps({sessionId:{type:String,required:true}});
const presets=[
  {rate:210,clarity:82,confidence:76,advice:[
    '语速练习：尝试在结论和关键技术点之后短暂停顿，避免连续堆叠信息；录音回听，检查听众是否容易跟上。',
    '清晰度练习：技术名词完整读出，长句拆成短句，采用“结论—原因—例子”组织回答。',
    '自信表达练习：开头先给出明确观点，再用项目事实支撑；不确定的地方说明边界，减少无意义的自我否定。'
  ]},
  {rate:160,clarity:60,confidence:65,advice:[
    '语速练习：不必刻意加快，先让句子衔接更连贯；回答前列出三个要点，减少句中反复组织措辞。',
    '清晰度练习：一次只解释一个观点，清楚说出关键词；检查麦克风距离与环境噪声，再回听是否容易理解。',
    '自信表达练习：先用熟悉的项目完成一分钟回答，练习“我负责什么—如何解决—得到什么结果”，逐步减少对逐字稿的依赖。'
  ]},
  {rate:195,clarity:90,confidence:85,advice:[
    '语速练习：保持自然节奏，在复杂原理和关键结论处适当放慢，给听众留出理解时间。',
    '清晰度练习：继续保持短句与清楚的层次，解释缩写和专业术语，避免默认听众了解项目背景。',
    '自信表达练习：增加追问练习，面对反例时先澄清条件，再解释方案取舍；保持有依据的表达，避免过度承诺。'
  ]}
];
const memory=new Map();
const preset=computed(()=>{
  const key=`interview-studio:expression-demo:v1:${props.sessionId}`;
  if(memory.has(key))return presets[memory.get(key)];
  let index;
  try{const stored=localStorage.getItem(key);if(stored!==null&&/^[0-2]$/.test(stored))index=Number(stored);}catch{}
  if(index===undefined){index=Math.floor(Math.random()*presets.length);try{localStorage.setItem(key,String(index));}catch{}}
  memory.set(key,index);return presets[index];
});
</script>
<template>
  <section class="card expression-demo">
    <div class="section-heading"><h2>表达分析</h2></div>
    <p class="small muted">演示数据与对应练习建议，不参与面试总分和成长统计。</p>
    <div class="expression-grid">
      <div><span>语速</span><strong>{{preset.rate}}<small>字/分钟</small></strong></div>
      <div><span>清晰度</span><strong>{{preset.clarity}}<small>/100</small></strong></div>
      <div><span>自信度</span><strong>{{preset.confidence}}<small>/100</small></strong></div>
    </div>
    <h3>改进建议</h3><ul><li v-for="advice in preset.advice" :key="advice">{{advice}}</li></ul>
  </section>
</template>
<style scoped>
.expression-demo{margin:20px 0}.expression-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:16px;margin:18px 0 22px}.expression-grid>div{background:#f5f6ee;border-radius:12px;padding:16px}.expression-grid span{display:block;color:var(--muted);font-size:13px}.expression-grid strong{display:block;color:#9b8035;font-size:30px;margin-top:8px}.expression-grid small{font-size:12px;margin-left:6px;font-weight:400}.expression-demo ul{padding-left:20px;margin-bottom:0}.expression-demo li{margin:10px 0;line-height:1.8}@media(max-width:767px){.expression-grid{grid-template-columns:1fr;gap:10px}.expression-grid>div{display:flex;align-items:center;justify-content:space-between}.expression-grid strong{margin:0;font-size:25px}}
</style>
