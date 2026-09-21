import { MOCK_DELAY } from '@/utils/constants';

const wait = (delay) => new Promise((resolve) => setTimeout(resolve, delay));

const ROLE_DEFINITIONS = [
  {
    key: 'frontend',
    scoreBase: 1,
    keywords: [
      '前端',
      '前端开发',
      'web',
      'h5',
      'javascript',
      'typescript',
      'vue',
      'react',
      'uniapp',
      'css',
      '小程序',
    ],
    title: '前端开发工程师',
    summary:
      '该岗位聚焦用户体验与前端工程化能力，重点考察页面性能优化、组件设计、跨端适配与业务协同落地能力。',
    tags: ['前端工程化', '交互体验', '性能优化', '跨端适配'],
    hardSkills: [
      '熟练掌握 JavaScript/TypeScript 与主流前端框架（Vue/React）',
      '具备组件化开发、状态管理与前端工程化实践能力',
      '能够开展首屏优化、打包优化与浏览器兼容性处理',
    ],
    softSkills: [
      '具备良好的需求理解与跨团队沟通能力',
      '能够在迭代中持续优化用户体验与交互细节',
      '面对线上问题具备快速定位与复盘改进意识',
    ],
  },
  {
    key: 'backend',
    scoreBase: 1,
    keywords: [
      '后端',
      '后端开发',
      'java',
      'spring',
      'springboot',
      'mysql',
      'redis',
      '数据库',
      '微服务',
      '接口',
      'golang',
      'go',
    ],
    title: '后端开发工程师',
    summary:
      '该岗位强调系统设计与服务稳定性，重点考察接口设计、数据库建模、并发处理与服务治理能力。',
    tags: ['系统设计', '服务治理', '数据库设计', '高并发'],
    hardSkills: [
      '熟练掌握 Java/Go 等后端语言及 Spring Boot 等常用框架',
      '具备数据库建模、SQL 优化与缓存策略设计能力',
      '能够完成接口规范设计、异常治理与链路排障',
    ],
    softSkills: [
      '具备清晰的技术文档表达与需求拆解能力',
      '能够与前端、测试高效协作推动版本落地',
      '有稳定性意识并能主动进行风险预判',
    ],
  },
  {
    key: 'algorithm',
    scoreBase: 1,
    keywords: [
      '算法',
      '算法工程师',
      '机器学习',
      '深度学习',
      '模型训练',
      'llm',
      '推荐',
      'nlp',
      'cv',
      '特征工程',
      'pytorch',
      'tensorflow',
    ],
    title: '算法工程师',
    summary:
      '该岗位关注模型效果与工程落地的平衡，重点考察算法理解、特征建模、实验设计与线上效果迭代能力。',
    tags: ['模型效果', '实验设计', '数据驱动', '工程落地'],
    hardSkills: [
      '熟悉机器学习/深度学习核心方法及常见评估指标',
      '具备数据清洗、特征工程、模型训练与调优能力',
      '能够将模型部署到业务链路并持续监控效果',
    ],
    softSkills: [
      '具备问题抽象能力与严谨的实验对比意识',
      '能够与产品和工程团队协同定义可落地目标',
      '具备持续学习新算法与快速迁移应用能力',
    ],
  },
  {
    key: 'pm',
    scoreBase: 1,
    keywords: [
      '产品经理',
      '产品',
      'prd',
      '需求分析',
      '需求管理',
      '用户研究',
      'roadmap',
      '原型',
      'axure',
      '增长',
    ],
    title: '产品经理',
    summary:
      '该岗位重点考察业务洞察与产品推进能力，关注需求分析、方案设计、跨团队协同与结果复盘。',
    tags: ['需求分析', '业务理解', '项目推进', '用户价值'],
    hardSkills: [
      '能够独立完成需求调研、PRD 编写与原型设计',
      '熟悉数据分析方法并能基于指标驱动迭代',
      '具备版本节奏管理与跨团队协同推进能力',
    ],
    softSkills: [
      '具备清晰表达与多方对齐能力',
      '面对不确定需求具备优先级判断能力',
      '能够持续复盘并沉淀产品方法论',
    ],
  },
  {
    key: 'qa',
    scoreBase: 1,
    keywords: [
      '测试',
      '测试工程师',
      'qa',
      '自动化测试',
      '接口测试',
      '性能测试',
      '回归测试',
      'jmeter',
      'pytest',
      '质量保障',
    ],
    title: '测试工程师',
    summary:
      '该岗位聚焦质量保障与风险控制，重点考察测试设计、自动化能力、缺陷分析与跨端验证能力。',
    tags: ['质量保障', '自动化测试', '缺陷分析', '回归体系'],
    hardSkills: [
      '熟悉功能、接口、性能测试方法与用例设计',
      '具备自动化测试脚本编写与持续集成接入能力',
      '能够进行缺陷定位、复现与质量数据分析',
    ],
    softSkills: [
      '具备较强的风险意识与细节敏感度',
      '能够与研发高效沟通并推动问题闭环',
      '在多任务场景下保持测试节奏与优先级管理',
    ],
  },
  {
    key: 'ops',
    scoreBase: 1,
    keywords: [
      '运维',
      'devops',
      'sre',
      'k8s',
      'kubernetes',
      'docker',
      'linux',
      '监控',
      '告警',
      'ci/cd',
      '发布',
      '稳定性',
    ],
    title: '运维工程师',
    summary:
      '该岗位关注系统稳定性与交付效率，重点考察自动化运维、监控告警、故障应急和持续交付能力。',
    tags: ['稳定性建设', '自动化运维', '监控告警', '持续交付'],
    hardSkills: [
      '熟悉 Linux、容器化与 K8s 相关运维体系',
      '具备 CI/CD 流水线搭建与发布策略设计能力',
      '能够进行监控告警配置、故障定位与容量规划',
    ],
    softSkills: [
      '具备较强的应急响应与复盘改进意识',
      '能够与研发协作推进可观测性建设',
      '具备标准化文档沉淀与流程优化能力',
    ],
  },
];

const DEFAULT_ROLE = {
  key: 'general',
  title: '技术岗位',
  summary:
    '该岗位强调专业基础与实践能力，建议结合具体业务场景重点准备技术深度、问题分析和协作表达。',
  tags: ['专业基础', '问题解决', '团队协作', '业务理解'],
  hardSkills: [
    '掌握岗位对应技术栈并具备项目落地经验',
    '能够围绕场景进行技术选型与方案设计',
    '具备代码质量、稳定性与性能优化意识',
  ],
  softSkills: [
    '表达清晰，能够结构化阐述方案与结果',
    '具备主动学习能力与快速问题定位能力',
    '能够在团队协作中推动任务闭环',
  ],
};

const normalizeText = (text = '') => String(text).toLowerCase();

const countKeywordScore = (text, keywords = []) => {
  let score = 0;

  keywords.forEach((keyword) => {
    if (!keyword) {
      return;
    }

    const hit = text.includes(String(keyword).toLowerCase());
    if (hit) {
      score += 1;
    }
  });

  return score;
};

const detectRole = (jdText = '') => {
  const text = normalizeText(jdText);

  let bestRole = null;
  let bestScore = 0;

  ROLE_DEFINITIONS.forEach((role) => {
    const score = countKeywordScore(text, role.keywords) + (role.scoreBase || 0);

    if (score > bestScore) {
      bestScore = score;
      bestRole = role;
    }
  });

  if (!bestRole) {
    return DEFAULT_ROLE;
  }

  if (bestScore <= (bestRole.scoreBase || 0)) {
    return DEFAULT_ROLE;
  }

  return bestRole;
};

const extractYearRequirementTag = (jdText = '') => {
  const text = String(jdText || '');
  const match = text.match(/(\d+)\s*年/);

  if (!match) {
    return '';
  }

  const year = Number(match[1]);
  if (!Number.isFinite(year) || year <= 0) {
    return '';
  }

  return `${year}年以上经验`;
};

const buildSummary = (role, jdText = '') => {
  const text = String(jdText || '');

  const hasTeam = /协作|沟通|跨部门|团队/.test(text);
  const hasProject = /项目|落地|交付|上线/.test(text);

  const fragments = [role.summary];

  if (hasProject) {
    fragments.push('从 JD 信息看，岗位对项目落地与结果交付有明确要求。');
  }

  if (hasTeam) {
    fragments.push('同时比较看重跨团队协作与沟通推进能力。');
  }

  return fragments.join(' ');
};

const uniqueList = (list = []) => [...new Set(list.filter(Boolean))];

const buildJdAnalysisResult = (jdText = '') => {
  const role = detectRole(jdText);
  const yearTag = extractYearRequirementTag(jdText);
  const tags = uniqueList([...role.tags, yearTag]);

  return {
    title: role.title,
    summary: buildSummary(role, jdText),
    tags,
    hardSkills: role.hardSkills,
    softSkills: role.softSkills,
  };
};

export const parseJd = async (payload = {}) => {
  await wait(MOCK_DELAY.normal);

  const jdText = String(payload?.jdText || '');

  return {
    code: 200,
    data: buildJdAnalysisResult(jdText),
  };
};
