// Explicit field-level fallback; never infer acoustic measurements from answer text.
export function resolveSpeechMetrics({ hasAudio = false, result = {}, allowMock = false } = {}) {
  const defaults = { speed: 180, clarity: 80, confidence: 75, emotion: '平稳', advice: '先说明结论，再分点解释依据，在观点之间适当停顿。' };
  const labels = { speed: '语速', clarity: '清晰度', confidence: '表达自信度（估计）', emotion: '情感表现（示例）', advice: '改进建议' };
  const valid = (key, value) => ['emotion', 'advice'].includes(key)
    ? typeof value === 'string' && value.trim().length > 0
    : typeof value === 'number' && Number.isFinite(value) && value >= 0 && (key === 'speed' ? value <= 1000 : value <= 100);
  const fields = Object.fromEntries(Object.entries(defaults).map(([key, fallback]) => {
    const field = result?.[key];
    const real = hasAudio && field?.source === 'real' && valid(key, field.value);
    const mock = hasAudio && allowMock && !real;
    return [key, {
      label: labels[key], value: real ? field.value : mock ? fallback : null,
      unit: key === 'speed' ? '字/分钟' : ['clarity', 'confidence'].includes(key) ? '分' : '',
      source: real ? 'real' : mock ? 'mock' : 'not_assessed',
      reason: real ? '' : !hasAudio ? '无有效音频，未评估' : mock ? '真实字段缺失或无效，使用固定 Mock 示例' : '真实分析暂不可用',
    }];
  }));
  return { schemaVersion: 'speech-v1', isMock: Object.values(fields).some(f => f.source === 'mock'), fields };
}
