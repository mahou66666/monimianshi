# Interview_elf_agent/app/chains/interview_critique.py

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.messages import HumanMessage

from app.core.config import settings
from app.prompts.interview_critique import build_system_prompt
from app.utils.message_utils import sanitize_messages
from app.utils.interview_mode import resolve_min_questions


llm = ChatOpenAI(
    base_url=settings.BASE_URL,
    api_key=settings.OPENAI_API_KEY,
    model=settings.MODEL_NAME,
    temperature=settings.TEMPERATURE,
    top_p=settings.TOP_P,
    frequency_penalty=settings.FREQUENCY_PENALTY,
    model_kwargs=settings.MODEL_KWARGS,
)


def interview_critique_node(state):
    min_questions = resolve_min_questions(state)
    evaluation_data = state.get("evaluation_data") or {}
    questions = evaluation_data.get("questions") if isinstance(evaluation_data, dict) else None
    if isinstance(questions, list) and len(questions) < min_questions:
        new_retry_count = state.get("retry_count", 0) + 1
        return {
            "messages": [
                HumanMessage(
                    content=(
                        f"题目数量不足：仅 {len(questions)} 道，需要至少 {min_questions} 道。"
                        "请补足并避免重复或高度相似的问题。"
                    )
                )
            ],
            "retry_count": new_retry_count,
        }
    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", build_system_prompt(state)),
            MessagesPlaceholder(variable_name="messages"),
            ("human", "请审核上面的面试问题预测。合格回复 APPROVE，否则给出具体修改意见。"),
        ]
    )

    chain = prompt | llm
    response = chain.invoke({"messages": sanitize_messages(state["messages"])})
    new_retry_count = state.get("retry_count", 0) + 1

    return {
        "messages": [HumanMessage(content=response.content)],
        "retry_count": new_retry_count,
    }
