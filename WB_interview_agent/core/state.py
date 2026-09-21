from typing import Annotated, Sequence, TypedDict
import operator
from typing_extensions import NotRequired
from langchain_core.messages import BaseMessage
from core.targeting import InterviewPlan


class RoundScore(TypedDict):
    round_id: int
    evaluator: str
    competency: str
    score: float
    evidence: str
    answer_excerpt: str
    question_excerpt: str


class InterviewState(TypedDict):
    session_id: str
    candidate_name: str
    target_industry: str
    job_description: str
    resume_highlights: str
    
    # 对话历史，使用 operator.add 确保每次追加而不覆盖
    messages: Annotated[Sequence[BaseMessage], operator.add]
    
    # 面试控制
    current_stage: str  # 可选值: greeting, ask_tech, ask_hr, eval_tech, eval_hr, end
    question_count: int
    max_questions: int
    tech_question_count: NotRequired[int]
    hr_question_count: NotRequired[int]
    last_interviewer: NotRequired[str]
    awaiting_answer: NotRequired[bool]
    interview_completed: NotRequired[bool]
    round_feedback: NotRequired[str]
    round_scores: Annotated[Sequence[RoundScore], operator.add]
    interview_plan: NotRequired[InterviewPlan]
    asked_tech_topics: Annotated[Sequence[str], operator.add]
    asked_hr_topics: Annotated[Sequence[str], operator.add]

    # 知识库检索（RAG）会话级路由配置
    rag_enabled: NotRequired[bool]
    rag_index_dir: NotRequired[str]
    rag_top_k: NotRequired[int]
    rag_max_context_chars: NotRequired[int]
    rag_query_template: NotRequired[str]
    allow_dangerous_faiss_deserialization: NotRequired[bool]
