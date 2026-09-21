# Interview_elf_agent/app/chains/problem_analysis.py

import re
from typing import List
from pydantic import BaseModel, Field
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.messages import AIMessage

from app.core.config import settings
from app.prompts.problem_analysis import build_system_prompt
from app.utils.message_utils import sanitize_messages


_ALLOWED_SEVERITY = {"低", "中", "高"}
_DEFAULT_TAGS = ["简历问题", "可改进"]
_DEFAULT_OPTIMIZATION_SUGGESTION = (
    "建议：补充具体技术动作、量化成果与业务背景，改写为可验证、客观的简历表述。"
)
_INTERVIEW_STYLE_PREFIXES = (
    "面试官",
    "您好",
    "你好",
    "我目前",
    "我在",
    "我负责",
    "我参与",
    "我理解",
    "我会",
    "我认为",
)
_ACTION_STYLE_PREFIXES = (
    "建议",
    "可",
    "应",
    "将",
    "把",
    "补充",
    "改为",
    "增加",
    "删除",
    "合并",
    "突出",
    "量化",
    "明确",
)
_DEFAULT_CHECKLIST_ITEMS = [
    "补充项目成果的量化指标（性能/效率/业务结果）",
    "统一经历时间格式，并明确项目/实习背景",
    "将技术描述改为“场景-动作-结果”的表述方式",
]
_DEFAULT_HIGHLIGHT_ITEMS = [
    "后端项目经验",
    "技术栈覆盖",
    "业务实践经验",
]
_DEFAULT_RESUME_OVERVIEW = "简历主体信息已提供，但关键经历仍需补充可验证细节与量化成果。"
_HIGHLIGHT_KEYWORD_MAP = [
    (("分布式", "微服务", "事务", "延迟队列", "消息队列", "后端", "系统设计"), "后端项目经验"),
    (("Python", "Java", "Spring", "Redis", "MySQL", "SQL", "NLP", "机器学习", "算法"), "技术栈覆盖"),
    (("实习", "工作", "公司", "业务", "落地"), "业务实践经验"),
    (("获奖", "竞赛", "证书", "一等奖", "ACM"), "竞赛获奖经历"),
    (("团队", "协作", "沟通", "组长", "负责人"), "团队协作能力"),
    (("学习", "成长", "自学", "快速"), "学习迭代能力"),
]


def _dedupe(items: List[str]) -> List[str]:
    seen = set()
    output: List[str] = []
    for item in items:
        if item in seen:
            continue
        seen.add(item)
        output.append(item)
    return output


class ProblemItem(BaseModel):
    title: str | None = Field(None, description="简短问题标题")
    severity: str | None = Field(None, description="严重程度: 低/中/高")
    tags: List[str] | None = Field(default_factory=list, description="2-5个标签")
    problem: str | None = Field(None, description="问题描述")
    answer: str | None = Field(None, description="简历优化建议（针对该问题如何改写简历）")


class ResumeHighlight(BaseModel):
    rank: int | None = Field(None, description="亮点优先级，1 为最大亮点")
    highlight: str | None = Field(None, description="简历亮点关键词（简短词组）")


class ProblemAnalysisResult(BaseModel):
    resume_overview: str | None = Field(
        "", description="简历现状简介（一句话）"
    )
    problems: List[ProblemItem] = Field(
        ..., description="问题列表（含标题、严重程度、标签、问题描述、简历优化建议）"
    )
    optimization_checklist: List[str] = Field(
        default_factory=list,
        description="简历优化清单（3-5条，简洁直观）",
    )
    highlights: List[ResumeHighlight] = Field(
        default_factory=list,
        description="简历亮点（固定3条，按主次排序，rank=1 为最大亮点）",
    )
    advice: str = Field(..., description="针对问题的修改建议")


def _normalize_resume_overview(
    overview: str, problems: List[ProblemItem], resume_text: str
) -> str:
    raw = " ".join((overview or "").replace("\n", " ").split()).strip()
    if not raw:
        high_count = sum(1 for p in problems if (p.severity or "").strip() == "高")
        if high_count >= 2:
            raw = "简历当前存在多项高优先级问题，建议先补齐关键经历细节与量化成果。"
        elif problems:
            raw = _DEFAULT_RESUME_OVERVIEW
        elif (resume_text or "").strip():
            raw = "简历整体较完整，建议进一步强化与目标岗位相关的量化表达。"
        else:
            raw = _DEFAULT_RESUME_OVERVIEW

    first_sentence = re.split(r"[。！？!?；;]", raw, maxsplit=1)[0].strip()
    normalized = first_sentence or raw
    normalized = normalized.strip("，,、;； ")
    if len(normalized) > 48:
        normalized = normalized[:47].rstrip("，,、 ") + "…"
    if not normalized:
        normalized = _DEFAULT_RESUME_OVERVIEW
    if normalized[-1] not in {"。", "！", "？"}:
        normalized += "。"
    return normalized


def _normalize_optimization_answer(answer: str, title: str, problem: str) -> str:
    text = (answer or "").strip()
    if not text:
        return _DEFAULT_OPTIMIZATION_SUGGESTION

    for prefix in ("面试官您好", "面试官你好", "您好", "你好"):
        if text.startswith(prefix):
            text = text[len(prefix) :].lstrip("，,。:： ")

    interview_style = any(text.startswith(prefix) for prefix in _INTERVIEW_STYLE_PREFIXES)
    interview_style = interview_style or ("面试官" in text[:12])
    if interview_style:
        focus = title or problem or "该问题"
        return (
            f"建议：围绕“{focus}”改写简历，补充技术动作、量化结果和业务影响，"
            "使用客观第三人称表述，避免口语化答辩。"
        )

    if not text.startswith(_ACTION_STYLE_PREFIXES):
        text = f"建议：{text}"
    return text


def _compress_text(text: str, max_len: int) -> str:
    clean = " ".join((text or "").split()).strip()
    if len(clean) <= max_len:
        return clean
    return clean[: max_len - 1].rstrip() + "…"


def _normalize_optimization_checklist(items: List[str], problems: List[ProblemItem]) -> List[str]:
    normalized: List[str] = []
    for item in items or []:
        text = _compress_text(str(item).strip(), 38)
        if text and text not in normalized:
            normalized.append(text)

    if len(normalized) < 3:
        for problem_item in problems:
            title = (problem_item.title or "").strip()
            if not title:
                continue
            candidate = _compress_text(f"补充“{title}”对应的关键证据与量化结果", 38)
            if candidate not in normalized:
                normalized.append(candidate)
            if len(normalized) >= 5:
                break

    if len(normalized) < 3:
        for fallback in _DEFAULT_CHECKLIST_ITEMS:
            if fallback not in normalized:
                normalized.append(fallback)
            if len(normalized) >= 3:
                break

    if len(normalized) > 5:
        normalized = normalized[:5]
    return normalized


def _normalize_highlight_keyword(text: str) -> str:
    clean = " ".join((text or "").split()).strip()
    if not clean:
        return ""

    for keys, label in _HIGHLIGHT_KEYWORD_MAP:
        if any(key in clean for key in keys):
            return label

    for prefix in ("具备", "拥有", "有", "在", "可"):
        if clean.startswith(prefix):
            clean = clean[len(prefix) :].strip("：:，,。 ")

    for suffix in ("方面", "能力", "经验", "背景"):
        pos = clean.find(suffix)
        if pos > 0 and pos <= 8:
            return clean[: pos + len(suffix)]

    clean = (
        clean.replace("，", " ")
        .replace("。", " ")
        .replace("；", " ")
        .replace("、", " ")
        .split(" ")[0]
    )
    return clean[:8]


def _fallback_highlights(resume_text: str) -> List[str]:
    text = resume_text or ""
    candidates: List[str] = []
    if any(k in text for k in ("项目", "系统", "平台", "开发", "落地")):
        candidates.append("后端项目经验")
    if any(k in text for k in ("实习", "工作", "公司", "业务")):
        candidates.append("业务实践经验")
    if any(
        k in text
        for k in ("Python", "Java", "Spring", "Redis", "MySQL", "SQL", "NLP", "机器学习", "算法")
    ):
        candidates.append("技术栈覆盖")
    if any(k in text for k in ("获奖", "竞赛", "证书", "一等奖", "ACM")):
        candidates.append("竞赛获奖经历")
    if any(k in text for k in ("团队", "协作", "沟通", "组长", "负责人")):
        candidates.append("团队协作能力")

    for fallback in _DEFAULT_HIGHLIGHT_ITEMS:
        if fallback not in candidates:
            candidates.append(fallback)
    return candidates


def _normalize_highlights(
    items: List[ResumeHighlight], resume_text: str, problems: List[ProblemItem]
) -> List[ResumeHighlight]:
    parsed: List[tuple[int, str]] = []
    for idx, item in enumerate(items or [], 1):
        if isinstance(item, dict):
            rank = item.get("rank")
            text = item.get("highlight")
        else:
            rank = getattr(item, "rank", None)
            text = getattr(item, "highlight", None)
        text_value = _normalize_highlight_keyword(str(text or ""))
        if not text_value:
            continue
        try:
            rank_value = int(rank) if rank is not None else idx
        except (TypeError, ValueError):
            rank_value = idx
        if rank_value <= 0:
            rank_value = idx
        parsed.append((rank_value, text_value))

    parsed.sort(key=lambda x: x[0])
    deduped: List[str] = []
    for _, text in parsed:
        if text not in deduped:
            deduped.append(text)

    if len(deduped) < 3:
        fallback = _fallback_highlights(resume_text)
        # 若问题过多，优先强调“可优化空间大”作为次级亮点提示
        if len(problems) >= 5 and "优化潜力明确" not in fallback:
            fallback.append("优化潜力明确")
        for text in fallback:
            text = _normalize_highlight_keyword(text)
            if text not in deduped:
                deduped.append(text)
            if len(deduped) >= 3:
                break

    deduped = deduped[:3]
    return [ResumeHighlight(rank=i + 1, highlight=text) for i, text in enumerate(deduped)]


def _normalize_problem_item(item: ProblemItem) -> ProblemItem:
    title = (item.title or "").strip()
    severity = (item.severity or "").strip()
    tags = [t.strip() for t in (item.tags or []) if t and t.strip()]
    problem = (item.problem or "").strip()
    answer = (item.answer or "").strip()

    if not title:
        if problem:
            title = problem[:12] + ("..." if len(problem) > 12 else "")
        else:
            title = "简历问题"

    if severity not in _ALLOWED_SEVERITY:
        severity = "中"

    tags = _dedupe(tags)
    if len(tags) < 2:
        tags = _dedupe(tags + _DEFAULT_TAGS)[:2]
    if len(tags) > 5:
        tags = tags[:5]

    if not problem:
        problem = title

    answer = _normalize_optimization_answer(answer, title, problem)

    return ProblemItem(
        title=title,
        severity=severity,
        tags=tags,
        problem=problem,
        answer=answer,
    )


llm = ChatOpenAI(
    base_url=settings.BASE_URL,
    api_key=settings.OPENAI_API_KEY,
    model=settings.MODEL_NAME,
    temperature=settings.TEMPERATURE,
    top_p=settings.TOP_P,
    frequency_penalty=settings.FREQUENCY_PENALTY,
    model_kwargs=settings.MODEL_KWARGS,
)
structured_llm = llm.with_structured_output(ProblemAnalysisResult)


def problem_analysis_node(state):
    resume_text = state["resume_text"]
    jd_text = state.get("jd_text", "")
    retry_count = state.get("retry_count", 0)

    if retry_count > 0:
        user_instruction = (
            "你的上一份问题分析未通过审核，请根据审核意见修正并重新输出。\n"
            f"简历内容：\n{resume_text}\n\n"
            f"目标JD：\n{jd_text}\n"
        )
    else:
        user_instruction = (
            f"简历内容：\n{resume_text}\n\n"
            f"目标JD：\n{jd_text}\n\n"
            "请输出问题分析结果。"
        )

    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", build_system_prompt(state)),
            MessagesPlaceholder(variable_name="messages"),
            ("human", user_instruction),
        ]
    )

    chain = prompt | structured_llm
    result: ProblemAnalysisResult = chain.invoke(
        {"messages": sanitize_messages(state["messages"])}
    )

    normalized = [_normalize_problem_item(item) for item in result.problems]
    normalized_resume_overview = _normalize_resume_overview(
        result.resume_overview, normalized, resume_text
    )
    normalized_checklist = _normalize_optimization_checklist(
        result.optimization_checklist, normalized
    )
    normalized_highlights = _normalize_highlights(
        result.highlights, resume_text, normalized
    )
    lines = []
    for item in normalized:
        tags_text = "、".join(item.tags or [])
        lines.append(
            "- 标题: {title}\n"
            "  严重程度: {severity}\n"
            "  标签: {tags}\n"
            "  问题: {problem}\n"
            "  简历优化建议: {answer}".format(
                title=item.title,
                severity=item.severity,
                tags=tags_text,
                problem=item.problem,
                answer=item.answer or "",
            )
        )

    checklist_lines = [
        f"{idx + 1}. {item}" for idx, item in enumerate(normalized_checklist)
    ]
    highlight_lines = []
    for idx, item in enumerate(normalized_highlights):
        tag = "（核心亮点）" if idx == 0 else ""
        highlight_lines.append(f"{idx + 1}. {item.highlight}{tag}")

    text_content = f"【简历现状简介】{normalized_resume_overview}\n\n"
    text_content += "【简历问题分析】\n" + "\n".join(lines)
    text_content += "\n\n【简历优化清单】\n" + "\n".join(checklist_lines)
    text_content += "\n\n【简历亮点（按主次）】\n" + "\n".join(highlight_lines)
    text_content += f"\n\n【修改建议】\n{result.advice}"

    return {
        "messages": [AIMessage(content=text_content)],
        "evaluation_data": ProblemAnalysisResult(
            resume_overview=normalized_resume_overview,
            problems=normalized,
            optimization_checklist=normalized_checklist,
            highlights=normalized_highlights,
            advice=result.advice,
        ).model_dump(),
    }
