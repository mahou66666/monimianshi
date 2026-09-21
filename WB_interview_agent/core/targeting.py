from typing import TypedDict


class InterviewPlan(TypedDict):
    jd_skills: list[str]
    resume_skills: list[str]
    strengths: list[str]
    gaps: list[str]
    tech_focus_topics: list[str]
    hr_focus_topics: list[str]


TECH_ALIAS_MAP = {
    "Java": ["java", "jvm"],
    "Spring Boot": ["spring boot", "spring"],
    "MySQL": ["mysql", "sql", "索引"],
    "Redis": ["redis", "缓存"],
    "Kafka": ["kafka", "消息队列", "mq"],
    "微服务": ["微服务", "service mesh"],
    "分布式一致性": ["一致性", "事务", "tcc", "2pc", "幂等"],
    "高并发": ["高并发", "高性能", "压测", "限流", "降级"],
    "容器化": ["docker", "kubernetes", "k8s"],
    "监控与可观测": ["监控", "告警", "可观测", "prometheus", "grafana"],
}

HR_ALIAS_MAP = {
    "团队协作": ["团队", "协作", "跨部门", "team"],
    "抗压能力": ["抗压", "高压", "压力"],
    "沟通表达": ["沟通", "表达", "反馈"],
    "冲突管理": ["冲突", "分歧", "矛盾"],
    "执行力": ["执行", "推进", "落地", "交付"],
    "复盘能力": ["复盘", "总结", "回顾"],
}

INDUSTRY_DEFAULT_TECH_TOPICS = {
    "金融": ["分布式一致性", "高并发", "监控与可观测"],
    "医疗": ["系统可靠性", "数据隐私", "监控与可观测"],
    "制造": ["高并发", "系统稳定性", "监控与可观测"],
    "零售": ["高并发", "Redis", "分布式一致性"],
}

DEFAULT_TECH_TOPICS = ["高并发", "系统设计", "故障排查"]
DEFAULT_HR_TOPICS = ["团队协作", "抗压能力", "执行力"]


def _find_alias_hits(text: str, alias_map: dict[str, list[str]]) -> list[str]:
    normalized = (text or "").lower()
    hits = []
    for canonical, aliases in alias_map.items():
        matched = False
        for alias in aliases:
            if alias.lower() in normalized:
                matched = True
                break
        if matched:
            hits.append(canonical)
    return hits


def _merge_unique(primary: list[str], secondary: list[str]) -> list[str]:
    merged = []
    for item in primary + secondary:
        if item and item not in merged:
            merged.append(item)
    return merged


def _industry_default_topics(industry: str) -> list[str]:
    for key, topics in INDUSTRY_DEFAULT_TECH_TOPICS.items():
        if key in (industry or ""):
            return topics
    return DEFAULT_TECH_TOPICS


def build_interview_plan(jd: str, resume: str, industry: str) -> InterviewPlan:
    jd_skills = _find_alias_hits(jd, TECH_ALIAS_MAP)
    resume_skills = _find_alias_hits(resume, TECH_ALIAS_MAP)
    strengths = [skill for skill in jd_skills if skill in resume_skills]
    gaps = [skill for skill in jd_skills if skill not in resume_skills]

    tech_focus_topics = _merge_unique(gaps, strengths)
    if not tech_focus_topics:
        tech_focus_topics = _industry_default_topics(industry)

    resume_hr_signals = _find_alias_hits(resume, HR_ALIAS_MAP)
    missing_hr = [topic for topic in DEFAULT_HR_TOPICS if topic not in resume_hr_signals]
    hr_focus_topics = _merge_unique(missing_hr, resume_hr_signals)
    if not hr_focus_topics:
        hr_focus_topics = DEFAULT_HR_TOPICS

    return {
        "jd_skills": jd_skills,
        "resume_skills": resume_skills,
        "strengths": strengths,
        "gaps": gaps,
        "tech_focus_topics": tech_focus_topics,
        "hr_focus_topics": hr_focus_topics,
    }


def pick_next_focus_topic(focus_topics: list[str], asked_topics: list[str]) -> str:
    asked_set = set(asked_topics or [])
    for topic in focus_topics or []:
        if topic not in asked_set:
            return topic
    return ""
