from dataclasses import dataclass, field
from typing import Any, Callable, Dict, List

from app.utils.interview_mode import resolve_interview_mode, resolve_min_questions


Condition = Callable[[Dict[str, Any]], bool]


@dataclass
class PromptBuilder:
    # 规则式 Prompt 组装器：基础模板 + 条件片段
    base: str
    rules: List[Dict[str, Any]] = field(default_factory=list)

    def when(self, condition: Condition, text: str) -> "PromptBuilder":
        self.rules.append({"condition": condition, "text": text})
        return self

    def build(self, ctx: Dict[str, Any]) -> str:
        # 基于上下文评估规则并拼接最终 Prompt
        parts = [self.base.strip()]
        for rule in self.rules:
            if rule["condition"](ctx):
                parts.append(rule["text"].strip())
        return "\n\n".join(parts).strip() + "\n"


def _missing_sections(input_data: Dict[str, Any]) -> List[str]:
    # 检测结构化简历缺失板块，用于引导更严格的提示
    if not input_data:
        return []
    sections = [
        ("personal_info", "个人信息"),
        ("education", "教育经历"),
        ("work_history", "工作经历"),
        ("internship_history", "实习经历"),
        ("project_experience", "项目经历"),
        ("skills", "个人能力"),
        ("awards", "获奖记录"),
    ]
    missing: List[str] = []
    for key, label in sections:
        value = input_data.get(key)
        if not value:
            missing.append(label)
    return missing


def build_context(state: Dict[str, Any]) -> Dict[str, Any]:
    # 提取与 Prompt 规则相关的状态信息
    input_data = state.get("input_data") or {}
    missing = _missing_sections(input_data) if isinstance(input_data, dict) else []
    task_type = state.get("task_type")
    jd_text = state.get("jd_text")
    return {
        "task_type": task_type,
        "has_jd": bool(jd_text),
        "has_structured": bool(input_data),
        "missing_sections": missing,
        "interview_mode": resolve_interview_mode(task_type, jd_text),
        "min_questions": resolve_min_questions(state),
    }
