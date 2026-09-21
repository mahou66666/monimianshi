# Interview_elf_agent/app/chains/problem_critique.py

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.messages import HumanMessage

from app.core.config import settings
from app.prompts.problem_critique import build_system_prompt
from app.utils.message_utils import sanitize_messages


llm = ChatOpenAI(
    base_url=settings.BASE_URL,
    api_key=settings.OPENAI_API_KEY,
    model=settings.MODEL_NAME,
    temperature=settings.TEMPERATURE,
    top_p=settings.TOP_P,
    frequency_penalty=settings.FREQUENCY_PENALTY,
    model_kwargs=settings.MODEL_KWARGS,
)


def problem_critique_node(state):
    prompt = ChatPromptTemplate.from_messages(
        [
            ("system", build_system_prompt(state)),
            MessagesPlaceholder(variable_name="messages"),
            ("human", "请审核上面的简历问题分析。合格回复 APPROVE，否则给出具体修改意见。"),
        ]
    )

    chain = prompt | llm
    response = chain.invoke({"messages": sanitize_messages(state["messages"])})
    new_retry_count = state.get("retry_count", 0) + 1

    return {
        "messages": [HumanMessage(content=response.content)],
        "retry_count": new_retry_count,
    }
