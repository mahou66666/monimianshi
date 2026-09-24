import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveSpeechMetrics } from './speechFallback.js';
test('speech fallback preserves real values including zero and fills only missing fields', () => {
  const r = resolveSpeechMetrics({ hasAudio: true, allowMock: true, result: { speed: { value: 0, source: 'real' }, clarity: { value: 101, source: 'real' } } });
  assert.equal(r.fields.speed.value, 0); assert.equal(r.fields.speed.source, 'real');
  assert.equal(r.fields.clarity.value, 80); assert.equal(r.fields.clarity.source, 'mock');
  assert.equal(r.isMock, true);
});
test('no audio or disabled fallback cannot produce mock assessment', () => {
  for (const input of [{ allowMock: true }, { hasAudio: true }]) {
    const r = resolveSpeechMetrics(input);
    assert.ok(Object.values(r.fields).every(f => f.value === null && f.source === 'not_assessed'));
    assert.equal(r.isMock, false);
  }
});
test('mock output is deterministic, serializable and never relabeled as real', () => {
  const input = { hasAudio: true, allowMock: true, result: { speed: { value: 213, source: 'mock' } } };
  assert.deepEqual(resolveSpeechMetrics(input), resolveSpeechMetrics(input));
  const r = JSON.parse(JSON.stringify(resolveSpeechMetrics(input)));
  assert.equal(r.fields.speed.source, 'mock'); assert.equal(r.fields.confidence.value, 75);
});
