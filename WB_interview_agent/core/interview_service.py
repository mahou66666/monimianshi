import uuid
from typing import Any, Optional

from langchain_core.messages import HumanMessage

from core.rag_config import build_effective_rag_config, extract_rag_overrides
from core.scoring import build_score_summary, render_structured_report
from core.targeting import build_interview_plan


class InterviewServiceError(RuntimeError):
    def __init__(self, message: str, status_code: int = 400):
        super().__init__(message)
        self.status_code = status_code


def load_interview_app():
    from core.graph import interview_app

    return interview_app


def parse_positive_int(raw_value: Any, default: int) -> int:
    try:
        return max(1, int(raw_value))
    except (TypeError, ValueError):
        return default


def resolve_api_session_id(session_id: Optional[str]) -> tuple[str, bool]:
    normalized = (session_id or "").strip()
    if normalized:
        return normalized, False
    return f"session_{uuid.uuid4().hex[:12]}", True


def _normalize_profile_value(value: str) -> str:
    return " ".join((value or "").strip().split())


def _last_message_type(messages: list) -> str:
    for msg in reversed(messages or []):
        msg_type = getattr(msg, "type", "")
        if msg_type:
            return msg_type
    return ""


def _last_ai_message_content(messages: list) -> str:
    for msg in reversed(messages or []):
        if getattr(msg, "type", "") == "ai":
            return getattr(msg, "content", "").strip()
    return ""


def _infer_last_interviewer(state_values: dict) -> str:
    stage = str(state_values.get("current_stage", "")).strip()
    if stage in {"hr_eval", "ask_hr", "eval_hr"}:
        return "hr"
    if stage in {"tech_eval", "ask_tech", "eval_tech"}:
        return "tech"
    tech_count = int(state_values.get("tech_question_count", 0) or 0)
    hr_count = int(state_values.get("hr_question_count", 0) or 0)
    return "tech" if tech_count >= hr_count else "hr"


def infer_resume_state_patch(state_values: dict, session_id: str) -> dict:
    messages = list(state_values.get("messages", []))
    last_message_type = _last_message_type(messages)
    last_message_content = getattr(messages[-1], "content", "") if messages else ""
    interview_completed = bool(state_values.get("interview_completed"))
    if not interview_completed and "评估报告预览" in last_message_content:
        interview_completed = True
    if not interview_completed and state_values.get("current_stage") == "end" and last_message_type == "ai":
        interview_completed = True

    patch = {
        "session_id": session_id,
        "round_feedback": "",
    }
    if "interview_completed" not in state_values:
        patch["interview_completed"] = interview_completed
    if not interview_completed:
        if "awaiting_answer" not in state_values:
            patch["awaiting_answer"] = last_message_type == "ai"
        if "last_interviewer" not in state_values:
            patch["last_interviewer"] = _infer_last_interviewer(state_values)
    return patch


def detect_session_conflicts(state_values: dict, expected_profile: dict) -> list[str]:
    conflict_fields = []
    for field in ["candidate_name", "target_industry", "job_description", "resume_highlights"]:
        stored_value = _normalize_profile_value(str(state_values.get(field, "")))
        expected_value = _normalize_profile_value(str(expected_profile.get(field, "")))
        if stored_value and expected_value and stored_value != expected_value:
            conflict_fields.append(field)
    return conflict_fields


def _build_config(session_id: str) -> dict:
    return {"configurable": {"thread_id": session_id}}


def _build_expected_profile(payload: dict) -> dict:
    return {
        "candidate_name": (payload.get("candidate_name") or "").strip(),
        "target_industry": (payload.get("target_industry") or "").strip(),
        "job_description": (payload.get("job_description") or "").strip(),
        "resume_highlights": (payload.get("resume_highlights") or "").strip(),
    }


def _extract_rag_overrides(payload: dict) -> dict:
    try:
        return extract_rag_overrides(payload)
    except ValueError as exc:
        raise InterviewServiceError(str(exc), status_code=400) from exc


def _try_update_state_patch(interview_app, config: dict, patch: dict) -> None:
    if not patch:
        return
    update_state_fn = getattr(interview_app, "update_state", None)
    if not callable(update_state_fn):
        return
    try:
        update_state_fn(config, patch)
    except Exception as exc:
        raise InterviewServiceError(f"更新会话配置失败: {exc}", status_code=500) from exc


def _build_initial_state(payload: dict, session_id: str, rag_overrides: Optional[dict] = None) -> dict:
    candidate_name = (payload.get("candidate_name") or "").strip() or "候选人"
    target_industry = (payload.get("target_industry") or "").strip() or "通用岗位"
    job_description = (payload.get("job_description") or "").strip() or "未提供岗位描述"
    resume_highlights = (payload.get("resume_highlights") or "").strip() or "未提供履历亮点"
    max_questions = parse_positive_int(payload.get("max_questions"), 3)
    interview_plan = build_interview_plan(job_description, resume_highlights, target_industry)
    state = {
        "session_id": session_id,
        "candidate_name": candidate_name,
        "target_industry": target_industry,
        "job_description": job_description,
        "resume_highlights": resume_highlights,
        "messages": [],
        "current_stage": "greeting",
        "question_count": 0,
        "max_questions": max_questions,
        "tech_question_count": 0,
        "hr_question_count": 0,
        "last_interviewer": "",
        "awaiting_answer": False,
        "interview_completed": False,
        "round_feedback": "",
        "round_scores": [],
        "interview_plan": interview_plan,
        "asked_tech_topics": [],
        "asked_hr_topics": [],
    }
    if rag_overrides:
        state.update(rag_overrides)
    return state


def _collect_stream_events(interview_app, update: dict, config: dict) -> dict:
    event_log = []
    reporter_finished = False
    try:
        for event in interview_app.stream(update, config):
            for key, value in event.items():
                if not isinstance(value, dict):
                    continue
                round_feedback = value.get("round_feedback", "").strip()
                if round_feedback:
                    event_log.append(
                        {
                            "node": key,
                            "kind": "round_feedback",
                            "content": round_feedback,
                        }
                    )
                messages = value.get("messages", [])
                if messages:
                    last_message = messages[-1]
                    event_log.append(
                        {
                            "node": key,
                            "kind": "message",
                            "role": getattr(last_message, "type", "ai"),
                            "content": getattr(last_message, "content", ""),
                        }
                    )
                if key == "Reporter":
                    reporter_finished = True
    except Exception as exc:
        raise InterviewServiceError(f"流程执行异常: {exc}", status_code=500) from exc
    return {
        "event_log": event_log,
        "reporter_finished": reporter_finished,
    }


def _last_event_content(event_log: list[dict], kind: str) -> str:
    for item in reversed(event_log):
        if item.get("kind") == kind:
            return str(item.get("content", "")).strip()
    return ""


def _build_response(
    *,
    session_id: str,
    state_values: dict,
    event_log: Optional[list[dict]] = None,
    session_created: bool = False,
    latest_round_score: Optional[dict] = None,
) -> dict:
    event_log = event_log or []
    messages = list(state_values.get("messages", []))
    round_scores = list(state_values.get("round_scores", []))
    candidate_name = state_values.get("candidate_name", "")
    interview_completed = bool(state_values.get("interview_completed", False))
    reply = _last_event_content(event_log, "message") or _last_ai_message_content(messages)
    round_feedback = _last_event_content(event_log, "round_feedback")
    score_summary = build_score_summary(round_scores) if round_scores else None
    final_report = render_structured_report(candidate_name, score_summary) if interview_completed and score_summary else None
    if latest_round_score is None and round_scores:
        latest_round_score = round_scores[-1]
    rag_config = build_effective_rag_config(state_values)

    return {
        "session_id": session_id,
        "session_created": session_created,
        "status": "completed" if interview_completed else "in_progress",
        "candidate_name": candidate_name,
        "target_industry": state_values.get("target_industry", ""),
        "question_count": int(state_values.get("question_count", 0) or 0),
        "max_questions": int(state_values.get("max_questions", 0) or 0),
        "last_interviewer": state_values.get("last_interviewer", ""),
        "awaiting_answer": bool(state_values.get("awaiting_answer", False)),
        "interview_completed": interview_completed,
        "reply_type": "report" if interview_completed else "question",
        "reply": reply,
        "round_feedback": round_feedback,
        "latest_round_score": latest_round_score,
        "score_summary": score_summary,
        "final_report": final_report,
        "event_log": event_log,
        "rag_config": rag_config,
    }


def get_interview_session(payload: dict) -> dict:
    session_id = (payload.get("session_id") or "").strip()
    if not session_id:
        raise InterviewServiceError("缺少 session_id。", status_code=400)

    interview_app = load_interview_app()
    config = _build_config(session_id)
    current_state = interview_app.get_state(config)
    if not current_state.values:
        raise InterviewServiceError("会话不存在。", status_code=404)

    expected_profile = _build_expected_profile(payload)
    if any(expected_profile.values()):
        conflict_fields = detect_session_conflicts(current_state.values, expected_profile)
        if conflict_fields:
            raise InterviewServiceError(
                f"session_id 已绑定不同候选人资料，冲突字段: {', '.join(conflict_fields)}",
                status_code=409,
            )

    rag_overrides = _extract_rag_overrides(payload)
    if rag_overrides:
        _try_update_state_patch(interview_app, config, rag_overrides)

    state_values = dict(current_state.values)
    if rag_overrides:
        state_values.update(rag_overrides)
    state_values.update(infer_resume_state_patch(state_values, session_id))
    return _build_response(session_id=session_id, state_values=state_values)


def start_interview_session(payload: dict) -> dict:
    session_id, session_created = resolve_api_session_id(payload.get("session_id"))
    interview_app = load_interview_app()
    config = _build_config(session_id)
    current_state = interview_app.get_state(config)
    rag_overrides = _extract_rag_overrides(payload)

    if current_state.values:
        expected_profile = _build_expected_profile(payload)
        conflict_fields = detect_session_conflicts(current_state.values, expected_profile)
        if conflict_fields:
            raise InterviewServiceError(
                f"session_id 已绑定不同候选人资料，冲突字段: {', '.join(conflict_fields)}",
                status_code=409,
            )
        if rag_overrides:
            _try_update_state_patch(interview_app, config, rag_overrides)

        state_values = dict(current_state.values)
        if rag_overrides:
            state_values.update(rag_overrides)
        state_values.update(infer_resume_state_patch(state_values, session_id))
        return _build_response(
            session_id=session_id,
            state_values=state_values,
            session_created=False,
        )

    initial_state = _build_initial_state(payload, session_id, rag_overrides=rag_overrides)
    stream_result = _collect_stream_events(interview_app, initial_state, config)
    state_values = dict(interview_app.get_state(config).values or initial_state)
    return _build_response(
        session_id=session_id,
        state_values=state_values,
        event_log=stream_result["event_log"],
        session_created=True,
    )


def submit_interview_answer(payload: dict) -> dict:
    session_id = (payload.get("session_id") or "").strip()
    answer = (payload.get("answer") or "").strip()
    if not session_id:
        raise InterviewServiceError("缺少 session_id。", status_code=400)
    if not answer:
        raise InterviewServiceError("answer 不能为空。", status_code=400)
    rag_overrides = _extract_rag_overrides(payload)

    interview_app = load_interview_app()
    config = _build_config(session_id)
    current_state = interview_app.get_state(config)
    if not current_state.values:
        raise InterviewServiceError("会话不存在，请先调用 /api/interviews/start。", status_code=404)

    expected_profile = _build_expected_profile(payload)
    if any(expected_profile.values()):
        conflict_fields = detect_session_conflicts(current_state.values, expected_profile)
        if conflict_fields:
            raise InterviewServiceError(
                f"session_id 已绑定不同候选人资料，冲突字段: {', '.join(conflict_fields)}",
                status_code=409,
            )

    resume_state_patch = infer_resume_state_patch(current_state.values, session_id)
    if current_state.values.get("interview_completed") or resume_state_patch.get("interview_completed"):
        raise InterviewServiceError("当前会话已结束，请创建新的 session_id。", status_code=409)

    waiting_for_answer = bool(current_state.values.get("awaiting_answer", False) or resume_state_patch.get("awaiting_answer", False))
    if not waiting_for_answer:
        raise InterviewServiceError("当前会话不处于等待候选人回答状态。", status_code=409)

    previous_round_scores = list(current_state.values.get("round_scores", []))
    update_payload = dict(resume_state_patch)
    if rag_overrides:
        update_payload.update(rag_overrides)
    update_payload["messages"] = [HumanMessage(content=answer)]
    stream_result = _collect_stream_events(interview_app, update_payload, config)
    state_values = dict(interview_app.get_state(config).values or {})
    round_scores = list(state_values.get("round_scores", []))
    latest_round_score = round_scores[-1] if len(round_scores) > len(previous_round_scores) else None
    return _build_response(
        session_id=session_id,
        state_values=state_values,
        event_log=stream_result["event_log"],
        latest_round_score=latest_round_score,
    )
