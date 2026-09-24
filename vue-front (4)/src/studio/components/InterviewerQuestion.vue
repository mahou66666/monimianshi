<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { createQuestionSpeech, interviewerVoiceEnabled, setInterviewerVoice } from '../services/interviewerVoice.js';
const props=defineProps({text:{type:String,default:''},stage:String,questionKey:String,busy:Boolean,recording:Boolean});
const speech=createQuestionSpeech(globalThis.speechSynthesis,globalThis.SpeechSynthesisUtterance);
const visible=ref(''),typing=ref(false),speaking=ref(false),starting=ref(false),hint=ref('');
const fullText=computed(()=>props.text.trim());
let typeTimer,startTimer,pending=false,disposed=false;
const reducedMotion=globalThis.matchMedia?.('(prefers-reduced-motion: reduce)');
function reveal(){clearInterval(typeTimer);visible.value=fullText.value;typing.value=false;}
function stop(){clearTimeout(startTimer);speech.stop();speaking.value=false;starting.value=false;}
function readQuestion(){
  if(disposed||document.hidden||!speech.supported||props.busy||props.recording||!fullText.value||!interviewerVoiceEnabled.value)return;
  stop();pending=false;hint.value='';starting.value=true;
  startTimer=setTimeout(()=>{if(starting.value){stop();hint.value='浏览器未开始播放，请点击“朗读问题”重试。';}},5000);
  speech.speak(fullText.value,{
    start(){clearTimeout(startTimer);starting.value=false;speaking.value=true;},
    end(){clearTimeout(startTimer);starting.value=false;speaking.value=false;},
    error(){clearTimeout(startTimer);starting.value=false;speaking.value=false;hint.value='暂时无法播放声音，可点击重试或继续无声面试。';},
  });
}
function toggle(){setInterviewerVoice(!interviewerVoiceEnabled.value);if(interviewerVoiceEnabled.value){if(!starting.value&&!speaking.value)readQuestion();}else{pending=false;stop();hint.value='';}}
watch(()=>[props.questionKey,props.text],()=>{
  stop();clearInterval(typeTimer);hint.value='';pending=Boolean(fullText.value);visible.value='';
  const letters=Array.from(fullText.value);let index=0;
  if(reducedMotion?.matches||!letters.length){reveal();}else{
    typing.value=true;typeTimer=setInterval(()=>{index=Math.min(index+1,letters.length);visible.value=letters.slice(0,index).join('');if(index===letters.length)reveal();},32);
  }
  if(!props.busy&&!props.recording&&interviewerVoiceEnabled.value)readQuestion();
},{immediate:true});
watch(()=>[props.busy,props.recording,interviewerVoiceEnabled.value],()=>{
  if(props.busy||props.recording||!interviewerVoiceEnabled.value){stop();if(props.recording){pending=false;reveal();}return;}
  if(pending)readQuestion();
},{flush:'sync'});
function visibility(){if(document.hidden){pending=false;stop();}}
onMounted(()=>document.addEventListener('visibilitychange',visibility));
onUnmounted(()=>{disposed=true;clearInterval(typeTimer);stop();document.removeEventListener('visibilitychange',visibility);});
</script>
<template><article class="card interviewer-question">
<div class="question-heading"><div class="interviewer-identity"><img src="/images/interview-fairy-transparent.png" alt="" width="64" height="64"/><div><span class="small muted">面试精灵</span><h2>{{stage}}</h2></div></div><button type="button" class="button secondary voice-toggle" :aria-pressed="interviewerVoiceEnabled" :disabled="!speech.supported" @click="toggle">{{interviewerVoiceEnabled&&speech.supported?'有声面试 · 切换无声':'无声面试 · 开启声音'}}</button></div>
<p v-if="fullText" class="question-text" aria-hidden="true">{{visible}}<span v-if="typing" class="typing-caret" aria-hidden="true">▍</span></p><p v-else class="muted" role="status">面试官正在准备问题…</p>
<p class="screen-reader-text" aria-live="polite" aria-atomic="true">{{fullText}}</p>
<div class="question-tools"><span class="small muted" role="status">{{recording?'回答处理中，朗读已停止':speaking?'面试官正在朗读…':starting?'正在准备声音…':typing?'正在呈现问题…':interviewerVoiceEnabled&&speech.supported?'听完问题，按自己的节奏回答':'无声面试，阅读题目后回答'}}</span><button v-if="typing" type="button" class="text-link" @click="reveal">显示完整题目</button><button v-if="interviewerVoiceEnabled&&speech.supported&&fullText" type="button" class="text-link" :disabled="busy||recording" @click="speaking||starting?stop():readQuestion()">{{speaking||starting?'停止朗读':'朗读问题'}}</button></div>
<p v-if="hint" class="small muted" role="status">{{hint}}</p><p v-if="!speech.supported" class="small muted">当前浏览器不支持朗读，已使用无声面试。</p>
</article></template>
<style scoped>
.question-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap}.interviewer-identity{display:flex;align-items:center;gap:12px}.interviewer-identity img{object-fit:contain}.voice-toggle{font-size:12px;min-height:44px}.question-text{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.9;font-size:18px;min-height:3.8em;margin-top:20px}.typing-caret{color:#9a7b26;animation:caret-pulse .8s steps(2) infinite}.question-tools{display:flex;align-items:center;gap:16px;flex-wrap:wrap;margin-top:14px}.question-tools>span{margin-right:auto}.screen-reader-text{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap}@keyframes caret-pulse{50%{opacity:0}}@media(max-width:767px){.question-text{font-size:16px}.interviewer-identity img{width:52px;height:52px}.question-heading{gap:10px}.voice-toggle{padding:8px 12px}.question-tools{gap:12px}}@media(prefers-reduced-motion:reduce){.typing-caret{animation:none}}
</style>
