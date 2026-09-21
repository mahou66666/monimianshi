import test from 'node:test';
import assert from 'node:assert/strict';
import { presentResumeAnalysis } from './resumePresentation.js';

test('legacy fallback response stays explicit and its UI is Chinese', () => {
  const result = presentResumeAnalysis({score:80, fallback:true, fallbackReason:'siliconflow request timeout after 180000 ms', badges:['General parse','Fragments: 7','AI fallback'], sections:[{title:'Work and Internship',status:'Parsed',suggestions:['Keep quantified impact and move metrics to the first sentence.']},{title:'Project Experience',optimizedText:'Vue 3 项目\n\nSuggested rewrite: start with business context, then architecture decisions, and end with measurable outcomes.'}],finalActionLabel:'Review and regenerate'});
  assert.equal(result.fallback,true);
  assert.match(result.fallbackMessage,/超时/);
  assert.match(result.scoreLabel,/非 AI/);
  assert.equal(result.score,80);
  assert.deepEqual(result.badges,['通用分析','内容分区：7','AI 改写失败，已使用兜底内容']);
  assert.equal(result.sections[0].title,'工作与实习经历');
  assert.match(result.sections[1].optimizedText,/Vue 3 项目/);
  assert.doesNotMatch(JSON.stringify(result.sections),/Suggested rewrite|Keep quantified/);
});

test('old response with only fallback badge does not look like AI success', () => {
  const result=presentResumeAnalysis({badges:['AI fallback'],sections:[]});
  assert.equal(result.fallback,true);
  assert.match(result.fallbackMessage,/未提供/);
});

test('success and user English content are preserved', () => {
  const result=presentResumeAnalysis({fallback:false,badges:['AI rewrite'],sections:[{originalText:'Designed Vue applications',optimizedText:'Implemented React services'}]});
  assert.equal(result.fallback,false);
  assert.equal(result.sections[0].optimizedText,'Implemented React services');
  assert.deepEqual(result.badges,['AI 改写完成']);
});
