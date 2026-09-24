import test from 'node:test';
import assert from 'node:assert/strict';
import {createQuestionSpeech} from './interviewerVoice.js';
function setup(){
  const queue=[];let cancelled=0;
  class Utterance{constructor(text){this.text=text;}}
  const voices=[{lang:'en-US',localService:true},{lang:'zh-CN',localService:true}];
  const synthesis={getVoices:()=>voices,speak:u=>queue.push(u),cancel:()=>cancelled++};
  return {speech:createQuestionSpeech(synthesis,Utterance),queue,voices,cancelled:()=>cancelled};
}
test('interviewer speaks real question text using available Chinese voice',()=>{
  const {speech,queue,voices}=setup();let started=false,ended=false;
  assert.equal(speech.speak('请介绍你的项目。',{start:()=>started=true,end:()=>ended=true}),true);
  assert.equal(queue[0].text,'请介绍你的项目。');assert.equal(queue[0].voice,voices[1]);
  queue[0].onstart();queue[0].onend();assert.ok(started&&ended);
});
test('new question cancels previous speech and ignores stale completion callbacks',()=>{
  const {speech,queue,cancelled}=setup();let stale=false;
  speech.speak('原题',{end:()=>stale=true});const oldEnd=queue[0].onend;
  speech.speak('追问');oldEnd();assert.equal(stale,false);assert.equal(cancelled(),1);assert.equal(queue[1].text,'追问');
  speech.stop();assert.equal(cancelled(),2);assert.equal(queue[1].onstart,null);
});
test('unsupported or blocked browser speech can safely fall back to text',()=>{
  const unsupported=createQuestionSpeech(null,null);assert.equal(unsupported.supported,false);assert.equal(unsupported.speak('问题'),false);unsupported.stop();
  let error='';const {speech,queue}=setup();speech.speak('问题',{error:value=>error=value});queue[0].onerror({error:'not-allowed'});assert.equal(error,'not-allowed');speech.stop();
});
