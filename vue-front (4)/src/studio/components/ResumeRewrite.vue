<script setup>
import { computed, ref } from 'vue';
import { realRequest } from '../services/real.js';
const props=defineProps({resume:{type:Object,required:true},role:{type:String,default:'java'}});
const emit=defineEmits(['busy','saved']);
const expanded=ref(false),roleId=ref(props.role||'java'),jd=ref(''),working=ref(''),error=ref(''),drafts=ref({}),created=ref(null);
const accepted=computed(()=>Object.values(drafts.value).filter(d=>d.accepted).length);
async function run(key,fn){working.value=key;error.value='';emit('busy',true);try{await fn();}catch(e){error.value=e.message;}finally{working.value='';emit('busy',false);}}
async function generate(fragment){await run(fragment.id,async()=>{
  const result=await realRequest(`/resumes/${props.resume.id}/rewrite`,{method:'POST',timeout:195000,body:{revision:props.resume.revision,fragmentId:fragment.id,roleId:roleId.value,jd:jd.value}});
  drafts.value[fragment.id]={text:result.text,suggestion:result.suggestion,accepted:false};
});}
async function save(){await run('save',async()=>{
  const fragments=props.resume.fragments.map(f=>({...f,text:drafts.value[f.id]?.accepted?drafts.value[f.id].text:f.text}));
  if(fragments.some(f=>!f.text.trim()||f.text.length>10000))throw Error('每段内容需为 1–10000 字，请检查已采纳内容');
  if(!created.value)created.value=await realRequest(`/resumes/${props.resume.id}/versions`,{method:'POST'});
  const result=await realRequest(`/resumes/${created.value.id}`,{method:'PUT',body:{revision:created.value.revision,fragments}});
  emit('saved',result);
});}
</script>
<template>
  <section class="rewrite">
    <button type="button" class="button secondary" :disabled="!!working" :aria-expanded="expanded" @click="expanded=!expanded">{{expanded?'收起简历优化':'AI 优化简历'}}</button>
    <div v-if="expanded" class="rewrite-workspace">
      <h3>让经历表达更清晰</h3><p class="small muted">逐段生成建议，核对事实后采纳。保存为新版本，原简历保留。</p>
      <div class="rewrite-settings"><label>目标岗位<select v-model="roleId" :disabled="!!working"><option value="java">Java 后端工程师</option><option value="web">Web 前端工程师</option><option value="algorithm">算法工程师</option><option value="testing">测试工程师</option></select></label><label>职位描述（可选）<textarea v-model="jd" rows="3" maxlength="10000" :disabled="!!working" placeholder="粘贴目标岗位要求，让建议更有针对性"/></label></div>
      <p v-if="error" class="error" role="alert">{{error}}</p>
      <p v-if="created" class="notice">修订版本已创建。若保存未完成，可再次点击保存；也可在简历列表中打开该版本。</p>
      <article v-for="(fragment,index) in resume.fragments" :key="fragment.id" class="rewrite-fragment">
        <div class="section-heading"><h3>{{fragment.section}} · {{index+1}}</h3><button type="button" class="text-link" :disabled="!!working" @click="generate(fragment)">{{working===fragment.id?'正在生成…':drafts[fragment.id]?'重新生成':'生成建议'}}</button></div>
        <div class="rewrite-compare"><div><h4>原文</h4><p class="original">{{fragment.text}}</p></div><div><h4>改写建议</h4><template v-if="drafts[fragment.id]"><textarea v-model="drafts[fragment.id].text" :aria-label="`编辑第 ${index+1} 段改写建议`" rows="8" maxlength="10000" :disabled="!!working"/><p class="small muted">{{drafts[fragment.id].suggestion}}</p><label class="check-label"><input v-model="drafts[fragment.id].accepted" type="checkbox" :disabled="!!working"/>采纳本段修改</label></template><p v-else class="small muted" role="status">{{working===fragment.id?'正在结合岗位生成，请稍候…':'点击“生成建议”，查看并编辑本段改写。'}}</p></div></div>
      </article>
      <div class="rewrite-save"><span class="small muted">已采纳 {{accepted}} 段 · 未采纳的片段保持原文</span><button class="button primary" :disabled="!!working||!accepted" @click="save">{{working==='save'?'正在保存…':'保存为新版本'}}</button></div>
    </div>
  </section>
</template>
<style scoped>
.rewrite{margin-top:20px}.rewrite-workspace{margin-top:20px;border-top:1px solid var(--line);padding-top:20px}.rewrite-settings label{margin:16px 0}.rewrite-fragment{padding:20px 0;border-bottom:1px solid var(--line)}.rewrite-compare{display:grid;grid-template-columns:1fr 1fr;gap:20px}.rewrite-compare>div{min-width:0}.original{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.8;background:#f7f8f2;padding:14px;border-radius:10px}.rewrite-compare textarea{line-height:1.8}.rewrite-save{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-top:20px}.check-label{display:flex;align-items:center;gap:8px}.check-label input{width:auto}@media(max-width:767px){.rewrite-compare{grid-template-columns:1fr}.rewrite-save{align-items:stretch;flex-direction:column}.section-heading{flex-wrap:wrap}}
</style>
