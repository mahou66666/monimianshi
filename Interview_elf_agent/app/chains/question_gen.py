# Interview_elf_agent/app/chains/question_gen.py

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.messages import AIMessage
from pydantic import BaseModel, Field
from typing import List, Any

# --- 新增 RAG 依赖 ---
from qdrant_client import QdrantClient
from qdrant_client.http import models as qdrant_models
from app.utils.model_loader import get_embedding_model

from app.core.config import settings
from app.prompts.interview_coach import build_system_prompt
from app.api.schemas import InterviewResult
from app.utils.message_utils import sanitize_messages
from app.utils.interview_mode import resolve_interview_mode, resolve_min_questions

# 1. 初始化模型
llm = ChatOpenAI(
    base_url=settings.BASE_URL,
    api_key=settings.OPENAI_API_KEY, 
    model=settings.MODEL_NAME, 
    temperature=settings.QUESTION_TEMPERATURE,
    top_p=settings.TOP_P,
    frequency_penalty=settings.FREQUENCY_PENALTY,
    model_kwargs=settings.MODEL_KWARGS
)

structured_llm = llm.with_structured_output(InterviewResult)

# 答案补全专用模型（更低随机性）
answer_llm = ChatOpenAI(
    base_url=settings.BASE_URL,
    api_key=settings.OPENAI_API_KEY,
    model=settings.MODEL_NAME,
    temperature=min(settings.TEMPERATURE, 0.2),
    top_p=settings.TOP_P,
    frequency_penalty=settings.FREQUENCY_PENALTY,
    model_kwargs=settings.MODEL_KWARGS,
)


class AnswerItem(BaseModel):
    answer: str = Field(..., description="参考答案（1-3句）")


class AnswerResult(BaseModel):
    answers: List[AnswerItem]

QUESTION_KEYS = (
    "question",
    "Question",
    "问题",
    "题目",
    "title",
    "Title",
    "text",
    "Text",
    "q",
    "Q",
)

ANSWER_KEYS = (
    "answer",
    "Answer",
    "答案",
    "参考答案",
    "a",
    "A",
)


def _pick_text_from_dict(item: dict, keys: tuple[str, ...]) -> str:
    for key in keys:
        value = item.get(key)
        if value is None:
            continue
        text = str(value).strip()
        if text:
            return text
    return ""


def _extract_question_text(item: Any) -> str:
    if isinstance(item, dict):
        text = _pick_text_from_dict(item, QUESTION_KEYS)
        if text:
            return text
        for key, value in item.items():
            if key in ANSWER_KEYS:
                continue
            if isinstance(value, (str, int, float, bool)):
                raw = str(value).strip()
                if raw:
                    return raw
        return str(item)
    return str(item).strip()


def _extract_answer_text(item: Any) -> str:
    if not isinstance(item, dict):
        return ""
    return _pick_text_from_dict(item, ANSWER_KEYS)


def _normalize_questions(questions: List[Any]) -> List[dict]:
    normalized: List[dict] = []
    for item in questions:
        question_text = _extract_question_text(item) or "（未提供问题）"
        answer_text = _extract_answer_text(item)
        row = {"question": question_text}
        if answer_text:
            row["answer"] = answer_text
        normalized.append(row)
    return normalized


def _build_answer_prompt(
    question_lines: List[str],
    resume_text: str,
    jd_text: str,
) -> ChatPromptTemplate:
    resume_text = resume_text or ""
    jd_text = jd_text or ""
    context_note = (
        "简历已提供，回答应优先依据简历内容。"
        if resume_text.strip()
        else "简历未提供，请仅依据 JD 内容作答，不要编造简历经历。"
    )
    return ChatPromptTemplate.from_messages(
        [
            (
                "system",
                "你是面试题答案补全助手。"
                "简历存在时请仅依据简历内容作答，不要编造经历；"
                "简历缺失时请仅依据 JD 作答，避免虚构简历经历。"
                "若输入中存在乱码/转码异常/OCR 噪声片段，请忽略这些片段，不要据此推断经历。"
                "每道题给出 1-3 句参考答案，语言简洁具体，优先 1 句，必要时 2-3 句。"
                "若信息不足，请以“回答可参考：”开头给出通用参考方向，避免编造经历。",
            ),
            (
                "human",
                f"{context_note}\n\n"
                "【简历内容】\n"
                f"{resume_text or '（未提供简历）'}\n\n"
                "【JD内容】\n"
                f"{jd_text or '（未提供JD）'}\n\n"
                "【问题列表】\n"
                f"{chr(10).join(question_lines)}\n\n"
                "请输出结构化结果，answers 数量必须与问题数一致，顺序一一对应。",
            ),
        ]
    )


def _fill_answers_batch(
    questions: List[dict],
    resume_text: str,
    jd_text: str = "",
) -> AnswerResult:
    question_lines = []
    for idx, item in enumerate(questions, 1):
        q = _extract_question_text(item)
        question_lines.append(f"{idx}. {q}")

    prompt = _build_answer_prompt(question_lines, resume_text, jd_text)
    chain = prompt | answer_llm.with_structured_output(AnswerResult)
    return chain.invoke({})


def _fallback_answer(resume_text: str, jd_text: str) -> str:
    if (resume_text or "").strip():
        return "回答可参考：可补充关键技术选型、实现步骤和量化结果。"
    if (jd_text or "").strip():
        return "回答可参考：可补充关键技术点、评估指标与实现方案。"
    return "回答可参考：可补充关键技术与实现方案。"


def _normalize_answer_text(text: str) -> str:
    if not text:
        return text
    return (
        text.replace("简历未说明", "回答可参考")
        .replace("JD未说明", "回答可参考")
        .replace("简历未提及", "回答可参考")
        .replace("JD未提及", "回答可参考")
    )


def fill_answers_with_resume(
    questions: List[dict],
    resume_text: str,
    jd_text: str = "",
    batch_size: int = 10,
) -> AnswerResult:
    all_answers: List[AnswerItem] = []
    idx = 0
    total = len(questions)
    while idx < total:
        current_size = min(batch_size, total - idx)
        while True:
            batch = questions[idx : idx + current_size]
            try:
                batch_result = _fill_answers_batch(batch, resume_text, jd_text)
                if len(batch_result.answers) != len(batch):
                    raise ValueError(
                        f"answer count mismatch: expected {len(batch)}, got {len(batch_result.answers)}"
                    )
                all_answers.extend(batch_result.answers)
                idx += current_size
                break
            except Exception as e:
                print(
                    f"Answer fill batch error (offset {idx}, size {current_size}): {e}"
                )
                if current_size <= 1:
                    all_answers.extend(
                        [AnswerItem(answer=_fallback_answer(resume_text, jd_text))]
                        * current_size
                    )
                    idx += current_size
                    break
                current_size = max(1, current_size // 2)
    return AnswerResult(answers=all_answers)

_embeddings = None
_qdrant_client = None

def get_embeddings():
    global _embeddings
    if _embeddings is None:
        print("🔄 Loading embedding model...")
        _embeddings = get_embedding_model()
    return _embeddings

def get_qdrant_client():
    global _qdrant_client
    if _qdrant_client is None:
        _qdrant_client = QdrantClient(host=settings.QDRANT_HOST, port=settings.QDRANT_PORT)
    return _qdrant_client


def build_query_filter(metadata: dict | None):
    if not metadata:
        return None
    rag_filter = metadata.get("rag_filter") if isinstance(metadata, dict) else None
    if not isinstance(rag_filter, dict):
        rag_filter = {}
    conditions = []
    for key in ("major", "topic", "subtopic", "source", "type"):
        value = metadata.get(key) if isinstance(metadata, dict) else None
        if value is None:
            value = rag_filter.get(key)
        if not value:
            continue
        if isinstance(value, list):
            cond = qdrant_models.FieldCondition(
                key=key, match=qdrant_models.MatchAny(any=value)
            )
        else:
            cond = qdrant_models.FieldCondition(
                key=key, match=qdrant_models.MatchValue(value=value)
            )
        conditions.append(cond)
    if not conditions:
        return None
    return qdrant_models.Filter(must=conditions)

def get_knowledge_context(query: str, query_filter=None) -> str:
    """执行 RAG 检索"""
    try:
        client = get_qdrant_client()

        # 向量化查询
        query_vec = get_embeddings().embed_query(query)

        top_k = settings.RAG_TOP_K
        score_threshold = settings.RAG_SCORE_THRESHOLD

        # 搜索 TopK（兼容不同版本客户端）
        if hasattr(client, "search"):
            search_kwargs = dict(
                collection_name=settings.COLLECTION_NAME,
                query_vector=query_vec,
                limit=top_k,
                with_payload=True,
            )
            if score_threshold and score_threshold > 0:
                search_kwargs["score_threshold"] = score_threshold
            if query_filter is not None:
                search_kwargs["query_filter"] = query_filter
            results = client.search(**search_kwargs)
        elif hasattr(client, "query_points"):
            query_kwargs = dict(
                collection_name=settings.COLLECTION_NAME,
                query=query_vec,
                limit=top_k,
                with_payload=True,
            )
            if score_threshold and score_threshold > 0:
                query_kwargs["score_threshold"] = score_threshold
            if query_filter is not None:
                query_kwargs["query_filter"] = query_filter
            response = client.query_points(**query_kwargs)
            results = response.points
        else:
            raise RuntimeError("Qdrant client does not support search/query_points.")

        if not results:
            return "暂无相关参考。"

        context = []
        max_chars = settings.RAG_MAX_CONTEXT_CHARS
        for i, point in enumerate(results, 1):
            payload = point.payload or {}
            metadata = payload.get("metadata") or {}
            if not isinstance(metadata, dict):
                metadata = {}
            src = metadata.get("source") or payload.get("source") or "unknown"
            content = payload.get("page_content") or payload.get("text") or ""
            snippet = f"【参考{i} (来源:{src})】: {content}"
            if max_chars > 0 and sum(len(x) for x in context) + len(snippet) > max_chars:
                break
            context.append(snippet)

        return "\n".join(context)
    except Exception as e:
        print(f"RAG Error: {e}")
        return "知识库连接异常，忽略此项。"

def interview_coach_node(state):
    resume_text = state["resume_text"]
    jd_text = state.get("jd_text", "")
    retry_count = state.get("retry_count", 0)
    mode = resolve_interview_mode(state.get("task_type"), jd_text)
    min_questions = resolve_min_questions(state)
    
    # --- RAG 检索 ---
    query_filter = build_query_filter(state.get("metadata"))
    if mode == "resume_only":
        knowledge_context = "本次为简历押题模式，不使用知识库参考。"
    else:
        if mode == "jd_only":
            search_query = jd_text[:200]
        else:
            # 构造查询：取 JD 和 简历 的前段部分作为语义查询
            search_query = f"{jd_text[:200]} {resume_text[:100]}"
        knowledge_context = get_knowledge_context(search_query, query_filter=query_filter)
    
    # --- 构建 Prompt ---
    # 将检索结果注入到 Human Message 中
    prompt_tail = f"请输出结构化的面试预测数据，questions 不少于 {min_questions} 道。"
    if retry_count > 0:
        if mode == "resume_only":
            user_instruction = (
                "上一版面试问题未通过审核，请根据审核意见调整后重新输出。\n\n"
                f"简历内容：\n{resume_text}\n\n"
                "【本次任务：简历押题】仅依靠简历项目经历、实习经历、工作经历与技能描述生成问题。\n\n"
                f"{prompt_tail}"
            )
        elif mode == "jd_only":
            user_instruction = (
                "上一版面试问题未通过审核，请根据审核意见调整后重新输出。\n\n"
                f"目标JD：\n{jd_text or '未提供JD'}\n\n"
                f"【📚 内部知识库参考】：\n{knowledge_context}\n\n"
                "【本次任务：JD押题】仅依靠JD与知识库参考，偏技术与八股文方向生成问题。\n\n"
                f"{prompt_tail}"
            )
        else:
            user_instruction = (
                "上一版面试问题未通过审核，请根据审核意见调整后重新输出。\n\n"
                f"简历内容：\n{resume_text}\n\n"
                f"目标JD：\n{jd_text or '未提供JD'}\n\n"
                f"【📚 内部知识库参考】：\n{knowledge_context}\n\n"
                f"{prompt_tail}"
            )
    else:
        if mode == "resume_only":
            user_instruction = (
                f"简历内容：\n{resume_text}\n\n"
                "【本次任务：简历押题】仅依靠简历项目经历、实习经历、工作经历与技能描述生成问题。\n\n"
                f"{prompt_tail}"
            )
        elif mode == "jd_only":
            user_instruction = (
                f"目标JD：\n{jd_text or '未提供JD'}\n\n"
                f"【📚 内部知识库参考】：\n{knowledge_context}\n\n"
                "【本次任务：JD押题】仅依靠JD与知识库参考，偏技术与八股文方向生成问题。\n\n"
                f"{prompt_tail}"
            )
        else:
            user_instruction = (
                f"简历内容：\n{resume_text}\n\n"
                f"目标JD：\n{jd_text or '未提供JD'}\n\n"
                f"【📚 内部知识库参考】：\n{knowledge_context}\n\n"
                f"{prompt_tail}"
            )

    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", build_system_prompt(state)),
            MessagesPlaceholder(variable_name="messages"),
            ("human", user_instruction),
        ]
    )
    
    chain = prompt | structured_llm
    
    result: InterviewResult = chain.invoke({
        "messages": sanitize_messages(state["messages"]),
        "resume_text": resume_text,
        "jd_text": jd_text
    })
    result.questions = _normalize_questions(result.questions)

    # --- 后处理：批量补全参考答案（基于简历） ---
    try:
        answer_result = fill_answers_with_resume(result.questions, resume_text, jd_text)
        for idx, q in enumerate(result.questions):
            if idx < len(answer_result.answers):
                q["answer"] = _normalize_answer_text(
                    answer_result.answers[idx].answer.strip()
                )
    except Exception as e:
        print(f"Answer fill error: {e}")
    
    # 构造文本回复
    text_content = f"【模拟面试准备】\n差距分析：{result.gap_analysis}\n\n预测问题：\n"
    
    # [修改] 更健壮的字典获取逻辑，防止 KeyError
    for idx, q in enumerate(result.questions):
        question_text = q.get("question") or "（未提供问题）"
        answer_text = q.get("answer") or ""
        text_content += f"{idx+1}. Q: {question_text}\n   A: {answer_text[:50]}...\n"
    
    return {
        "messages": [AIMessage(content=text_content)],
        "evaluation_data": result.model_dump()
    }
