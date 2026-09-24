<script setup>
import {ref,computed,onMounted,onUnmounted,watch} from 'vue';
import {useRoute,useRouter} from 'vue-router';
import {realRequest} from '../services/real.js';
import AsrInput from '../components/AsrInput.vue';
import InterviewerQuestion from '../components/InterviewerQuestion.vue';
import {interviewerVoiceEnabled,setInterviewerVoice} from '../services/interviewerVoice.js';
const voiceBusy=ref(false),inputMode=ref('text');
const speechMetrics=ref(null);
function mergeSpeechMetrics(current,next){if(!next||typeof next!=='object')return current;const base=current||{source:'audio',vadMethod:next.vadMethod||'pcm-energy-vad',audioDurationMs:0,speechDurationMs:0,speechSegments:[],pauseCount:0,pauseDurationMs:0,longestPauseMs:0,transcriptCharacters:0,fillerCount:0,repetitionCount:0};const merged={...base};for(const key of ['audioDurationMs','speechDurationMs','pauseCount','pauseDurationMs','transcriptCharacters','fillerCount','repetitionCount'])merged[key]=Number(base[key]||0)+Number(next[key]||0);merged.longestPauseMs=Math.max(Number(base.longestPauseMs||0),Number(next.longestPauseMs||0));const offset=Number(base.audioDurationMs||0);merged.speechSegments=[...(base.speechSegments||[]),...(next.speechSegments||[]).map(segment=>({...segment,startMs:Number(segment.startMs||0)+offset,endMs:Number(segment.endMs||0)+offset}))];merged.speechRateCharsPerMin=merged.speechDurationMs?Math.round(merged.transcriptCharacters*60000/merged.speechDurationMs*10)/10:0;merged.overallRateCharsPerMin=merged.audioDurationMs?Math.round(merged.transcriptCharacters*60000/merged.audioDurationMs*10)/10:0;merged.speechCoverageRatio=merged.audioDurationMs?Math.round(merged.speechDurationMs/merged.audioDurationMs*1000)/1000:0;return merged;}
function appendTranscript(payload){const text=typeof payload==='string'?payload:payload?.text||'';const combined=[answer.value.trim(),text].filter(Boolean).join('\n');if(combined.length>12000){error.value='加入转写后超过 12000 字，请先精简现有回答。';return;}answer.value=combined;inputMode.value='text';speechMetrics.value=mergeSpeechMetrics(speechMetrics.value,payload?.metrics);}
const route=useRoute(),router=useRouter();
const roles={java:'Java 后端工程师',web:'Web 前端工程师',algorithm:'算法工程师',testing:'测试工程师'};
const roleId=ref(roles[route.query.role]?String(route.query.role):'java');
const sourceSessionId=String(route.query.sourceSessionId||''),sourceRound=Number(route.query.sourceRound);
const practice=ref(null),practiceTurn=ref(null);
const resumes=ref([]),history=ref([]),session=ref(null),resumeId=ref(String(route.query.resumeId||''));
const industry=ref('互联网'),jd=ref(''),maxQuestions=ref(3),answer=ref(''),consent=ref(false),busy=ref(false),error=ref(''),signedIn=ref(true);
const selectedResume=computed(()=>resumes.value.find(r=>r.id===resumeId.value));
const stageName=computed(()=>result.value.last_interviewer==='intro'?'自我介绍':result.value.last_interviewer==='hr'?'行为面试':'技术面试');
const result=computed(()=>session.value?.result||{}),phase={busy:'正在处理',uncertain:'等待核对',ready:'面试中',completed:'已完成'};
const loginPath=computed(()=>({path:'/login',query:{returnTo:route.fullPath}}));
let requestId=crypto.randomUUID(),timer,closed=false,elapsed=0,visibleStart=null,clockRound=null;
function updateClock(){
  const now=performance.now();if(visibleStart!==null)elapsed+=now-visibleStart;visibleStart=null;
  const key=session.value?.id+':'+result.value.question_count;
  if(clockRound!==key){elapsed=0;clockRound=key;}
  if(!closed&&!busy.value&&session.value?.status==='ready'&&document.visibilityState==='visible')visibleStart=now;
}
function duration(){updateClock();return Math.min(7200000,Math.max(0,Math.round(elapsed)));}
watch(()=>[busy.value,session.value?.status,result.value.question_count],updateClock,{flush:'sync'});
const api=(path,options={})=>realRequest('/interviews'+path,{timeout:260000,...options});
async function perform(fn){if(busy.value)return;busy.value=true;error.value='';try{await fn();}catch(e){error.value=e.message;if(e.status===401){signedIn.value=false;session.value=null;resumes.value=[];history.value=[];practice.value=null;practiceTurn.value=null;answer.value='';}}finally{busy.value=false;}}
async function refresh(){session.value=await api('/'+session.value.id);schedule();}
function schedule(){clearTimeout(timer);if(!closed&&session.value?.status==='busy')timer=setTimeout(()=>perform(refresh),3000);}
async function list(){history.value=await api('');}
async function start(){await perform(async()=>{try{session.value=await api('',{method:'POST',body:{requestId,resumeId:resumeId.value,industry:industry.value,jd:jd.value,maxQuestions:Number(maxQuestions.value),roleId:roleId.value,sourceSessionId:sourceSessionId||null,sourceRound:sourceSessionId?sourceRound:null}});answer.value='';speechMetrics.value=null;await router.replace({path:'/interviews/real',query:{id:session.value.id}});}finally{await list();}});}
async function submit(){if(voiceBusy.value)return;const durationMs=duration();await perform(async()=>{try{session.value=await api('/'+session.value.id+'/answers',{method:'POST',body:{expectedQuestion:result.value.question_count,answer:answer.value,durationMs,speechMetrics:speechMetrics.value}});answer.value='';speechMetrics.value=null;await list();schedule();}catch(e){try{session.value=await api('/'+session.value.id);schedule();}catch{}throw e;}});}
onMounted(()=>perform(async()=>{
  document.addEventListener('visibilitychange',updateClock);
  const me=await realRequest('/me');if(!roles[route.query.role]&&roles[me.roleId])roleId.value=me.roleId;
  resumes.value=(await realRequest('/resumes')).filter(r=>r.status==='confirmed');if(!resumeId.value)resumeId.value=resumes.value[0]?.id||'';
  await list();
  if(sourceSessionId){
    practice.value=await api('/'+sourceSessionId+'/report');
    practiceTurn.value=practice.value.turns.find(t=>t.roundNo===sourceRound&&t.status==='confirmed'&&t.score);
    if(practice.value.status!=='completed'||!practiceTurn.value)throw Error('该报告没有可用于再练的完整题目，请返回报告重新选择。');
    resumeId.value=practice.value.resumeId;roleId.value=practice.value.roleId;
  }
  if(route.query.id){session.value=await api('/'+route.query.id);schedule();}
}));
onUnmounted(()=>{closed=true;clearTimeout(timer);updateClock();document.removeEventListener('visibilitychange',updateClock);});
</script>
<template>
  <div class="interview-page" :class="{'in-session':session}">
    <header class="page-heading interview-heading">
      <div><p class="eyebrow">{{session?'专注表达，按自己的节奏':'准备下一次对话'}}</p><h1>{{session?'面试进行室':'准备这场面试'}}</h1><p class="muted">{{session?'结合真实经历，讲清思路、行动与结果。':'带上你的简历，让问题更贴近目标岗位。'}}</p></div>
      <RouterLink class="button secondary" to="/interviews/real/history">{{session?'返回面试记录':'历史记录'}}</RouterLink>
    </header>
    <p v-if="error" class="error" role="alert">{{error}}</p>
    <p v-if="!signedIn" class="notice">请先 <RouterLink :to="loginPath">登录账户</RouterLink>，再继续面试。</p>
    <div v-else-if="!session" class="preparation-grid">
      <section class="card preparation-form">
        <h2>你的面试方向</h2><p class="muted">已确认的简历与岗位要求将用于生成个性化问题。</p>
        <form id="interview-setup" @submit.prevent="start">
          <div v-if="sourceSessionId" class="notice"><h3>针对上一题专项再练</h3><template v-if="practiceTurn"><p class="wb-text">{{practiceTurn.question}}</p><p>{{practiceTurn.score.competency}} · 上次 {{practiceTurn.score.score}} / 10</p><p class="wb-text">{{practiceTurn.score.evidence}}</p><p>本次将带上原题、回答和反馈，继续练习薄弱项。</p></template><p v-else>正在读取原题…</p></div>
          <div class="setup-fields">
            <label v-if="!sourceSessionId">目标岗位<select v-model="roleId" :disabled="busy"><option v-for="(name,id) in roles" :key="id" :value="id">{{name}}</option></select></label>
            <label>使用简历<select v-model="resumeId" required :disabled="busy||!!sourceSessionId"><option value="" disabled>请选择已确认简历</option><option v-for="r in resumes" :key="r.id" :value="r.id">{{r.filename}}</option></select></label>
            <label v-if="!sourceSessionId">目标行业<input v-model="industry" maxlength="80" required :disabled="busy"/></label>
            <label>正式题数<input v-model="maxQuestions" type="number" min="1" max="20" required :disabled="busy"/><small class="muted">不含开场自我介绍</small></label>
          </div>
          <p v-if="!resumes.length&&!busy" class="notice">还没有已确认的简历。<RouterLink to="/resumes/real" class="text-link">上传并确认简历 →</RouterLink></p>
          <label v-if="!sourceSessionId">目标岗位与 JD<textarea v-model="jd" rows="5" maxlength="12000" required :disabled="busy" placeholder="粘贴岗位职责与任职要求，让提问更有针对性。"/></label>
          <label class="check-label consent-note"><input type="checkbox" v-model="consent" :disabled="busy" required/>同意将本次简历、岗位要求及回答交给面试服务和已配置的模型处理。</label>
        </form>
      </section>
      <aside class="card preparation-summary">
        <p class="eyebrow">准备就绪，从这里开始</p><h2>本次面试</h2>
        <dl class="session-facts"><div><dt>目标岗位</dt><dd>{{roles[roleId]}}</dd></div><div><dt>使用简历</dt><dd>{{selectedResume?.filename||'尚未选择'}}</dd></div><div v-if="!sourceSessionId"><dt>目标行业</dt><dd>{{industry||'尚未填写'}}</dd></div><div><dt>问题安排</dt><dd>自我介绍 + {{maxQuestions||'—'}} 道正式题</dd></div><div><dt>回答方式</dt><dd>文字 / 语音，可随时切换</dd></div></dl>
        <button form="interview-setup" type="submit" class="button primary wide" :disabled="busy||!resumeId||!consent||(!!sourceSessionId&&!practiceTurn)">{{busy?'正在连接面试官…':'开始面试'}}</button>
        <label class="check-label"><input type="checkbox" :checked="interviewerVoiceEnabled" @change="setInterviewerVoice($event.target.checked)"/>面试官朗读问题</label><p class="small muted">可随时切换无声面试。使用浏览器语音，音色因设备而异。</p><p class="small muted">提交后的回答与评分会保存到你的账户。</p>
      </aside>
    </div>
    <div v-else class="session-grid">
      <section class="session-conversation">
        <div class="session-toolbar"><span class="status-badge" :class="'status-'+session.status">{{phase[session.status]}}</span><span>{{stageName}} · 正式题 {{result.question_count||0}} / {{result.max_questions||'—'}}</span><button class="text-link" :disabled="busy||voiceBusy" @click="perform(refresh)">核对进度</button></div>
        <p v-if="session.status==='busy'||busy" class="notice" role="status">面试官正在思考，请稍候。已提交的回答会保留。</p>
        <p v-if="session.status==='uncertain'" class="notice" role="status">上次提交的结果尚待核对。请点击“核对进度”，确认前暂不能重复提交。</p>
        <template v-if="!result.interview_completed">
          <InterviewerQuestion :text="result.reply||''" :stage="stageName" :question-key="`${session.id}:${result.question_count}`" :busy="busy||session.status!=='ready'" :recording="voiceBusy"/>
          <section class="card answer-panel">
            <div class="answer-mode" role="group" aria-label="回答方式"><button type="button" :aria-pressed="inputMode==='text'" :class="{active:inputMode==='text'}" :disabled="voiceBusy" @click="inputMode='text'">文字回答</button><button type="button" :aria-pressed="inputMode==='voice'" :class="{active:inputMode==='voice'}" :disabled="voiceBusy" @click="inputMode='voice'">语音回答</button></div>
            <AsrInput v-show="inputMode==='voice'" :key="`${session.id}:${result.question_count}`" :session-id="session.id" :question="result.question_count" :remaining="12000-answer.trim().length-(answer.trim()?1:0)" :disabled="busy||session.status!=='ready'" @busy="voiceBusy=$event" @text="appendTranscript"/>
            <form id="interview-answer" @submit.prevent="submit"><label v-show="inputMode==='text'">你的回答<textarea v-model="answer" rows="7" maxlength="12000" required :disabled="busy||session.status!=='ready'" placeholder="先说结论，再结合经历说明行动、取舍和结果。"/></label>
              <div class="answer-actions"><span class="small muted">{{answer.length}} / 12000 · {{inputMode==='voice'?'转写确认后加入回答':'切换方式会保留当前文字'}}</span><button class="button primary" :disabled="busy||voiceBusy||session.status!=='ready'||!answer.trim()||inputMode==='voice'">{{busy?'正在提交…':'提交回答'}}</button></div>
            </form>
          </section>
          <details v-if="result.round_feedback" class="card feedback-details"><summary>查看上一轮反馈</summary><p class="wb-text">{{result.round_feedback}}</p></details>
        </template>
        <section v-else class="card completion-panel"><span class="status-badge status-completed">面试已完成</span><h2>每一次认真回答，都值得复盘。</h2><p class="muted">查看逐题评分、回答证据与改进建议，再选择薄弱项继续练习。</p><RouterLink class="button primary" :to="`/interviews/real/${session.id}/report`">查看报告与逐题复盘</RouterLink></section>
      </section>
      <aside class="card session-aside"><h2>本次进度</h2><dl class="session-facts"><div><dt>当前阶段</dt><dd>{{result.interview_completed?'已完成':stageName}}</dd></div><div><dt>正式题进度</dt><dd>{{result.question_count||0}} / {{result.max_questions||'—'}}</dd></div><div><dt>简历版本</dt><dd>{{session.resumeId.slice(0,8)}}</dd></div></dl><p class="small muted">自我介绍不计入正式题数，追问与阶段以面试官实际安排为准。</p><hr/><h3>表达小提示</h3><p class="muted">先给出判断，再解释依据。用具体行动和结果支持你的回答。</p><details><summary>评分与语音说明</summary><p class="small muted">评分沿用现有面试服务；异常兜底分数尚无逐条来源标记，仅供练习参考。语音回答确认提交后保存语速与停顿证据，纯文字回答不生成语音指标。</p></details></aside>
    </div>
  </div>
</template>
<style scoped>
.interview-page{width:100%}.in-session{max-width:1280px;margin:auto;padding:32px 40px 56px}.preparation-grid{display:grid;grid-template-columns:minmax(0,1fr) 320px;gap:24px;align-items:start}.setup-fields{display:grid;grid-template-columns:1fr 1fr;gap:0 20px}.preparation-summary{position:sticky;top:24px}.session-facts{margin:20px 0}.session-facts>div{padding:14px 0;border-bottom:1px solid var(--line)}.session-facts dt{color:var(--muted);font-size:12px;margin-bottom:5px}.session-facts dd{margin:0;font-weight:600;overflow-wrap:anywhere}.consent-note{font-size:12px;line-height:1.8}.session-grid{display:grid;grid-template-columns:minmax(0,1fr) 270px;gap:24px;align-items:start}.session-conversation{min-width:0}.session-toolbar{display:flex;align-items:center;gap:14px;flex-wrap:wrap;margin-bottom:16px;font-size:13px}.session-toolbar>.text-link{margin-left:auto}.interviewer-identity{display:flex;align-items:center;gap:12px}.interviewer-identity img{object-fit:contain}.wb-text{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.9}.question-text{font-size:18px;margin-top:20px}.answer-panel{margin-top:20px}.answer-mode{display:flex;gap:6px;padding:5px;background:var(--paper);border-radius:12px;width:fit-content}.answer-mode button{border:0;background:transparent;padding:10px 18px;border-radius:8px;min-height:44px;color:var(--muted)}.answer-mode button.active{background:#fff;color:var(--ink);box-shadow:0 1px 5px #29332b14;font-weight:600}.answer-actions{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-top:18px}.feedback-details{margin-top:20px}.session-aside{position:sticky;top:24px}.completion-panel h2{margin-top:20px}.completion-panel .button{margin-top:12px}summary{cursor:pointer;min-height:44px;padding:10px 0}.interview-heading h1{font-size:28px}
@media(max-width:1000px){.preparation-grid{grid-template-columns:minmax(0,1fr) 270px}.session-grid{grid-template-columns:1fr}.session-aside{position:static}.session-aside .session-facts{display:flex;gap:24px;flex-wrap:wrap}.session-aside .session-facts>div{flex:1}}
@media(max-width:767px){.in-session{padding:20px 16px calc(110px + env(safe-area-inset-bottom,0px))}.preparation-grid,.setup-fields{grid-template-columns:1fr}.preparation-summary{position:static}.interview-heading{display:flex;align-items:flex-start;gap:10px}.interview-heading h1{font-size:23px}.interview-heading .eyebrow{display:none}.interview-heading>.button{font-size:12px;min-height:44px;padding:8px 12px;flex-shrink:0;margin:0}.interview-heading p.muted{font-size:12px}.session-toolbar{gap:8px;font-size:12px}.question-text{font-size:16px}.answer-panel{padding:18px 16px}.answer-mode{width:100%}.answer-mode button{flex:1}.answer-actions{position:sticky;bottom:0;background:#fff;padding:12px 0 calc(12px + env(safe-area-inset-bottom,0px));margin-top:12px;z-index:5;flex-wrap:wrap}.answer-actions>.button{width:100%}.session-aside{display:none}.interviewer-identity img{width:52px;height:52px}}
</style>
