<script setup>
import {ref,onUnmounted} from 'vue';
import {realRequest} from '../services/real.js';
import {prepareAsrWav} from '../services/asrAudio.js';
const props=defineProps({sessionId:String,question:Number,disabled:Boolean,remaining:{type:Number,default:12000}});
const emit=defineEmits(['text','busy']);
const filePicker=ref(null),fileName=ref(''),previewUrl=ref(''),durationLabel=ref('');
const phase=ref('idle'),transcript=ref(''),error=ref(''),seconds=ref(0);
let stream,recorder,timer,limit,disposed=false,parts=[],lastWav=null,lastMetrics=null;
function setPhase(value){phase.value=value;emit('busy',['requesting','recording','processing'].includes(value));}
function cleanup(){clearInterval(timer);clearTimeout(limit);stream?.getTracks().forEach(t=>t.stop());stream=null;}
async function recognize(wav){
  const id=props.sessionId,q=props.question;
  setPhase('processing');error.value='';
  try{const data=new FormData();data.append('audio',wav,'answer.wav');data.append('expectedQuestion',String(q));
    const response=await realRequest(`/interviews/${id}/transcribe`,{method:'POST',body:data,timeout:165000});
    if(!disposed&&props.sessionId===id&&props.question===q){transcript.value=response.text;lastMetrics=response.speechMetrics||null;}
  }catch(e){if(!disposed)error.value=e.message;}finally{if(!disposed)setPhase('idle');}
}
function clearAudio(){if(previewUrl.value)URL.revokeObjectURL(previewUrl.value);previewUrl.value='';fileName.value='';durationLabel.value='';lastWav=null;lastMetrics=null;transcript.value='';error.value='';}
async function process(blob,name='本次录音'){
  if(previewUrl.value)URL.revokeObjectURL(previewUrl.value);previewUrl.value='';
  fileName.value=name;durationLabel.value='';
  if(blob.size>0&&blob.size<=10*1024*1024)previewUrl.value=URL.createObjectURL(blob);
  setPhase('processing');error.value='';
  try{lastWav=await prepareAsrWav(blob);durationLabel.value=((lastWav.size-44)/32000).toFixed(1)+' 秒';if(!disposed)await recognize(lastWav);}catch(e){if(!disposed){error.value=['EncodingError','NotSupportedError'].includes(e.name)?'无法读取这段音频，请尝试 WAV 或 MP3 文件。':e.message;setPhase('idle');}}
}
async function start(){
  if(props.disabled||phase.value!=='idle')return;
  clearAudio();parts=[];seconds.value=0;setPhase('requesting');
  try{
    if(!navigator.mediaDevices?.getUserMedia||!globalThis.MediaRecorder)throw Error('当前浏览器不支持录音，请使用音频文件或文字回答。手机访问需 HTTPS。');
    stream=await navigator.mediaDevices.getUserMedia({audio:true});
    if(disposed){cleanup();return;}
    recorder=new MediaRecorder(stream);
    recorder.ondataavailable=e=>{if(e.data.size)parts.push(e.data);};
    recorder.onerror=()=>{error.value='录音中断，请检查麦克风后重试';recorder.onstop=null;cleanup();setPhase('idle');};
    recorder.onstop=()=>{cleanup();if(!disposed)process(new Blob(parts,{type:recorder.mimeType||'audio/webm'}));};
    recorder.start();setPhase('recording');timer=setInterval(()=>seconds.value++,1000);limit=setTimeout(stop,60000);
  }catch(e){cleanup();if(!disposed){error.value=e.name==='NotAllowedError'?'麦克风权限未开启。请允许麦克风，或改用文字回答。':e.message;setPhase('idle');}}
}
function stop(){if(recorder?.state==='recording')recorder.stop();cleanup();}
async function upload(event){const file=event.target.files?.[0];event.target.value='';if(!file||props.disabled||phase.value!=='idle')return;lastWav=null;lastMetrics=null;transcript.value='';await process(file,file.name);}
function useText(){if(!transcript.value.trim()||props.disabled)return;if(transcript.value.trim().length>props.remaining){error.value='加入后超过回答字数限制，请先精简回答或转写内容。';return;}emit('text',{text:transcript.value.trim(),metrics:lastMetrics});clearAudio();}
onUnmounted(()=>{disposed=true;if(recorder){recorder.onstop=null;recorder.ondataavailable=null;if(recorder.state==='recording')recorder.stop();}cleanup();parts=[];lastWav=null;if(previewUrl.value)URL.revokeObjectURL(previewUrl.value);emit('busy',false);});
</script>
<template><section class="asr-input">
  <div class="section-heading"><h3>说出你的回答</h3><span v-if="phase==='recording'" role="status">录音中 {{seconds}} 秒 / 60 秒</span></div>
  <p class="small muted">单次最多 60 秒，可分段录制。停止后核对转写，再加入回答。</p>
  <div class="asr-actions"><button v-if="phase!=='recording'" type="button" class="button primary record-button" :disabled="disabled||phase!=='idle'" @click="start">开始录音</button><button v-else type="button" class="button primary record-button" @click="stop">停止并转写</button><span class="asr-or">或</span><div class="audio-upload"><input ref="filePicker" type="file" accept="audio/*,.wav,.mp3,.m4a,.webm" hidden :disabled="disabled||phase!=='idle'" @change="upload"/><button type="button" class="button secondary upload-button" :disabled="disabled||phase!=='idle'" @click="filePicker.click()"><span aria-hidden="true">↑</span>{{fileName?'更换音频文件':'上传已有音频'}}</button><small class="muted">WAV / MP3 / M4A / WebM · 最大 10MB，0.3–61 秒</small></div></div>
  <div v-if="fileName" class="audio-file-card"><div class="audio-file-heading"><div class="audio-file-info"><strong>{{fileName}}</strong><span class="small muted">{{durationLabel||'等待检查音频'}} · {{phase==='processing'?'正在转写':transcript?'转写完成，请核对':error?'未完成转写':'已选择'}}</span></div><button type="button" class="text-link" :disabled="disabled||phase!=='idle'" @click="clearAudio">移除</button></div><audio v-if="previewUrl" :src="previewUrl" controls preload="metadata" aria-label="试听所选音频"/><p class="small muted">可以先试听确认声音是否清晰。转写文字不会自动提交面试。</p></div>
  <p v-if="phase==='requesting'" class="notice" role="status">等待麦克风授权…</p><p v-if="phase==='processing'" class="notice" role="status">正在识别，请稍候…</p>
  <p v-if="error" class="error" role="alert">{{error}}</p><button v-if="error&&lastWav" class="text-link" type="button" :disabled="disabled||phase!=='idle'" @click="recognize(lastWav)">重试这段音频</button>
  <div v-if="transcript"><label>核对转写内容<textarea v-model="transcript" rows="5" maxlength="12000" :disabled="disabled||phase!=='idle'"/></label><button class="button secondary" type="button" :disabled="disabled||phase!=='idle'||!transcript.trim()" @click="useText">确认文字并加入回答</button><p class="small muted">会追加到现有文字后。确认回答后，再点击“提交回答”。</p></div>
</section></template>
<style scoped>
.asr-input{padding:18px 0;margin:0}.record-button{min-width:150px}.asr-actions{display:flex;gap:16px;align-items:center;flex-wrap:wrap;margin:24px 0}.asr-or{color:var(--muted);font-size:13px}.audio-upload{display:flex;align-items:flex-start;flex-direction:column;gap:7px;min-width:0}.audio-upload input[hidden]{display:none}.upload-button{gap:9px}.upload-button>span{font-size:22px;line-height:1}.audio-upload small{font-size:12px}.asr-input h3{margin:0}.audio-file-card{background:#f7f9f2;border:1px solid var(--line);border-radius:12px;padding:16px;margin:16px 0}.audio-file-heading{display:flex;justify-content:space-between;align-items:center;gap:12px}.audio-file-info{display:flex;flex-direction:column;min-width:0}.audio-file-info strong{overflow-wrap:anywhere;font-size:14px}.audio-file-heading button{flex-shrink:0}.audio-file-card audio{width:100%;height:42px;margin-top:14px}.audio-file-card p{margin-bottom:0}@media(max-width:767px){.asr-actions{gap:12px}.record-button,.audio-upload,.upload-button{width:100%}.asr-or{display:none}.audio-upload small{line-height:1.6}.audio-file-card{padding:12px}}
</style>
