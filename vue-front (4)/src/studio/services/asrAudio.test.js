import test from 'node:test';
import assert from 'node:assert/strict';
import { encodePcmWav, prepareAsrWav } from './asrAudio.js';

const readAscii = (bytes, start, length) => String.fromCharCode(...bytes.slice(start, start + length));

test('encodePcmWav writes a 44-byte PCM16 mono WAV header and saturates samples', async () => {
  const blob = encodePcmWav(new Float32Array([1, -1, 2, -2, 0]), 8000);
  const bytes = new Uint8Array(await blob.arrayBuffer());
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);

  assert.equal(blob.type, 'audio/wav');
  assert.equal(bytes.byteLength, 44 + 5 * 2);
  assert.equal(readAscii(bytes, 0, 4), 'RIFF');
  assert.equal(view.getUint32(4, true), bytes.byteLength - 8);
  assert.equal(readAscii(bytes, 8, 4), 'WAVE');
  assert.equal(readAscii(bytes, 12, 4), 'fmt ');
  assert.equal(view.getUint32(16, true), 16);
  assert.equal(view.getUint16(20, true), 1);
  assert.equal(view.getUint16(22, true), 1);
  assert.equal(view.getUint32(24, true), 8000);
  assert.equal(view.getUint32(28, true), 16000);
  assert.equal(view.getUint16(32, true), 2);
  assert.equal(view.getUint16(34, true), 16);
  assert.equal(readAscii(bytes, 36, 4), 'data');
  assert.equal(view.getUint32(40, true), 10);
  assert.equal(view.getInt16(44, true), 32767);
  assert.equal(view.getInt16(46, true), -32768);
  assert.equal(view.getInt16(48, true), 32767);
  assert.equal(view.getInt16(50, true), -32768);
  assert.equal(view.getInt16(52, true), 0);
});

test('prepareAsrWav rejects an empty input before creating browser audio contexts', async () => {
  const originalAudioContext = globalThis.AudioContext;
  const originalOfflineAudioContext = globalThis.OfflineAudioContext;
  globalThis.AudioContext = class UnexpectedAudioContext {};
  globalThis.OfflineAudioContext = class UnexpectedOfflineAudioContext {};
  try {
    await assert.rejects(
      prepareAsrWav(new Blob([], { type: 'audio/wav' })),
      /音频文件为空/,
    );
  } finally {
    globalThis.AudioContext = originalAudioContext;
    globalThis.OfflineAudioContext = originalOfflineAudioContext;
  }
});

test('prepareAsrWav closes AudioContext when decoding fails', async () => {
  let closed = false;
  globalThis.AudioContext = class FakeAudioContext {
    decodeAudioData() {
      return Promise.reject(new Error('decode failed'));
    }

    async close() {
      closed = true;
    }
  };
  globalThis.OfflineAudioContext = class FakeOfflineAudioContext {};
  try {
    await assert.rejects(prepareAsrWav(new Blob([new Uint8Array([1])])), /decode failed/);
    assert.equal(closed, true);
  } finally {
    delete globalThis.AudioContext;
    delete globalThis.OfflineAudioContext;
  }
});
