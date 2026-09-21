import re
from typing import Optional, List, Dict, Any, Union

from pydantic import BaseModel, Field, ConfigDict, model_validator


def _strip_text(value: Any) -> Any:
    if isinstance(value, str):
        return value.strip()
    return value


def _has_non_empty_value(value: Any) -> bool:
    if value is None:
        return False
    if isinstance(value, str):
        return bool(value.strip())
    if isinstance(value, dict):
        return any(_has_non_empty_value(v) for v in value.values())
    if isinstance(value, list):
        return any(_has_non_empty_value(v) for v in value)
    return True


def _normalize_text_block_or_list(
    value: Any, text_key: str = "description"
) -> Optional[Union[str, List[Dict[str, Any]]]]:
    if value is None:
        return None

    if isinstance(value, str):
        text = value.strip()
        return text or None

    if isinstance(value, dict):
        return [value]

    if isinstance(value, list):
        has_dict = any(isinstance(item, dict) for item in value)
        text_lines = [item.strip() for item in value if isinstance(item, str) and item.strip()]
        if has_dict:
            rows: List[Dict[str, Any]] = [item for item in value if isinstance(item, dict)]
            rows.extend({text_key: line} for line in text_lines)
            return rows or None
        if text_lines:
            return "\n".join(text_lines)
        return None

    return None


def _split_skill_items(value: str) -> List[str]:
    parts = [p.strip() for p in re.split(r"[，,、/\n]+", value) if p and p.strip()]
    return parts if parts else ([value.strip()] if value.strip() else [])


def _normalize_skills(value: Any) -> Optional[Union[str, List[Dict[str, Any]]]]:
    if value is None:
        return None
    if isinstance(value, str):
        text = value.strip()
        return text or None

    if isinstance(value, dict):
        if "name" in value or "items" in value:
            return _normalize_skills([value])
        rows: List[Dict[str, Any]] = []
        for name, items_value in value.items():
            if isinstance(items_value, str):
                items = _split_skill_items(items_value)
            elif isinstance(items_value, list):
                items = [str(x).strip() for x in items_value if str(x).strip()]
            else:
                items = []
            if items:
                rows.append({"name": str(name).strip() if name else None, "items": items})
        return rows or None

    if isinstance(value, list):
        has_dict = any(isinstance(item, dict) for item in value)
        text_lines = [item.strip() for item in value if isinstance(item, str) and item.strip()]
        if not has_dict:
            return "\n".join(text_lines) if text_lines else None

        rows: List[Dict[str, Any]] = []
        for item in value:
            if isinstance(item, str):
                continue
            if not isinstance(item, dict):
                continue
            name = _strip_text(item.get("name") or item.get("category"))
            raw_items = item.get("items")
            if isinstance(raw_items, str):
                items = _split_skill_items(raw_items)
            elif isinstance(raw_items, list):
                items = [str(x).strip() for x in raw_items if str(x).strip()]
            else:
                items = []
            if name or items:
                rows.append({"name": name, "items": items or None})
        for line in text_lines:
            rows.append({"name": None, "items": [line]})
        return rows or None

    return None


def _normalize_personal_info(value: Any) -> Optional[Dict[str, Any]]:
    if value is None:
        return None
    if isinstance(value, str):
        text = value.strip()
        return {"summary": text} if text else None
    if not isinstance(value, dict):
        return None

    alias_map = {
        "jobIntention": "job_intention",
        "job_target": "job_intention",
        "jobTarget": "job_intention",
    }
    normalized = dict(value)
    for src, dst in alias_map.items():
        if src in normalized and dst not in normalized:
            normalized[dst] = normalized.pop(src)

    cleaned = {k: _strip_text(v) for k, v in normalized.items() if _has_non_empty_value(v)}
    return cleaned or None


class PersonalInfo(BaseModel):
    model_config = ConfigDict(extra="allow")

    name: Optional[str] = Field(None, description="姓名")
    phone: Optional[str] = Field(None, description="电话")
    email: Optional[str] = Field(None, description="邮箱")
    location: Optional[str] = Field(None, description="所在地")
    website: Optional[str] = Field(None, description="个人网站")
    linkedin: Optional[str] = Field(None, description="LinkedIn")
    github: Optional[str] = Field(None, description="GitHub")
    age: Optional[str] = Field(None, description="年龄")
    gender: Optional[str] = Field(None, description="性别")
    university: Optional[str] = Field(None, description="毕业院校")
    degree: Optional[str] = Field(None, description="学历")
    job_intention: Optional[str] = Field(None, description="求职意向")
    summary: Optional[str] = Field(None, description="个人简介")


class EducationItem(BaseModel):
    school: Optional[str] = Field(None, description="学校")
    degree: Optional[str] = Field(None, description="学历/学位")
    major: Optional[str] = Field(None, description="专业")
    start_date: Optional[str] = Field(None, description="开始时间")
    end_date: Optional[str] = Field(None, description="结束时间")
    gpa: Optional[str] = Field(None, description="GPA")
    description: Optional[str] = Field(None, description="补充说明")


class ExperienceItem(BaseModel):
    company: Optional[str] = Field(None, description="公司/组织")
    position: Optional[str] = Field(None, description="岗位")
    location: Optional[str] = Field(None, description="地点")
    start_date: Optional[str] = Field(None, description="开始时间")
    end_date: Optional[str] = Field(None, description="结束时间")
    description: Optional[str] = Field(None, description="职责/描述")
    highlights: Optional[List[str]] = Field(None, description="要点列表")


class ProjectItem(BaseModel):
    name: Optional[str] = Field(None, description="项目名称")
    role: Optional[str] = Field(None, description="角色")
    start_date: Optional[str] = Field(None, description="开始时间")
    end_date: Optional[str] = Field(None, description="结束时间")
    description: Optional[str] = Field(None, description="项目描述")
    tech_stack: Optional[List[str]] = Field(None, description="技术栈")
    highlights: Optional[List[str]] = Field(None, description="要点列表")
    link: Optional[str] = Field(None, description="链接")


class SkillCategory(BaseModel):
    name: Optional[str] = Field(None, description="类别")
    items: Optional[List[str]] = Field(None, description="技能列表")


class AwardItem(BaseModel):
    title: Optional[str] = Field(None, description="奖项/证书")
    issuer: Optional[str] = Field(None, description="颁发机构")
    date: Optional[str] = Field(None, description="时间")
    description: Optional[str] = Field(None, description="说明")


class CampusItem(BaseModel):
    organization: Optional[str] = Field(None, description="组织/社团")
    role: Optional[str] = Field(None, description="角色")
    start_date: Optional[str] = Field(None, description="开始时间")
    end_date: Optional[str] = Field(None, description="结束时间")
    description: Optional[str] = Field(None, description="描述")
    highlights: Optional[List[str]] = Field(None, description="要点列表")


class ResumeInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    resume_text: Optional[str] = Field(None, description="完整简历文本（可选）")
    personal_info: Optional[PersonalInfo] = Field(None, description="个人信息")
    education: Optional[Union[str, List[EducationItem]]] = Field(
        None, description="教育经历（推荐传完整文段字符串）"
    )
    work_history: Optional[Union[str, List[ExperienceItem]]] = Field(
        None, description="工作经历（推荐传完整文段字符串）"
    )
    internship_history: Optional[Union[str, List[ExperienceItem]]] = Field(
        None, description="实习经历（推荐传完整文段字符串）"
    )
    project_experience: Optional[Union[str, List[ProjectItem]]] = Field(
        None, description="项目经历（推荐传完整文段字符串）"
    )
    skills: Optional[Union[str, List[SkillCategory]]] = Field(
        None, description="技能描述（推荐传完整文段字符串）"
    )
    campus_experience: Optional[Union[str, List[CampusItem]]] = Field(
        None, description="校园经历（推荐传完整文段字符串）"
    )
    awards: Optional[Union[str, List[AwardItem]]] = Field(
        None, description="获奖记录（推荐传完整文段字符串）"
    )
    self_evaluation: Optional[str] = Field(None, description="自我评价")

    @model_validator(mode="before")
    @classmethod
    def normalize_legacy_payload(cls, data: Any):
        if not isinstance(data, dict):
            return data

        payload = dict(data)
        legacy_map = {
            "BASIC_INFO": "personal_info",
            "PERSONAL_INFO": "personal_info",
            "EDUCATION": "education",
            "WORK_EXPERIENCE": "work_history",
            "WORK_HISTORY": "work_history",
            "INTERNSHIP_EXPERIENCE": "internship_history",
            "PROJECT_EXPERIENCE": "project_experience",
            "SKILLS": "skills",
            "AWARDS": "awards",
            "CAMPUS_EXPERIENCE": "campus_experience",
            "SELF_EVALUATION": "self_evaluation",
        }
        for legacy_key, canonical_key in legacy_map.items():
            if legacy_key in payload and canonical_key not in payload:
                payload[canonical_key] = payload[legacy_key]
            payload.pop(legacy_key, None)

        for noisy_key in ("rawContent", "RAW_CONTENT", "errorMessage", "ERROR_MESSAGE"):
            payload.pop(noisy_key, None)

        payload["resume_text"] = _strip_text(payload.get("resume_text"))
        payload["personal_info"] = _normalize_personal_info(payload.get("personal_info"))
        payload["education"] = _normalize_text_block_or_list(payload.get("education"), "description")
        payload["work_history"] = _normalize_text_block_or_list(payload.get("work_history"), "description")
        payload["internship_history"] = _normalize_text_block_or_list(
            payload.get("internship_history"), "description"
        )
        payload["project_experience"] = _normalize_text_block_or_list(
            payload.get("project_experience"), "description"
        )
        payload["skills"] = _normalize_skills(payload.get("skills"))
        payload["campus_experience"] = _normalize_text_block_or_list(
            payload.get("campus_experience"), "description"
        )
        payload["awards"] = _normalize_text_block_or_list(payload.get("awards"), "title")
        payload["self_evaluation"] = _strip_text(payload.get("self_evaluation"))

        # 兼容旧字段：若仅提供自我评价，则补到 personal_info.summary
        if payload.get("self_evaluation"):
            info = payload.get("personal_info") or {}
            if isinstance(info, dict) and not _has_non_empty_value(info.get("summary")):
                info["summary"] = payload["self_evaluation"]
                payload["personal_info"] = info

        return payload

    def has_meaningful_content(self) -> bool:
        payload = self.model_dump(exclude_none=True)
        if _has_non_empty_value(payload.get("resume_text")):
            return True
        for key in (
            "personal_info",
            "education",
            "work_history",
            "internship_history",
            "project_experience",
            "skills",
            "campus_experience",
            "awards",
            "self_evaluation",
        ):
            if _has_non_empty_value(payload.get(key)):
                return True
        return False

# --- 1. 请求体定义 ---
class AnalyzeRequest(BaseModel):
    resume_text: Optional[str] = Field(None, description="简历文本内容")
    jd_text: Optional[str] = Field(None, description="岗位JD内容")
    input_data: Optional[ResumeInput] = Field(
        None, description="前/后端传入的结构化 JSON 数据"
    )
    
    # 增强的任务类型定义
    task_type: str = Field(
        "auto", 
        description="任务指令: 'auto'(自动), 'problem_only'(问题分析), 'score_only'(仅打分), 'interview'(简历+JD综合押题), 'interview_resume'(仅简历押题), 'interview_jd'(仅JD押题), 'refine'(润色)"
    )

    question_count: Optional[int] = Field(
        None,
        description="面试题数量（仅对 interview* 生效）。如小于最小题量，将按最小题量执行。",
    )
    
    # [新增] 元数据字段，用于 Spring Boot 传递一些上下文(如 callback_url, request_id 等)
    metadata: Optional[Dict[str, Any]] = None

    @model_validator(mode="after")
    def validate_input(self):
        """校验逻辑：任务模式 + 输入有效性"""
        task_type = (self.task_type or "").strip().lower()
        jd_text = (self.jd_text or "").strip()
        resume_text = (self.resume_text or "").strip()
        has_structured = bool(self.input_data and self.input_data.has_meaningful_content())

        if task_type in {"interview_jd", "interview_jd_only", "jd_only"}:
            if not jd_text:
                raise ValueError("JD 押题模式必须提供 jd_text")
            return self
        if not resume_text and not has_structured:
            raise ValueError("必须提供有效的 resume_text 或 input_data（至少一个非空简历板块）")
        return self

# --- 2. 核心数据结构定义 ---

class Scores(BaseModel):
    education_score: int = Field(..., description="教育经历 (0-100)")
    work_score: int = Field(..., description="工作经历 (0-100)")
    project_score: int = Field(..., description="项目经历 (0-100)")
    skill_score: int = Field(..., description="技能评分 (0-100)")
    award_score: int = Field(..., description="获奖评分 (0-100)")
    job_fit_score: int = Field(..., description="岗位匹配度评分 (0-100)")
    total_score: int = Field(..., description="总分 (0-100)")


class ScoreDimensionBrief(BaseModel):
    dimension: str = Field(..., description="维度名称")
    score: int = Field(..., description="该维度分数 (0-100)")
    deduction_summary: str = Field(..., description="该维度扣分点简介（一句话）")
    strength_summary: str = Field(..., description="该维度优势点简介（一句话）")


class ScoreProblemItem(BaseModel):
    title: Optional[str] = Field(None, description="简短问题标题")
    severity: Optional[str] = Field(None, description="严重程度: 低/中/高")
    tags: Optional[List[str]] = Field(default_factory=list, description="2-5个标签")
    problem: Optional[str] = Field(None, description="问题描述")
    answer: Optional[str] = Field(None, description="简历优化建议（针对该问题如何改写简历）")


class EvaluationResult(BaseModel):
    """简历评估任务的返回结果"""
    scores: Scores
    dimension_analysis: str = Field(..., description="打分理由")
    dimension_briefs: List[ScoreDimensionBrief] = Field(
        default_factory=list, description="6个维度的扣分点与优势点简介"
    )
    problems: List[ScoreProblemItem] = Field(
        default_factory=list, description="结构化问题列表（含标题、严重程度、标签、问题描述、优化建议）"
    )
    advice: str = Field(..., description="修改建议")

class InterviewResult(BaseModel):
    """[新增] 面试模拟任务的返回结果"""
    gap_analysis: str = Field(..., description="能力差距分析")
    questions: List[Dict[str, str]] = Field(..., description="预测问题列表 [{'question': '...', 'answer': '...'}]")

class ProblemItem(BaseModel):
    title: Optional[str] = Field(None, description="简短问题标题")
    severity: Optional[str] = Field(None, description="严重程度: 低/中/高")
    tags: Optional[List[str]] = Field(default_factory=list, description="2-5个标签")
    problem: Optional[str] = Field(None, description="问题描述")
    answer: Optional[str] = Field(None, description="简历优化建议（针对该问题如何改写简历）")


class ResumeHighlight(BaseModel):
    rank: Optional[int] = Field(None, description="亮点优先级，1 为最大亮点")
    highlight: Optional[str] = Field(None, description="简历亮点关键词短语（如：后端项目经验）")


class ProblemAnalysisResult(BaseModel):
    """简历问题分析任务的返回结果"""
    resume_overview: str = Field(..., description="简历现状简介（一句话）")
    problems: List[ProblemItem] = Field(
        ..., description="简历问题列表（含标题、严重程度、标签、问题描述、简历优化建议）"
    )
    optimization_checklist: List[str] = Field(
        default_factory=list, description="简历优化清单（3-5条）"
    )
    highlights: List[ResumeHighlight] = Field(
        default_factory=list, description="简历亮点（3条关键词短语，按主次排序）"
    )
    advice: str = Field(..., description="针对问题的修改建议")

# --- 3. 统一响应体定义 ---

class AnalyzeResponse(BaseModel):
    code: int = Field(200, description="状态码")
    msg: str = Field("success", description="状态信息")
    
    # 使用 Any 类型，因为 data 可能是 EvaluationResult，也可能是 InterviewResult
    # 在 FastAPI 文档中，这会显示为通用的 Object
    data: Optional[Dict[str, Any]] = Field(None, description="业务数据 (EvaluationResult 或 InterviewResult 的字典形式)")
    
    # 调试日志，方便后端排查 Agent 的思考路径
    trace_log: Optional[List[str]] = Field(None, description="Agent 思考链日志")


class TaskSubmitResponse(BaseModel):
    code: int = Field(200, description="状态码")
    msg: str = Field("success", description="状态信息")
    task_id: str = Field(..., description="任务ID")


class TaskStatusResponse(BaseModel):
    code: int = Field(200, description="状态码")
    msg: str = Field("success", description="状态信息")
    task_id: str = Field(..., description="任务ID")
    status: str = Field(..., description="任务状态: PENDING/RUNNING/SUCCESS/FAILED")
    data: Optional[Dict[str, Any]] = Field(None, description="任务结果")
    error: Optional[str] = Field(None, description="错误信息")
    trace_log: Optional[List[str]] = Field(None, description="Agent 思考链日志")


class KBFilter(BaseModel):
    major: Optional[Union[str, List[str]]] = None
    topic: Optional[Union[str, List[str]]] = None
    subtopic: Optional[Union[str, List[str]]] = None
    source: Optional[Union[str, List[str]]] = None
    type: Optional[Union[str, List[str]]] = None


class KBDeleteRequest(BaseModel):
    ids: Optional[List[str]] = None
    filter: Optional[KBFilter] = None


class KBCountResponse(BaseModel):
    count: int


class KBIngestRequest(BaseModel):
    input_path: Optional[str] = None
    source: Optional[str] = "auto"
    mode: Optional[str] = "incremental"
    batch_size: Optional[int] = 64
    chunk_size: Optional[int] = 700
    chunk_overlap: Optional[int] = 100
    recreate: Optional[bool] = False


class KBIngestResponse(BaseModel):
    status: str
    output: str


class KBExportRequest(BaseModel):
    output_path: Optional[str] = None
    filter: Optional[KBFilter] = None
    limit: Optional[int] = 0


class ResumeScoreNormalizeConfig(BaseModel):
    enabled: Optional[bool] = Field(None, description="是否启用简历总分正态化")
    center: Optional[float] = Field(None, description="总分分布中心")
    spread: Optional[float] = Field(None, description="总分分布跨度")
    min_score: Optional[float] = Field(None, description="总分最低值")
    max_score: Optional[float] = Field(None, description="总分最高值")
