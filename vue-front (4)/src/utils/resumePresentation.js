const labels = {
  'General parse': '通用分析', 'JD tuned': '结合岗位分析',
  'AI fallback': 'AI 改写失败，已使用兜底内容', 'AI rewrite': 'AI 改写完成',
  'Work and Internship': '工作与实习经历', 'Project Experience': '项目经历',
  Education: '教育经历', Parsed: '已解析', 'Need content': '待补充',
  'Rewrite with AI': 'AI 智能改写', 'Apply changes': '应用修改', Undo: '撤销',
  'Review and regenerate': '检查后重新生成', 'Generate final resume': '生成最终简历',
  'File parsed': '文件已解析',
  'Keep quantified impact and move metrics to the first sentence.': '保留量化成果，并将关键指标放在首句。',
  'Add measurable outcomes (latency, throughput, conversion, or cost saving).': '补充可衡量的成果，如延迟、吞吐量、转化率或成本节省。',
  'Provide target JD to generate stronger role-specific suggestions.': '补充目标岗位描述，以获得更有针对性的建议。',
  'Use stronger action verbs to highlight ownership.': '使用明确的行动词，突出个人职责与贡献。',
  'Increase JD keyword alignment in work bullets.': '在经历描述中加强与目标岗位关键词的对应。',
  'JD keyword alignment is acceptable; improve depth with concrete trade-offs.': '岗位关键词匹配尚可，可补充具体方案取舍来体现深度。',
  'Keep action verbs and add architecture constraints for context.': '保留行动词，并补充架构约束与业务背景。',
  'Add measurable outcomes if possible': '尽可能补充可衡量的成果',
  'Highlight responsibilities matching JD keywords': '突出与岗位关键词匹配的职责',
  'Highlight core impact and ownership': '突出核心成果与个人贡献',
  'Use action + context + result sentence style': '按照“行动、背景、结果”组织描述',
  'No work or internship fragment detected.': '未识别到工作或实习经历。',
  'No project fragment detected.': '未识别到项目经历。',
  'No project fragment found. Add one project with architecture, challenge, and measurable impact.': '未识别到项目经历，请补充项目架构、挑战与可衡量的成果。',
};

const translate = (value) => {
  if (typeof value !== 'string') return value;
  return labels[value] ?? value
    .replace(/^Fragments: (\d+)$/, '内容分区：$1')
    .replace(/^JD match (\d+)%$/, '岗位关键词匹配：$1%')
    .replace(/^File: /, '文件：')
    .replace(/^Detected (\d+) work or internship fragment\(s\) from parser output\.$/, '已识别 $1 个工作或实习内容片段。')
    .replace(/^Detected (\d+) project fragment\(s\)\. You can now refine project bullets\.$/, '已识别 $1 个项目内容片段，可继续完善项目描述。');
};

const fallbackMessage = (reason) => {
  if (!reason) return '后端未提供具体原因，请查看本次分析请求的 fallbackReason 或服务日志。';
  if (/timeout|timed out/i.test(reason)) return 'AI 改写请求超时；当前展示兜底内容，不代表改写成功。';
  if (/output truncated|finish_reason=length/i.test(reason)) return '模型输出达到长度上限，改写内容未完成，请重新生成。';
  if (/required tags missing/i.test(reason)) return '模型回复缺少规定的结果标签，无法读取完整改写结果。';
  if (/api key.*missing|401|unauthorized/i.test(reason)) return 'AI 服务认证失败或未配置密钥。';
  if (/429|rate limit/i.test(reason)) return 'AI 服务请求受限，请稍后重试。';
  if (/403|balance|quota/i.test(reason)) return 'AI 服务权限、余额或配额异常，请检查服务账户。';
  return 'AI 改写调用或结果处理失败，具体原因需查看服务日志。';
};

export const presentResumeAnalysis = (result) => {
  const fallback = result.fallback === true || result.badges?.includes('AI fallback') || result.badges?.includes(labels['AI fallback']) || false;
  return {
    ...result, fallback,
    scoreLabel: '规则参考分（非 AI 评分）',
    fallbackMessage: fallback ? fallbackMessage(result.fallbackReason) : '',
    badges: (result.badges || []).map(translate),
    sections: (result.sections || []).map(section => {
      const translated = {...section};
      for (const key of ['title','status','actionLabel','primaryActionLabel','secondaryActionLabel']) translated[key] = translate(section[key]);
      translated.suggestions = section.suggestions?.map(translate);
      if (typeof section.optimizedText === 'string') translated.optimizedText = translate(section.optimizedText)
        .replace('\n\nSuggested rewrite: start with business context, then architecture decisions, and end with measurable outcomes.', '\n\n规则建议：先介绍业务背景，再说明架构决策，最后列出可衡量的成果。')
        .replace(/\n\nSuggested rewrite: add explicit alignment with JD keywords \(([^\n]*)\) and quantify the final result\.$/, '\n\n规则建议：说明与岗位关键词（$1）的对应关系，并量化最终成果。');
      return translated;
    }),
    finalActionLabel: translate(result.finalActionLabel),
  };
};
