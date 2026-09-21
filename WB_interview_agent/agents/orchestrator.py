from langchain_core.messages import AIMessage

from core.state import InterviewState


def calc_tech_target(max_q: int) -> int:
    if max_q <= 1:
        return 1
    if max_q == 2:
        return 1
    return max(1, min(max_q - 1, round(max_q * 0.67)))


def _last_message_type(messages: list) -> str:
    for msg in reversed(messages):
        msg_type = getattr(msg, "type", "")
        if msg_type:
            return msg_type
    return ""


def _pick_next_question_stage(count: int, max_q: int, tech_count: int, hr_count: int) -> str:
    tech_target = calc_tech_target(max_q)
    if tech_count < tech_target:
        return "ask_tech"
    if hr_count < max_q - tech_target:
        return "ask_hr"
    if count >= max_q:
        return "end"
    return "ask_tech" if tech_count <= hr_count else "ask_hr"


def run_orchestrator(state: InterviewState) -> dict:
    count = state.get("question_count", 0)
    max_q = max(1, state.get("max_questions", 3))
    tech_count = state.get("tech_question_count", 0)
    hr_count = state.get("hr_question_count", 0)
    messages = list(state.get("messages", []))
    awaiting_answer = bool(state.get("awaiting_answer", False))
    last_message_type = _last_message_type(messages)

    if state.get("interview_completed"):
        return {"current_stage": "end", "round_feedback": ""}

    # Step 1: for a fresh interview, ask the candidate to self-introduce first.
    if count == 0 and not messages:
        candidate_name = state.get("candidate_name", "候选人")
        greeting_msg = (
            f"你好，{candidate_name}！我是今天的 AI 面试官。"
            "请先做一个简短的自我介绍，重点说你的技术方向、代表项目和你负责的部分。"
            "我会基于你的介绍继续追问技术细节。"
        )
        return {
            "messages": [AIMessage(content=greeting_msg)],
            "current_stage": "greeting",
            "last_interviewer": "intro",
            "awaiting_answer": True,
            "round_feedback": "",
        }

    # Step 2: candidate just answered the latest prompt.
    if awaiting_answer and last_message_type == "human":
        # The very first answer is self-introduction. Route directly to first tech question.
        if state.get("last_interviewer") == "intro" and count == 0:
            return {"current_stage": "ask_tech", "round_feedback": ""}
        if state.get("last_interviewer") == "hr":
            return {"current_stage": "eval_hr", "round_feedback": ""}
        return {"current_stage": "eval_tech", "round_feedback": ""}

    # Step 3: stop once reaching max rounds.
    if count >= max_q:
        return {"current_stage": "end", "round_feedback": ""}

    # Step 4: after each evaluation, pick next interviewer by routing strategy.
    return {
        "current_stage": _pick_next_question_stage(count, max_q, tech_count, hr_count),
        "round_feedback": "",
    }
