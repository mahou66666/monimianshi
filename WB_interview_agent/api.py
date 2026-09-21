from typing import Any, Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from core.interview_service import (
    InterviewServiceError,
    get_interview_session,
    start_interview_session,
    submit_interview_answer,
)

app = FastAPI(title="WB Interview Agent API", version="1.0.0")


class RAGRouteConfigMixin(BaseModel):
    rag_enabled: Optional[bool] = Field(default=None, description="是否启用知识库检索。")
    rag_index_dir: Optional[str] = Field(default=None, description="知识库索引目录（相对项目根目录或绝对路径）。")
    rag_top_k: Optional[int] = Field(default=None, ge=1, description="检索召回条数。")
    rag_max_context_chars: Optional[int] = Field(default=None, ge=200, description="注入提示词的知识上下文最大长度。")
    rag_query_template: Optional[str] = Field(default=None, description="检索 query 模板，支持 {industry}/{answer}。")
    allow_dangerous_faiss_deserialization: Optional[bool] = Field(
        default=None,
        description="是否允许 FAISS 危险反序列化（本地可信文件场景）。",
    )


class InterviewStartRequest(RAGRouteConfigMixin):
    session_id: Optional[str] = Field(default=None, description="后端传入的会话 ID；为空时由 Agent 服务自动生成。")
    candidate_name: str
    target_industry: str
    job_description: str
    resume_highlights: str
    max_questions: int = Field(default=3, ge=1)


class InterviewAnswerRequest(RAGRouteConfigMixin):
    session_id: str
    answer: str
    candidate_name: Optional[str] = None
    target_industry: Optional[str] = None
    job_description: Optional[str] = None
    resume_highlights: Optional[str] = None


class InterviewStateRequest(RAGRouteConfigMixin):
    session_id: str
    candidate_name: Optional[str] = None
    target_industry: Optional[str] = None
    job_description: Optional[str] = None
    resume_highlights: Optional[str] = None


class InterviewTurnResponse(BaseModel):
    session_id: str
    session_created: bool
    status: str
    candidate_name: str
    target_industry: str
    question_count: int
    max_questions: int
    last_interviewer: str
    awaiting_answer: bool
    interview_completed: bool
    reply_type: str
    reply: str
    round_feedback: str
    latest_round_score: Optional[dict[str, Any]]
    score_summary: Optional[dict[str, Any]]
    final_report: Optional[str]
    event_log: list[dict[str, Any]]
    rag_config: Optional[dict[str, Any]] = None


@app.get("/health")
def healthcheck() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/api/interviews/start", response_model=InterviewTurnResponse)
def start_interview(request: InterviewStartRequest):
    try:
        return start_interview_session(request.model_dump())
    except InterviewServiceError as exc:
        raise HTTPException(status_code=exc.status_code, detail=str(exc)) from exc


@app.post("/api/interviews/answer", response_model=InterviewTurnResponse)
def answer_interview(request: InterviewAnswerRequest):
    try:
        return submit_interview_answer(request.model_dump())
    except InterviewServiceError as exc:
        raise HTTPException(status_code=exc.status_code, detail=str(exc)) from exc


@app.post("/api/interviews/state", response_model=InterviewTurnResponse)
def get_interview_state(request: InterviewStateRequest):
    try:
        return get_interview_session(request.model_dump())
    except InterviewServiceError as exc:
        raise HTTPException(status_code=exc.status_code, detail=str(exc)) from exc
