# Interview_elf_agent/app/chains/scorer.py

from typing import Any, List
import math

from pydantic import BaseModel, Field
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.messages import AIMessage

from app.core.config import settings
from app.prompts.resume_analyst import build_system_prompt
from app.utils.message_utils import sanitize_messages


_ALLOWED_SEVERITY = {"低", "中", "高"}
_DEFAULT_TAGS = ["简历问题", "可改进"]
_DEFAULT_PROBLEM_ANSWER = "建议：按 STAR 模型补充技术动作、量化结果与业务背景。"
_DIMENSION_FIELDS = [
    ("education_score", "教育经历"),
    ("work_score", "工作/实习经历"),
    ("project_score", "项目经历"),
    ("skill_score", "个人技能"),
    ("award_score", "获奖与证书"),
    ("job_fit_score", "岗位匹配度"),
]
_DIMENSION_ALIAS = {
    "教育": "教育经历",
    "教育经历": "教育经历",
    "education": "教育经历",
    "work": "工作/实习经历",
    "工作": "工作/实习经历",
    "工作经历": "工作/实习经历",
    "工作/实习": "工作/实习经历",
    "工作/实习经历": "工作/实习经历",
    "project": "项目经历",
    "项目": "项目经历",
    "项目经历": "项目经历",
    "skill": "个人技能",
    "skills": "个人技能",
    "技能": "个人技能",
    "个人技能": "个人技能",
    "award": "获奖与证书",
    "awards": "获奖与证书",
    "获奖": "获奖与证书",
    "获奖与证书": "获奖与证书",
    "jobfit": "岗位匹配度",
    "job_fit": "岗位匹配度",
    "job_fit_score": "岗位匹配度",
    "岗位匹配": "岗位匹配度",
    "岗位匹配度": "岗位匹配度",
}


def _get_score_weights() -> dict[str, float]:
    return {
        "education_score": settings.RESUME_WEIGHT_EDUCATION,
        "work_score": settings.RESUME_WEIGHT_WORK,
        "project_score": settings.RESUME_WEIGHT_PROJECT,
        "skill_score": settings.RESUME_WEIGHT_SKILL,
        "award_score": settings.RESUME_WEIGHT_AWARD,
        "job_fit_score": settings.RESUME_WEIGHT_JOB_FIT,
    }


def _compress_text(text: str, max_len: int = 50) -> str:
    clean = " ".join((text or "").split()).strip()
    if len(clean) <= max_len:
        return clean
    return clean[: max_len - 1].rstrip() + "…"


def _ensure_sentence(text: str, fallback: str) -> str:
    normalized = _compress_text(text or "")
    if not normalized:
        normalized = fallback
    if normalized[-1] not in {"。", "！", "？"}:
        normalized += "。"
    return normalized


def _normalize_dimension_name(name: Any) -> str:
    raw = (str(name or "")).strip()
    if not raw:
        return ""
    if raw in _DIMENSION_ALIAS:
        return _DIMENSION_ALIAS[raw]
    key = raw.lower().replace(" ", "").replace("_", "")
    return _DIMENSION_ALIAS.get(key, "")


class ResumeScores(BaseModel):
    education_score: int = Field(..., description="教育经历评分 (0-100)")
    work_score: int = Field(..., description="工作/实习经历评分 (0-100)")
    project_score: int = Field(..., description="项目经历评分 (0-100)")
    skill_score: int = Field(..., description="个人技能评分 (0-100)")
    award_score: int = Field(..., description="获奖与证书评分 (0-100)")
    job_fit_score: int = Field(..., description="岗位匹配度评分 (0-100)")
    total_score: int = Field(..., description="基于权重的加权总分 (0-100)")


class ScoreProblemItem(BaseModel):
    title: str | None = Field(None, description="简短问题标题")
    severity: str | None = Field(None, description="严重程度: 低/中/高")
    tags: List[str] | None = Field(default_factory=list, description="2-5个标签")
    problem: str | None = Field(None, description="问题描述")
    answer: str | None = Field(None, description="简历优化建议（针对该问题如何改写简历）")


class ScoreDimensionBrief(BaseModel):
    dimension: str | None = Field(None, description="维度名称")
    score: int | None = Field(None, description="该维度分数 (0-100)")
    deduction_summary: str | None = Field(None, description="扣分点简介（一句话）")
    strength_summary: str | None = Field(None, description="优势点简介（一句话）")


class ResumeEvaluation(BaseModel):
    dimension_analysis: str = Field(..., description="对6个维度打分的详细理由陈述")
    dimension_briefs: List[ScoreDimensionBrief | dict[str, Any]] = Field(
        default_factory=list, description="6个维度扣分点与优势点简介"
    )
    problems: List[ScoreProblemItem | dict[str, Any] | str] = Field(
        default_factory=list, description="结构化问题列表"
    )
    scores: ResumeScores = Field(..., description="6个维度的具体评分")
    advice: str = Field(..., description="修改建议")


def _compute_weighted_raw_total(scores: ResumeScores) -> float:
    total = 0.0
    weights = _get_score_weights()
    weight_sum = sum(weights.values())
    if weight_sum <= 0:
        return 0.0
    for field, weight in weights.items():
        value = getattr(scores, field, 0)
        total += value * (weight / weight_sum)
    return max(0.0, min(100.0, total))


def _compute_weighted_total(scores: ResumeScores) -> int:
    raw_total = _compute_weighted_raw_total(scores)
    if not settings.RESUME_SCORE_NORMALIZE:
        return int(round(raw_total))

    center = settings.RESUME_SCORE_CENTER
    spread = settings.RESUME_SCORE_SPREAD
    if spread <= 0:
        return int(round(raw_total))
    z = (raw_total - center) / spread
    normalized = center + spread * math.tanh(z)
    normalized = max(settings.RESUME_SCORE_MIN, min(settings.RESUME_SCORE_MAX, normalized))
    return int(round(normalized))


def _default_deduction_summary(score: int) -> str:
    if score < 70:
        return "该维度扣分较多，核心证据与细节支撑不足。"
    if score < 85:
        return "该维度存在一定扣分，建议补充更具体的项目与结果。"
    return "该维度扣分有限，主要可优化表达精度。"


def _default_strength_summary(score: int) -> str:
    if score >= 85:
        return "该维度优势较明显，具备较好的岗位竞争力。"
    if score >= 70:
        return "该维度具备一定基础，可继续强化亮点。"
    return "该维度优势暂不明显，建议先补齐关键信息。"


def _normalize_problem_item(item: ScoreProblemItem | dict[str, Any] | str) -> ScoreProblemItem:
    if isinstance(item, ScoreProblemItem):
        title = (item.title or "").strip()
        severity = (item.severity or "").strip()
        tags = [t.strip() for t in (item.tags or []) if t and t.strip()]
        problem = (item.problem or "").strip()
        answer = (item.answer or "").strip()
    elif isinstance(item, dict):
        title = str(item.get("title") or "").strip()
        severity = str(item.get("severity") or "").strip()
        raw_tags = item.get("tags") or []
        if isinstance(raw_tags, list):
            tags = [str(t).strip() for t in raw_tags if str(t).strip()]
        else:
            tags = [str(raw_tags).strip()] if str(raw_tags).strip() else []
        problem = str(item.get("problem") or "").strip()
        answer = str(item.get("answer") or "").strip()
    else:
        text = str(item or "").strip()
        title = _compress_text(text[:18], 18) if text else "简历问题"
        severity = "中"
        tags = []
        problem = text
        answer = ""

    if severity not in _ALLOWED_SEVERITY:
        severity = "中"
    if not title:
        title = _compress_text(problem or "简历问题", 18)
    if not problem:
        problem = title
    if not tags:
        tags = list(_DEFAULT_TAGS)
    if len(tags) == 1:
        tags.append(_DEFAULT_TAGS[1])
    tags = list(dict.fromkeys(tags))[:5]
    if not answer:
        answer = _DEFAULT_PROBLEM_ANSWER
    elif not answer.startswith(("建议", "可", "应", "将", "补充", "改为", "删除", "突出", "量化", "明确")):
        answer = f"建议：{answer}"
    answer = _ensure_sentence(answer, _DEFAULT_PROBLEM_ANSWER)

    return ScoreProblemItem(
        title=title,
        severity=severity,
        tags=tags,
        problem=problem,
        answer=answer,
    )


def _normalize_dimension_briefs(
    items: List[ScoreDimensionBrief | dict[str, Any]], scores: ResumeScores
) -> List[ScoreDimensionBrief]:
    score_map = {label: int(getattr(scores, field)) for field, label in _DIMENSION_FIELDS}
    normalized: dict[str, ScoreDimensionBrief] = {}

    for item in items or []:
        if isinstance(item, ScoreDimensionBrief):
            raw_dimension = item.dimension
            raw_score = item.score
            raw_deduction = item.deduction_summary
            raw_strength = item.strength_summary
        elif isinstance(item, dict):
            raw_dimension = item.get("dimension") or item.get("name")
            raw_score = item.get("score")
            raw_deduction = item.get("deduction_summary") or item.get("deduction")
            raw_strength = item.get("strength_summary") or item.get("strength")
        else:
            continue

        dimension = _normalize_dimension_name(raw_dimension)
        if not dimension:
            continue
        score = score_map[dimension]
        if raw_score is not None:
            try:
                score = int(raw_score)
            except (TypeError, ValueError):
                pass
        deduction = _ensure_sentence(
            str(raw_deduction or ""), _default_deduction_summary(score)
        )
        strength = _ensure_sentence(
            str(raw_strength or ""), _default_strength_summary(score)
        )
        normalized[dimension] = ScoreDimensionBrief(
            dimension=dimension,
            score=score,
            deduction_summary=deduction,
            strength_summary=strength,
        )

    output: List[ScoreDimensionBrief] = []
    for _, dimension in _DIMENSION_FIELDS:
        score = score_map[dimension]
        brief = normalized.get(dimension)
        if not brief:
            brief = ScoreDimensionBrief(
                dimension=dimension,
                score=score,
                deduction_summary=_default_deduction_summary(score),
                strength_summary=_default_strength_summary(score),
            )
        output.append(brief)
    return output


llm = ChatOpenAI(
    base_url=settings.BASE_URL,
    api_key=settings.OPENAI_API_KEY,
    model=settings.MODEL_NAME,
    temperature=settings.TEMPERATURE,
    top_p=settings.TOP_P,
    frequency_penalty=settings.FREQUENCY_PENALTY,
    model_kwargs=settings.MODEL_KWARGS,
)
structured_llm = llm.with_structured_output(ResumeEvaluation)


def resume_analyst_node(state):
    resume_text = state["resume_text"]
    jd_text = state.get("jd_text", "")
    retry_count = state.get("retry_count", 0)

    if retry_count > 0:
        user_instruction = (
            "【指令更新】\n"
            "你的上一份分析报告（见上方历史记录）未通过审核。审核员已给出了具体的修改意见。\n\n"
            "请执行以下操作：\n"
            "1. 回顾你之前的评分和结构化问题。\n"
            "2. 结合审核员反馈修正不合理部分。\n"
            "3. 重新输出完整、修正后的评分报告。\n\n"
            f"当前简历内容参考：\n{resume_text}\n\n"
            f"目标JD参考：\n{jd_text or '未提供JD'}"
        )
    else:
        user_instruction = (
            f"简历内容：\n{resume_text}\n\n"
            f"目标JD：\n{jd_text or '未提供JD'}\n\n"
            "请严格按照评分标准进行分析。"
        )

    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", build_system_prompt(state)),
            MessagesPlaceholder(variable_name="messages"),
            ("human", user_instruction),
        ]
    )

    chain = prompt | structured_llm
    evaluation: ResumeEvaluation = chain.invoke(
        {"messages": sanitize_messages(state["messages"])}
    )

    raw_total = _compute_weighted_raw_total(evaluation.scores)
    normalized_total = _compute_weighted_total(evaluation.scores)
    evaluation.scores.total_score = normalized_total
    if settings.RESUME_SCORE_NORMALIZE:
        score_note = (
            f"[系统计分] 维度加权原始总分：{raw_total:.1f}；"
            f"正态化后总分：{normalized_total}。"
        )
    else:
        score_note = f"[系统计分] 总分按权重自动计算：{normalized_total}。"
    analysis_text = (evaluation.dimension_analysis or "").strip()
    evaluation.dimension_analysis = f"{analysis_text}\n\n{score_note}".strip()

    normalized_dimension_briefs = _normalize_dimension_briefs(
        evaluation.dimension_briefs, evaluation.scores
    )
    normalized_problems = [_normalize_problem_item(item) for item in (evaluation.problems or [])]
    if not normalized_problems:
        normalized_problems = [
            ScoreProblemItem(
                title="简历描述偏概括",
                severity="中",
                tags=["信息完整性", "表达"],
                problem="多个维度描述偏概括，缺少关键技术动作与量化结果。",
                answer=_DEFAULT_PROBLEM_ANSWER,
            )
        ]
    evaluation.dimension_briefs = normalized_dimension_briefs
    evaluation.problems = normalized_problems

    dimension_lines = []
    for idx, brief in enumerate(normalized_dimension_briefs, 1):
        dimension_lines.append(
            f"{idx}. {brief.dimension}（{brief.score}分）\n"
            f"   扣分点：{brief.deduction_summary}\n"
            f"   优势点：{brief.strength_summary}"
        )

    problem_lines = []
    for idx, item in enumerate(normalized_problems, 1):
        tags_text = "、".join(item.tags or [])
        problem_lines.append(
            f"{idx}. 标题：{item.title}\n"
            f"   严重程度：{item.severity}\n"
            f"   标签：{tags_text}\n"
            f"   问题：{item.problem}\n"
            f"   优化建议：{item.answer}"
        )

    text_content = (
        f"【简历评估报告 (v{retry_count + 1})】\n"
        f"🏆 总分：{evaluation.scores.total_score}/100\n"
        f"1. 教育：{evaluation.scores.education_score}\n"
        f"2. 工作：{evaluation.scores.work_score}\n"
        f"3. 项目：{evaluation.scores.project_score}\n"
        f"4. 技能：{evaluation.scores.skill_score}\n"
        f"5. 获奖：{evaluation.scores.award_score}\n"
        f"6. 岗位匹配度：{evaluation.scores.job_fit_score}\n\n"
        f"【维度扣分与优势简介】\n{chr(10).join(dimension_lines)}\n\n"
        f"【理由】\n{evaluation.dimension_analysis}\n\n"
        f"【结构化问题】\n{chr(10).join(problem_lines)}\n\n"
        f"【建议】\n{evaluation.advice}"
    )

    return {
        "messages": [AIMessage(content=text_content)],
        "evaluation_data": evaluation.model_dump(),
    }
