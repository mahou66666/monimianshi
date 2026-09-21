from pathlib import Path
from langchain_core.messages import AIMessage, SystemMessage
from langchain_community.vectorstores.faiss import FAISS
from core.state import InterviewState
from core.config import reasoning_llm, embeddings_model
from core.rag_config import build_effective_rag_config
from core.scoring import (
    build_round_score,
    extract_last_ai_question_before_answer,
    extract_last_human_answer,
    format_round_score,
)
from core.semantic_dedup import is_semantically_duplicate_question
from core.targeting import build_interview_plan, pick_next_focus_topic
from prompts.system_prompts import DOMAIN_EXPERT_PROMPT
from prompts.industries import get_industry_prompt

_RETRIEVER_CACHE = {}
FALLBACK_TECH_QUESTIONS = [
    "请结合一个你主导的线上系统，详细说明一次性能瓶颈定位与优化的完整过程。",
    "请说明你在分布式系统中处理数据一致性的方案，并给出一次线上故障复盘。",
    "请描述一次你在高并发场景下进行容量评估、压测和扩容决策的完整实践。",
]


def get_retriever(state: InterviewState):
    """根据会话配置获取 FAISS 向量检索器。"""
    rag_config = build_effective_rag_config(state)
    if not rag_config["rag_enabled"]:
        return None

    vector_db_path = Path(rag_config["rag_index_dir_resolved"])
    index_file = vector_db_path / "index.faiss"
    docstore_file = vector_db_path / "index.pkl"
    if not (index_file.exists() and docstore_file.exists()):
        return None
    if not rag_config["allow_dangerous_faiss_deserialization"]:
        print(
            "[WARN] FAISS dangerous deserialization is disabled; set "
            "allow_dangerous_faiss_deserialization=true to enable local RAG."
        )
        return None

    cache_key = (
        str(vector_db_path),
        int(rag_config["rag_top_k"]),
        bool(rag_config["allow_dangerous_faiss_deserialization"]),
    )
    if cache_key in _RETRIEVER_CACHE:
        return _RETRIEVER_CACHE[cache_key]
    try:
        vs = FAISS.load_local(
            str(vector_db_path),
            embeddings_model,
            allow_dangerous_deserialization=True,
        )
        retriever = vs.as_retriever(search_kwargs={"k": rag_config["rag_top_k"]})
        _RETRIEVER_CACHE[cache_key] = retriever
        return retriever
    except Exception as e:
        print(f"[WARN] Failed to load FAISS retriever, fallback to no-RAG mode: {e}")
        return None


def _build_tech_question(
    state: InterviewState,
    plan: dict,
    focus_topic: str,
    messages: list,
    industry: str,
    jd: str,
) -> AIMessage:
    last_user_msg = extract_last_human_answer(messages)
    rag_config = build_effective_rag_config(state)

    knowledge_context = "暂无额外知识库背景。"
    retriever = get_retriever(state)
    if retriever and last_user_msg:
        try:
            try:
                rag_query = rag_config["rag_query_template"].format(industry=industry, answer=last_user_msg)
            except KeyError:
                rag_query = f"针对行业：{industry}。候选人提及：{last_user_msg}"
            docs = retriever.invoke(rag_query)
            doc_texts = [d.page_content.strip() for d in docs if getattr(d, "page_content", "").strip()]
            if doc_texts:
                knowledge_context = "\n".join(doc_texts)[: rag_config["rag_max_context_chars"]]
        except Exception:
            pass

    industry_guidelines = get_industry_prompt(industry)
    targeting_context = (
        "【JD-简历定向画像】\n"
        f"- JD核心技能: {', '.join(plan.get('jd_skills', [])[:6]) or '未抽取到'}\n"
        f"- 简历已体现: {', '.join(plan.get('resume_skills', [])[:6]) or '未抽取到'}\n"
        f"- 技能差距: {', '.join(plan.get('gaps', [])[:6]) or '暂无明显差距'}\n"
        f"- 本轮聚焦: {focus_topic or '沿候选人回答继续追问'}\n"
        "请优先围绕“本轮聚焦”提一个可验证、可落地的问题。"
    )
    sys_msg = SystemMessage(
        content=(
            DOMAIN_EXPERT_PROMPT.format(
                industry=industry,
                jd=jd,
                industry_guidelines=industry_guidelines,
                knowledge=knowledge_context,
            )
            + "\n\n"
            + targeting_context
        )
    )

    fallback_question = FALLBACK_TECH_QUESTIONS[state.get("tech_question_count", 0) % len(FALLBACK_TECH_QUESTIONS)]
    try:
        response = reasoning_llm.invoke([sys_msg] + messages[-4:])
        if is_semantically_duplicate_question(getattr(response, "content", ""), messages):
            return AIMessage(content=fallback_question)
        question_text = getattr(response, "content", "").strip() or fallback_question
        return AIMessage(content=question_text)
    except Exception as e:
        print(f"[WARN] Failed to generate tech question, using fallback: {e}")
        return AIMessage(content=fallback_question)


def run_domain_expert_question(state: InterviewState) -> dict:
    industry = state.get("target_industry", "通用技术岗位")
    jd = state.get("job_description", "未提供岗位描述")
    resume = state.get("resume_highlights", "未提供履历亮点")
    messages = list(state.get("messages", []))
    plan = state.get("interview_plan") or build_interview_plan(jd, resume, industry)
    asked_topics = list(state.get("asked_tech_topics", []))
    focus_topic = pick_next_focus_topic(plan.get("tech_focus_topics", []), asked_topics)
    response = _build_tech_question(state, plan, focus_topic, messages, industry, jd)
    result = {
        "messages": [response],
        "question_count": state.get("question_count", 0) + 1,
        "tech_question_count": state.get("tech_question_count", 0) + 1,
        "last_interviewer": "tech",
        "awaiting_answer": True,
        "round_feedback": "",
    }
    if not state.get("interview_plan"):
        result["interview_plan"] = plan
    if focus_topic:
        result["asked_tech_topics"] = [focus_topic]
    return result


def run_domain_expert_evaluation(state: InterviewState) -> dict:
    industry = state.get("target_industry", "通用技术岗位")
    jd = state.get("job_description", "未提供岗位描述")
    messages = list(state.get("messages", []))
    asked_topics = list(state.get("asked_tech_topics", []))
    last_focus_topic = asked_topics[-1] if asked_topics else ""
    last_user_msg = extract_last_human_answer(messages)
    last_question = extract_last_ai_question_before_answer(messages)
    if not last_user_msg:
        return {"awaiting_answer": False, "round_feedback": ""}

    score_record = build_round_score(
        evaluator="tech",
        round_id=state.get("question_count", 0),
        candidate_answer=last_user_msg,
        question_excerpt=last_question,
        context=f"industry={industry}; jd={jd[:300]}; focus={last_focus_topic or '通用技术深挖'}",
    )
    return {
        "round_scores": [score_record],
        "round_feedback": format_round_score(score_record),
        "awaiting_answer": False,
    }
