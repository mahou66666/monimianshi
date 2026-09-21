# Interview_elf_agent/app/chains/critique.py

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.messages import HumanMessage
from app.core.config import settings
from app.prompts.critique import build_system_prompt
from app.utils.message_utils import sanitize_messages

# [修改] 传入 base_url
llm = ChatOpenAI(
    base_url=settings.BASE_URL,
    api_key=settings.OPENAI_API_KEY, 
    model=settings.MODEL_NAME, 
    temperature=settings.TEMPERATURE,
    top_p=settings.TOP_P,
    frequency_penalty=settings.FREQUENCY_PENALTY,
    model_kwargs=settings.MODEL_KWARGS
)

def critique_node(state):
    """
    审核节点：检查上一条 Resume_Analyst 的回复质量
    """
    prompt = ChatPromptTemplate.from_messages([
        ("system", build_system_prompt(state)),
        MessagesPlaceholder(variable_name="messages"),
        ("human", "请审核上面的分析报告。如果是合格的回复 'APPROVE'，否则给出具体的改进反馈。")
    ])
    
    chain = prompt | llm
    
    response = chain.invoke({"messages": sanitize_messages(state["messages"])})
    
    new_retry_count = state.get("retry_count", 0) + 1
    
    return {
        "messages": [HumanMessage(content=response.content)],
        "retry_count": new_retry_count
    }
