from langchain_core.messages import AIMessage, SystemMessage
from core.state import InterviewState
from core.config import reasoning_llm
from core.scoring import (
    build_round_score,
    extract_last_ai_question_before_answer,
    extract_last_human_answer,
    format_round_score,
)
from core.semantic_dedup import is_semantically_duplicate_question
from core.targeting import build_interview_plan, pick_next_focus_topic
from prompts.system_prompts import HR_ASSESSOR_PROMPT

FALLBACK_HR_QUESTIONS = [
    "请分享一次你在高压场景下协调团队并达成目标的经历，重点说明你的行动和结果。",
    "请回顾一次你与关键同事意见冲突的案例，你如何推动达成共识并交付结果？",
    "请描述一次你面对模糊目标时的拆解过程，以及你如何验证最终方案有效。",
]


def _build_hr_question(state: InterviewState, plan: dict, resume_highlights: str, messages: list, focus_topic: str) -> AIMessage:
    targeting_context = (
        "【JD-简历行为面定向画像】\n"
        f"- 目标行为焦点: {', '.join(plan.get('hr_focus_topics', [])[:6]) or '团队协作、抗压、执行力'}\n"
        f"- 本轮聚焦: {focus_topic or '行为证据深挖'}\n"
        "请优先围绕本轮聚焦，追问一个 STAR 可量化的问题。"
    )
    sys_msg = SystemMessage(
        content=(
            HR_ASSESSOR_PROMPT.format(
                resume_highlights=resume_highlights
            )
            + "\n\n"
            + targeting_context
        )
    )

    fallback_question = FALLBACK_HR_QUESTIONS[state.get("hr_question_count", 0) % len(FALLBACK_HR_QUESTIONS)]
    try:
        response = reasoning_llm.invoke([sys_msg] + messages[-4:])
        if is_semantically_duplicate_question(getattr(response, "content", ""), messages):
            return AIMessage(content=fallback_question)
        question_text = getattr(response, "content", "").strip() or fallback_question
        return AIMessage(content=question_text)
    except Exception as e:
        print(f"[WARN] Failed to generate HR question, using fallback: {e}")
        return AIMessage(content=fallback_question)


def run_hr_assessor_question(state: InterviewState) -> dict:
    industry = state.get("target_industry", "通用岗位")
    jd = state.get("job_description", "未提供岗位描述")
    resume_highlights = state.get("resume_highlights", "未提供履历亮点")
    messages = list(state.get("messages", []))
    plan = state.get("interview_plan") or build_interview_plan(jd, resume_highlights, industry)
    asked_topics = list(state.get("asked_hr_topics", []))
    focus_topic = pick_next_focus_topic(plan.get("hr_focus_topics", []), asked_topics)
    response = _build_hr_question(state, plan, resume_highlights, messages, focus_topic)
    result = {
        "messages": [response],
        "question_count": state.get("question_count", 0) + 1,
        "hr_question_count": state.get("hr_question_count", 0) + 1,
        "last_interviewer": "hr",
        "awaiting_answer": True,
        "round_feedback": "",
    }
    if not state.get("interview_plan"):
        result["interview_plan"] = plan
    if focus_topic:
        result["asked_hr_topics"] = [focus_topic]
    return result


def run_hr_assessor_evaluation(state: InterviewState) -> dict:
    resume_highlights = state.get("resume_highlights", "未提供履历亮点")
    messages = list(state.get("messages", []))
    asked_topics = list(state.get("asked_hr_topics", []))
    last_focus_topic = asked_topics[-1] if asked_topics else ""
    last_user_msg = extract_last_human_answer(messages)
    last_question = extract_last_ai_question_before_answer(messages)
    if not last_user_msg:
        return {"awaiting_answer": False, "round_feedback": ""}

    score_record = build_round_score(
        evaluator="hr",
        round_id=state.get("question_count", 0),
        candidate_answer=last_user_msg,
        question_excerpt=last_question,
        context=f"resume={resume_highlights[:300]}; hr_focus={last_focus_topic or '行为证据挖掘'}",
    )
    return {
        "round_scores": [score_record],
        "round_feedback": format_round_score(score_record),
        "awaiting_answer": False,
    }
