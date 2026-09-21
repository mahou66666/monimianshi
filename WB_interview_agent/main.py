import os
import uuid
from dotenv import load_dotenv
from langchain_core.messages import HumanMessage
from core.targeting import build_interview_plan

load_dotenv()


def load_interview_app():
    from core.graph import interview_app

    return interview_app


def stream_and_print(interview_app, update: dict, config: dict) -> tuple[bool, bool]:
    reporter_finished = False
    stream_failed = False
    try:
        for event in interview_app.stream(update, config):
            for key, value in event.items():
                if not isinstance(value, dict):
                    continue
                round_feedback = value.get("round_feedback", "").strip()
                if round_feedback:
                    print(f"\n[{key} 评分]: {round_feedback}")
                messages = value.get("messages", [])
                if messages:
                    print(f"\n[{key} 面试官]: {messages[-1].content}")
                if key == "Reporter":
                    reporter_finished = True
    except Exception as e:
        print(f"\n❌ 流程执行异常：{e}")
        stream_failed = True
    return reporter_finished, stream_failed


def parse_positive_int(raw_value: str, default: int) -> int:
    try:
        return max(1, int(raw_value))
    except (TypeError, ValueError):
        return default


def resolve_session_id() -> tuple[str, bool]:
    raw_candidate_id = os.getenv("CANDIDATE_ID", "").strip()
    if raw_candidate_id:
        return raw_candidate_id, False
    return f"session_{uuid.uuid4().hex[:12]}", True


def _normalize_profile_value(value: str) -> str:
    return " ".join((value or "").strip().split())


def _last_message_type(messages: list) -> str:
    for msg in reversed(messages or []):
        msg_type = getattr(msg, "type", "")
        if msg_type:
            return msg_type
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


def main():
    print("🤖 AI 多智能体面试系统已启动...\n")

    interview_app = load_interview_app()
    session_id, session_generated = resolve_session_id()
    candidate_name = os.getenv("CANDIDATE_NAME", "张三").strip() or "张三"
    target_industry = os.getenv("TARGET_INDUSTRY", "Java后端开发").strip() or "Java后端开发"
    job_description = os.getenv(
        "JOB_DESCRIPTION",
        "负责高并发微服务架构设计，精通 Spring Boot、MySQL、Redis。",
    ).strip() or "负责高并发微服务架构设计，精通 Spring Boot、MySQL、Redis。"
    resume_highlights = os.getenv(
        "RESUME_HIGHLIGHTS",
        "拥有 5 年开发经验，主导过日活百万级电商系统的重构。",
    ).strip() or "拥有 5 年开发经验，主导过日活百万级电商系统的重构。"
    max_questions = parse_positive_int(os.getenv("MAX_QUESTIONS", "3"), 3)
    config = {"configurable": {"thread_id": session_id}}
    expected_profile = {
        "candidate_name": candidate_name,
        "target_industry": target_industry,
        "job_description": job_description,
        "resume_highlights": resume_highlights,
    }

    # 尝试获取历史状态
    current_state = interview_app.get_state(config)
    resume_state_patch = {"session_id": session_id}

    if not current_state.values:
        if session_generated:
            print(f"🆕 未提供 CANDIDATE_ID，已自动生成独立会话 ID: {session_id}")
            print("如需断点续面，请在下次启动前将该值写入 .env 的 CANDIDATE_ID。")
        print("🆕 检测到新候选人，初始化面试状态...")
        interview_plan = build_interview_plan(job_description, resume_highlights, target_industry)
        initial_state = {
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
        _, startup_failed = stream_and_print(interview_app, initial_state, config)
        if startup_failed:
            print("❌ 初始化流程失败，请检查模型配置或网络后重试。")
            return
    else:
        conflict_fields = detect_session_conflicts(current_state.values, expected_profile)
        if conflict_fields:
            print("❌ 检测到当前 CANDIDATE_ID 已绑定不同候选人资料，为避免会话串号已拒绝恢复。")
            print(f"冲突字段: {', '.join(conflict_fields)}")
            print("请更换新的 CANDIDATE_ID 后重试。")
            return
        resume_state_patch = infer_resume_state_patch(current_state.values, session_id)
        if current_state.values.get("interview_completed") or resume_state_patch.get("interview_completed"):
            print(f"ℹ️ 会话 [{session_id}] 已结束。请更换新的 CANDIDATE_ID 开启下一场面试。")
            return
        print(f"🔄 唤醒长期记忆！已恢复候选人 [{current_state.values.get('candidate_name')}] 的进度。")
        print(f"当前会话 ID: {session_id}")
        print(f"当前已提问 {current_state.values.get('question_count')} 轮。")

    # 终端对话循环
    while True:
        try:
            user_input = input("\n[候选人] 请回答: ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n💾 面试已暂停，进度已自动保存至 PostgreSQL 长记忆库。")
            break

        if user_input.lower() in ['quit', 'q', 'exit']:
            print("💾 面试已暂停，进度已自动保存至 PostgreSQL 长记忆库。")
            break
        if not user_input:
            print("⚠️ 输入为空，请继续作答或输入 q 退出。")
            continue
            
        # 传入候选人的回答，驱动图流转
        update_payload = dict(resume_state_patch)
        update_payload["messages"] = [HumanMessage(content=user_input)]
        reporter_finished, stream_failed = stream_and_print(
            interview_app,
            update_payload,
            config,
        )
        resume_state_patch = {"session_id": session_id}
        if stream_failed:
            print("❌ 本轮面试流程失败，已自动终止。")
            break
                    
        # 流程流转到 Reporter，说明面试彻底结束
        if reporter_finished:
            print("\n🎉 面试正式结束。您可以随时启动下一次面试。")
            break

if __name__ == "__main__":
    main()
