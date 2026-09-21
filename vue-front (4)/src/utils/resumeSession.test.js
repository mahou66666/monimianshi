import test from 'node:test';
import assert from 'node:assert/strict';
import {readResumeSession,saveResumeSession,RESUME_SESSION_KEY} from './resumeSession.js';
const memory=()=>{const m=new Map();return {getItem:k=>m.get(k),setItem:(k,v)=>m.set(k,v),removeItem:k=>m.delete(k)}};
test('completed result survives refresh without file or credential storage',()=>{
 const s=memory();saveResumeSession(s,'user:1',{targetJdText:'前端',selectedFile:'private file',status:{analysis:'success'},uploadResult:{resumeId:7,fileName:'test.pdf',token:'secret'},analysisResult:{score:80}});
 const restored=readResumeSession(s,'user:1');assert.equal(restored.uploadResult.resumeId,7);assert.equal(restored.analysisResult.score,80);assert.doesNotMatch(s.getItem(RESUME_SESSION_KEY),/private file|secret/);
 assert.equal(readResumeSession(s,'user:2'),null);assert.equal(readResumeSession(s,'user:1',Date.now()+86400001),null);
});
test('in-flight upload cannot trigger another paid analysis on refresh',()=>{
 const s=memory();saveResumeSession(s,'user:1',{targetJdText:'draft',status:{analysis:'loading'},uploadResult:{resumeId:7},analysisResult:null});
 assert.equal(readResumeSession(s,'user:1').uploadResult,null);
 saveResumeSession(s,null,{});assert.equal(s.getItem(RESUME_SESSION_KEY),undefined);
});
test('corrupt or unavailable storage is safe',()=>{
 assert.equal(readResumeSession({getItem:()=>'{broken'},'user:1'),null);
 assert.doesNotThrow(()=>saveResumeSession({setItem:()=>{throw Error('quota')}},'user:1',{status:{analysis:'idle'}}));
});
