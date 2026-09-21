import operator
from typing import Annotated, Sequence, TypedDict, Optional, Dict, Any
from langchain_core.messages import BaseMessage

class AgentState(TypedDict):
    # 消息历史
    messages: Annotated[Sequence[BaseMessage], operator.add]
    
    # 核心数据
    resume_text: str
    jd_text: str
    
    # --- [关键补充] 任务指令 ---
    # 必须存在，否则 Supervisor 无法读取 request 中的 task_type
    task_type: str

    # 面试题数量（可选，优先于默认配置）
    question_count: Optional[int]
    
    # 路由控制
    next: str
    
    # 重试计数器
    retry_count: int

    # 结构化的评估结果 (兼顾 简历评估 和 面试结果)
    evaluation_data: Optional[Dict[str, Any]]

    # 结构化输入
    input_data: Optional[Dict[str, Any]]
