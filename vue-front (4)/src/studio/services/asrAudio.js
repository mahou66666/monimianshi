const TARGET_SAMPLE_RATE = 16000;
const MAX_INPUT_BYTES = 10 * 1024 * 1024;
const MIN_DURATION_SECONDS = 0.3;
const MAX_DURATION_SECONDS = 61;

const getBrowserConstructor = (name, prefixedName) => {
  const root = typeof globalThis === 'undefined' ? {} : globalThis;
  const windowObject = root.window || {};
  return root[name] || root[prefixedName] || windowObject[name] || windowObject[prefixedName] || null;
};

const writeAscii = (view, offset, value) => {
  for (let index = 0; index < value.length; index += 1) {
    view.setUint8(offset + index, value.charCodeAt(index));
  }
};

export function encodePcmWav(samples, sampleRate = TARGET_SAMPLE_RATE) {
  if (!samples || typeof samples.length !== 'number') {
    throw new TypeError('samples 必须是 Float32Array。');
  }
  if (!Number.isFinite(sampleRate) || !Number.isInteger(sampleRate) || sampleRate <= 0) {
    throw new RangeError('sampleRate 必须是正整数。');
  }

  const frameCount = samples.length;
  const dataSize = frameCount * 2;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);

  writeAscii(view, 0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeAscii(view, 8, 'WAVE');
  writeAscii(view, 12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  writeAscii(view, 36, 'data');
  view.setUint32(40, dataSize, true);

  for (let index = 0; index < frameCount; index += 1) {
    const sample = Number(samples[index]);
    const clamped = Number.isNaN(sample) ? 0 : Math.max(-1, Math.min(1, sample));
    const pcm = clamped < 0 ? Math.round(clamped * 0x8000) : Math.round(clamped * 0x7fff);
    view.setInt16(44 + index * 2, pcm, true);
  }

  return new Blob([buffer], { type: 'audio/wav' });
}

const getDecodedDuration = (audioBuffer, frameCount, sampleRate) => {
  const duration = Number(audioBuffer?.duration);
  if (Number.isFinite(duration) && duration >= 0) {
    return duration;
  }
  return frameCount / sampleRate;
};

const assertInputBlob = (blob) => {
  if (!blob || typeof blob.arrayBuffer !== 'function') {
    throw new TypeError('音频文件无效。');
  }
  if (Number(blob.size) === 0) {
    throw new Error('音频文件为空。');
  }
  if (Number.isFinite(Number(blob.size)) && Number(blob.size) > MAX_INPUT_BYTES) {
    throw new Error('音频文件不能超过 10 MB。');
  }
};

const decodeAudioData = (audioContext, arrayBuffer) => new Promise((resolve, reject) => {
  let settled = false;
  const resolveOnce = (value) => {
    if (!settled) {
      settled = true;
      resolve(value);
    }
  };
  const rejectOnce = (error) => {
    if (!settled) {
      settled = true;
      reject(error);
    }
  };

  let result;
  try {
    result = audioContext.decodeAudioData(arrayBuffer, resolveOnce, rejectOnce);
  } catch (error) {
    rejectOnce(error);
    return;
  }

  if (result && typeof result.then === 'function') {
    result.then(resolveOnce, rejectOnce);
  } else if (result !== undefined || audioContext.decodeAudioData.length < 2) {
    resolveOnce(result);
  }
});

const mixChannelsToMono = (decodedBuffer) => {
  const channelCount = Number(decodedBuffer.numberOfChannels);
  const frameCount = Number(decodedBuffer.length);
  if (!Number.isInteger(channelCount) || channelCount < 1 || !Number.isInteger(frameCount) || frameCount < 1) {
    throw new Error('音频解码结果为空。');
  }

  const mono = new Float32Array(frameCount);
  for (let channelIndex = 0; channelIndex < channelCount; channelIndex += 1) {
    const channel = decodedBuffer.getChannelData(channelIndex);
    for (let frameIndex = 0; frameIndex < frameCount; frameIndex += 1) {
      mono[frameIndex] += channel[frameIndex] / channelCount;
    }
  }
  return mono;
};

const renderAtTargetRate = async (monoSamples, sourceRate, duration, OfflineAudioContextCtor) => {
  const outputLength = Math.max(1, Math.ceil(duration * TARGET_SAMPLE_RATE));
  const offlineContext = new OfflineAudioContextCtor(1, outputLength, TARGET_SAMPLE_RATE);
  const sourceBuffer = offlineContext.createBuffer(1, monoSamples.length, sourceRate);
  sourceBuffer.getChannelData(0).set(monoSamples);

  const sourceNode = offlineContext.createBufferSource();
  sourceNode.buffer = sourceBuffer;
  sourceNode.connect(offlineContext.destination);
  sourceNode.start(0);

  const renderedBuffer = await offlineContext.startRendering();
  const renderedSamples = renderedBuffer?.getChannelData?.(0);
  if (!renderedSamples || renderedSamples.length < 1) {
    throw new Error('音频重采样结果为空。');
  }
  return renderedSamples;
};

export async function prepareAsrWav(blob) {
  assertInputBlob(blob);

  const sourceArrayBuffer = await blob.arrayBuffer();
  if (!sourceArrayBuffer || sourceArrayBuffer.byteLength === 0) {
    throw new Error('音频文件为空。');
  }
  if (sourceArrayBuffer.byteLength > MAX_INPUT_BYTES) {
    throw new Error('音频文件不能超过 10 MB。');
  }

  const AudioContextCtor = getBrowserConstructor('AudioContext', 'webkitAudioContext');
  if (!AudioContextCtor) {
    throw new Error('当前浏览器不支持 AudioContext，无法解码音频。');
  }
  const OfflineAudioContextCtor = getBrowserConstructor('OfflineAudioContext', 'webkitOfflineAudioContext');
  if (!OfflineAudioContextCtor) {
    throw new Error('当前浏览器不支持 OfflineAudioContext，无法重采样音频。');
  }

  const audioContext = new AudioContextCtor();
  try {
    if (typeof audioContext.decodeAudioData !== 'function') {
      throw new Error('当前浏览器不支持 AudioContext.decodeAudioData，无法解码音频。');
    }
    const decodedBuffer = await decodeAudioData(audioContext, sourceArrayBuffer.slice(0));
    const sourceRate = Number(decodedBuffer?.sampleRate);
    const frameCount = Number(decodedBuffer?.length);
    if (!Number.isFinite(sourceRate) || sourceRate <= 0 || !Number.isInteger(frameCount) || frameCount < 1) {
      throw new Error('音频解码结果为空。');
    }

    const duration = getDecodedDuration(decodedBuffer, frameCount, sourceRate);
    if (!Number.isFinite(duration) || duration < MIN_DURATION_SECONDS || duration > MAX_DURATION_SECONDS) {
      throw new Error('音频时长必须在 0.3 到 61 秒之间。');
    }

    const monoSamples = mixChannelsToMono(decodedBuffer);
    const resampledSamples = await renderAtTargetRate(
      monoSamples,
      sourceRate,
      duration,
      OfflineAudioContextCtor,
    );
    return encodePcmWav(resampledSamples, TARGET_SAMPLE_RATE);
  } finally {
    if (typeof audioContext.close === 'function') {
      try {
        await audioContext.close();
      } catch {
        // AudioContext 已经完成解码；关闭失败不应覆盖原始处理结果或错误。
      }
    }
  }
}
