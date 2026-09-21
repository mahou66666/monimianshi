import atexit
import os

from langgraph.graph import END, StateGraph

from agents.domain_expert import run_domain_expert_evaluation, run_domain_expert_question
from agents.hr_assessor import run_hr_assessor_evaluation, run_hr_assessor_question
from agents.orchestrator import run_orchestrator
from agents.reporter import run_reporter
from core.state import InterviewState

POSTGRES_CHECKPOINT_URL = os.getenv("POSTGRES_CHECKPOINT_URL", "").strip()
CHECKPOINTER_FALLBACK_MEMORY = os.getenv("CHECKPOINTER_FALLBACK_MEMORY", "true").strip().lower() in {
    "1",
    "true",
    "yes",
    "on",
}


def _build_workflow() -> StateGraph:
    workflow = StateGraph(InterviewState)

    workflow.add_node("Orchestrator", run_orchestrator)
    workflow.add_node("TechQuestioner", run_domain_expert_question)
    workflow.add_node("TechEvaluator", run_domain_expert_evaluation)
    workflow.add_node("HRQuestioner", run_hr_assessor_question)
    workflow.add_node("HREvaluator", run_hr_assessor_evaluation)
    workflow.add_node("Reporter", run_reporter)

    workflow.set_entry_point("Orchestrator")

    workflow.add_edge("TechEvaluator", "Orchestrator")
    workflow.add_edge("HREvaluator", "Orchestrator")
    workflow.add_edge("TechQuestioner", END)
    workflow.add_edge("HRQuestioner", END)

    def route_from_orchestrator(state: InterviewState):
        stage = state.get("current_stage", "greeting")
        if stage == "ask_tech":
            return "TechQuestioner"
        if stage == "ask_hr":
            return "HRQuestioner"
        if stage == "eval_tech":
            return "TechEvaluator"
        if stage == "eval_hr":
            return "HREvaluator"
        if stage == "end":
            return "Reporter"
        return END

    workflow.add_conditional_edges(
        "Orchestrator",
        route_from_orchestrator,
        {
            "TechQuestioner": "TechQuestioner",
            "HRQuestioner": "HRQuestioner",
            "TechEvaluator": "TechEvaluator",
            "HREvaluator": "HREvaluator",
            "Reporter": "Reporter",
            END: END,
        },
    )
    workflow.add_edge("Reporter", END)
    return workflow


def _build_memory_checkpointer(reason: str):
    try:
        from langgraph.checkpoint.memory import MemorySaver
    except ImportError as exc:
        raise RuntimeError(
            f"Cannot use memory checkpointer ({reason}). Missing langgraph.checkpoint.memory: {exc}"
        ) from exc

    print(f"[graph] using in-memory checkpointer: {reason}")
    return MemorySaver()


def _build_postgres_checkpointer():
    if not POSTGRES_CHECKPOINT_URL:
        if CHECKPOINTER_FALLBACK_MEMORY:
            return _build_memory_checkpointer("missing POSTGRES_CHECKPOINT_URL")
        raise RuntimeError("POSTGRES_CHECKPOINT_URL is required")

    try:
        import psycopg
        from langgraph.checkpoint.postgres import PostgresSaver
        from psycopg.rows import dict_row
    except ImportError as exc:
        if CHECKPOINTER_FALLBACK_MEMORY:
            return _build_memory_checkpointer(f"postgres dependency missing: {exc}")
        raise RuntimeError(
            "Missing postgres checkpoint dependencies. Install: langgraph-checkpoint-postgres psycopg[binary]"
        ) from exc

    try:
        conn = psycopg.connect(POSTGRES_CHECKPOINT_URL, autocommit=True, row_factory=dict_row)
        checkpointer = PostgresSaver(conn)
        checkpointer.setup()
        atexit.register(conn.close)
        return checkpointer
    except Exception as exc:
        if CHECKPOINTER_FALLBACK_MEMORY:
            return _build_memory_checkpointer(f"postgres init failed: {exc}")
        raise RuntimeError(f"Postgres checkpointer init failed: {exc}") from exc


def build_interview_graph():
    workflow = _build_workflow()
    checkpointer = _build_postgres_checkpointer()
    return workflow.compile(checkpointer=checkpointer)


interview_app = build_interview_graph()
