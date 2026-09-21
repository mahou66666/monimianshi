# Interview_elf_agent/app/graph/workflow.py

from langgraph.graph import StateGraph, END

# 引入核心配置和状态
from app.graph.state import AgentState

# 引入各个工作节点 (Chains)
from app.chains.scorer import resume_analyst_node
from app.chains.question_gen import interview_coach_node
from app.chains.critique import critique_node as score_critique_node
from app.chains.problem_analysis import problem_analysis_node
from app.chains.problem_critique import problem_critique_node
from app.chains.interview_critique import interview_critique_node
from app.utils.interview_mode import is_interview_task, normalize_task_type

# ============================================================
# 1. Router：根据 task_type 决定唯一任务
# ============================================================

def route_by_task(state):
    task_type = normalize_task_type(state.get("task_type") or "auto")
    if task_type == "problem_only":
        return "Problem_Analyst"
    if task_type == "score_only":
        return "Resume_Analyst"
    if is_interview_task(task_type):
        return "Interview_Coach"
    if task_type == "auto":
        return "Interview_Coach" if state.get("jd_text") else "Resume_Analyst"
    # 未支持任务类型时，回退到简历评分
    return "Resume_Analyst"


def router_node(state):
    return {"next": route_by_task(state)}


def should_retry(state):
    messages = state["messages"]
    last_message = messages[-1]
    content = last_message.content.strip().upper()
    retry_count = state.get("retry_count", 0)

    if "APPROVE" in content:
        return "END"
    if retry_count >= 3:
        return "END"
    return "RETRY"

# ============================================================
# 2. 构建图 (Graph Construction)
# ============================================================

def create_graph():
    workflow = StateGraph(AgentState)

    workflow.add_node("Router", router_node)
    workflow.add_node("Problem_Analyst", problem_analysis_node)
    workflow.add_node("Problem_Reviewer", problem_critique_node)
    workflow.add_node("Resume_Analyst", resume_analyst_node)
    workflow.add_node("Score_Reviewer", score_critique_node)
    workflow.add_node("Interview_Coach", interview_coach_node)
    workflow.add_node("Interview_Reviewer", interview_critique_node)

    workflow.add_conditional_edges(
        "Router",
        lambda x: x["next"],
        {
            "Problem_Analyst": "Problem_Analyst",
            "Resume_Analyst": "Resume_Analyst",
            "Interview_Coach": "Interview_Coach",
        },
    )

    # --- Problem analysis loop ---
    workflow.add_edge("Problem_Analyst", "Problem_Reviewer")
    workflow.add_conditional_edges(
        "Problem_Reviewer",
        should_retry,
        {
            "RETRY": "Problem_Analyst",
            "END": END,
        },
    )

    # --- Scoring loop ---
    workflow.add_edge("Resume_Analyst", "Score_Reviewer")
    workflow.add_conditional_edges(
        "Score_Reviewer",
        should_retry,
        {
            "RETRY": "Resume_Analyst",
            "END": END,
        },
    )

    # --- Interview loop ---
    workflow.add_edge("Interview_Coach", "Interview_Reviewer")
    workflow.add_conditional_edges(
        "Interview_Reviewer",
        should_retry,
        {
            "RETRY": "Interview_Coach",
            "END": END,
        },
    )

    workflow.set_entry_point("Router")

    return workflow.compile()


app_graph = create_graph()
