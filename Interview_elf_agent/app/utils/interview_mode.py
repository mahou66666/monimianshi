from typing import Optional, Any

from app.core.config import settings


RESUME_ONLY_TASK_TYPES = {
    "interview_resume",
    "interview_resume_only",
    "resume_only",
}
JD_ONLY_TASK_TYPES = {
    "interview_jd",
    "interview_jd_only",
    "jd_only",
}
INTERVIEW_TASK_TYPES = {"interview", *RESUME_ONLY_TASK_TYPES, *JD_ONLY_TASK_TYPES}


def normalize_task_type(task_type: Optional[str]) -> str:
    return (task_type or "").strip().lower()


def is_interview_task(task_type: Optional[str]) -> bool:
    return normalize_task_type(task_type) in INTERVIEW_TASK_TYPES


def resolve_interview_mode(task_type: Optional[str], jd_text: Optional[str]) -> str:
    normalized = normalize_task_type(task_type)
    if normalized in RESUME_ONLY_TASK_TYPES:
        return "resume_only"
    if normalized in JD_ONLY_TASK_TYPES:
        return "jd_only"
    if normalized == "interview" and not (jd_text or "").strip():
        return "resume_only"
    return "combined"


def resolve_min_questions(state_or_value: Any) -> int:
    raw = None
    if isinstance(state_or_value, dict):
        raw = state_or_value.get("question_count")
    else:
        raw = state_or_value
    try:
        raw_int = int(raw) if raw is not None else None
    except (TypeError, ValueError):
        raw_int = None
    if raw_int and raw_int > 0:
        return max(raw_int, settings.MIN_INTERVIEW_QUESTIONS)
    return settings.MIN_INTERVIEW_QUESTIONS
