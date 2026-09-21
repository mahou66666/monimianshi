import unittest
from unittest.mock import patch

from langchain_core.messages import AIMessage, HumanMessage

from core.interview_service import (
    InterviewServiceError,
    get_interview_session,
    start_interview_session,
    submit_interview_answer,
)


class _Snapshot:
    def __init__(self, values):
        self.values = values


class _FakeInterviewApp:
    def __init__(self):
        self.states = {}

    def get_state(self, config):
        session_id = config["configurable"]["thread_id"]
        return _Snapshot(self.states.get(session_id, {}))

    def stream(self, update, config):
        session_id = config["configurable"]["thread_id"]
        state = self.states.get(session_id)
        if not state:
            greeting = AIMessage(content="你好，候选人！我们开始吧。")
            question = AIMessage(content="请介绍一次你主导的性能优化实践。")
            new_state = dict(update)
            new_state["messages"] = [greeting, question]
            new_state["question_count"] = 1
            new_state["max_questions"] = 2
            new_state["tech_question_count"] = 1
            new_state["hr_question_count"] = 0
            new_state["last_interviewer"] = "tech"
            new_state["awaiting_answer"] = True
            new_state["interview_completed"] = False
            self.states[session_id] = new_state
            yield {"Orchestrator": {"messages": [greeting]}}
            yield {"TechQuestioner": {"messages": [question]}}
            return

        state = dict(state)
        for key, value in update.items():
            if key == "messages":
                continue
            state[key] = value
        messages = list(state.get("messages", [])) + list(update.get("messages", []))
        state["messages"] = messages
        score = {
            "round_id": state["question_count"],
            "evaluator": "tech",
            "competency": "性能优化",
            "score": 8.0,
            "evidence": "回答给出了量化指标。",
            "answer_excerpt": getattr(update["messages"][0], "content", "")[:160],
            "question_excerpt": "请介绍一次你主导的性能优化实践。",
        }
        state["round_scores"] = list(state.get("round_scores", [])) + [score]
        yield {
            "TechEvaluator": {
                "round_feedback": (
                    "【本轮结构化评分】\n"
                    "competency: 性能优化\n"
                    "score: 8.0/10\n"
                    "evidence: 回答给出了量化指标。"
                )
            }
        }

        if state["question_count"] >= state["max_questions"]:
            state["awaiting_answer"] = False
            state["interview_completed"] = True
            final_message = AIMessage(content="面试结束。感谢你的时间！\n\n【幕后生成的评估报告预览】：\n报告内容")
            state["messages"] = list(state["messages"]) + [final_message]
            self.states[session_id] = state
            yield {"Reporter": {"messages": [final_message]}}
            return

        next_question = AIMessage(content="下一题：请说明你如何做容量评估。")
        state["messages"] = list(state["messages"]) + [next_question]
        state["question_count"] += 1
        state["awaiting_answer"] = True
        state["last_interviewer"] = "tech"
        self.states[session_id] = state
        yield {"TechQuestioner": {"messages": [next_question]}}


class InterviewServiceTests(unittest.TestCase):
    def test_start_interview_session_returns_structured_response(self):
        fake_app = _FakeInterviewApp()
        payload = {
            "candidate_name": "候选人A",
            "target_industry": "Java后端开发",
            "job_description": "负责高并发微服务。",
            "resume_highlights": "主导过性能优化项目。",
            "max_questions": 2,
        }

        with patch("core.interview_service.load_interview_app", return_value=fake_app):
            result = start_interview_session(payload)

        self.assertTrue(result["session_created"])
        self.assertTrue(result["session_id"].startswith("session_"))
        self.assertEqual(result["reply"], "请介绍一次你主导的性能优化实践。")
        self.assertTrue(result["awaiting_answer"])
        self.assertEqual(len(result["event_log"]), 2)

    def test_submit_interview_answer_returns_feedback_and_next_question(self):
        fake_app = _FakeInterviewApp()
        start_payload = {
            "session_id": "session_test_api",
            "candidate_name": "候选人A",
            "target_industry": "Java后端开发",
            "job_description": "负责高并发微服务。",
            "resume_highlights": "主导过性能优化项目。",
            "max_questions": 2,
        }
        answer_payload = {
            "session_id": "session_test_api",
            "answer": "我把 p95 从 500ms 降到了 180ms。",
        }

        with patch("core.interview_service.load_interview_app", return_value=fake_app):
            start_interview_session(start_payload)
            result = submit_interview_answer(answer_payload)

        self.assertIn("【本轮结构化评分】", result["round_feedback"])
        self.assertEqual(result["reply"], "下一题：请说明你如何做容量评估。")
        self.assertIsNotNone(result["latest_round_score"])
        self.assertFalse(result["interview_completed"])

    def test_submit_final_answer_returns_report_payload(self):
        fake_app = _FakeInterviewApp()
        start_payload = {
            "session_id": "session_test_done",
            "candidate_name": "候选人A",
            "target_industry": "Java后端开发",
            "job_description": "负责高并发微服务。",
            "resume_highlights": "主导过性能优化项目。",
            "max_questions": 2,
        }

        with patch("core.interview_service.load_interview_app", return_value=fake_app):
            start_interview_session(start_payload)
            submit_interview_answer({"session_id": "session_test_done", "answer": "第一轮回答"})
            result = submit_interview_answer({"session_id": "session_test_done", "answer": "第二轮回答"})

        self.assertTrue(result["interview_completed"])
        self.assertEqual(result["reply_type"], "report")
        self.assertIsNotNone(result["final_report"])
        self.assertEqual(result["status"], "completed")

    def test_get_interview_session_returns_existing_question(self):
        fake_app = _FakeInterviewApp()
        start_payload = {
            "session_id": "session_test_state",
            "candidate_name": "候选人A",
            "target_industry": "Java后端开发",
            "job_description": "负责高并发微服务。",
            "resume_highlights": "主导过性能优化项目。",
            "max_questions": 2,
        }

        with patch("core.interview_service.load_interview_app", return_value=fake_app):
            start_interview_session(start_payload)
            result = get_interview_session({"session_id": "session_test_state"})

        self.assertEqual(result["reply"], "请介绍一次你主导的性能优化实践。")
        self.assertTrue(result["awaiting_answer"])
        self.assertFalse(result["session_created"])

    def test_start_session_persists_rag_route_config(self):
        fake_app = _FakeInterviewApp()
        payload = {
            "candidate_name": "候选人A",
            "target_industry": "金融风控",
            "job_description": "负责授信策略与模型监控。",
            "resume_highlights": "具备风控建模经验。",
            "max_questions": 2,
            "rag_enabled": False,
            "rag_index_dir": "memory/knowledge_index/finance_risk",
            "rag_top_k": 3,
        }

        with patch("core.interview_service.load_interview_app", return_value=fake_app):
            result = start_interview_session(payload)

        session_id = result["session_id"]
        self.assertIn(session_id, fake_app.states)
        self.assertFalse(fake_app.states[session_id]["rag_enabled"])
        self.assertEqual(fake_app.states[session_id]["rag_top_k"], 3)
        self.assertEqual(result["rag_config"]["rag_index_dir"], "memory/knowledge_index/finance_risk")

    def test_submit_answer_updates_rag_route_config(self):
        fake_app = _FakeInterviewApp()
        start_payload = {
            "session_id": "session_test_rag_update",
            "candidate_name": "候选人A",
            "target_industry": "Java后端开发",
            "job_description": "负责高并发微服务。",
            "resume_highlights": "主导过性能优化项目。",
            "max_questions": 2,
        }
        answer_payload = {
            "session_id": "session_test_rag_update",
            "answer": "我把 p95 从 500ms 降到了 180ms。",
            "rag_enabled": True,
            "rag_index_dir": "memory/knowledge_index/computer_backend",
            "rag_top_k": 4,
            "rag_max_context_chars": 1500,
        }

        with patch("core.interview_service.load_interview_app", return_value=fake_app):
            start_interview_session(start_payload)
            result = submit_interview_answer(answer_payload)

        state = fake_app.states["session_test_rag_update"]
        self.assertTrue(state["rag_enabled"])
        self.assertEqual(state["rag_index_dir"], "memory/knowledge_index/computer_backend")
        self.assertEqual(state["rag_top_k"], 4)
        self.assertEqual(state["rag_max_context_chars"], 1500)
        self.assertEqual(result["rag_config"]["rag_top_k"], 4)

    def test_start_session_rejects_invalid_rag_enabled(self):
        fake_app = _FakeInterviewApp()
        payload = {
            "candidate_name": "候选人A",
            "target_industry": "Java后端开发",
            "job_description": "负责高并发微服务。",
            "resume_highlights": "主导过性能优化项目。",
            "rag_enabled": "not_bool",
        }

        with patch("core.interview_service.load_interview_app", return_value=fake_app):
            with self.assertRaises(InterviewServiceError) as ctx:
                start_interview_session(payload)

        self.assertEqual(ctx.exception.status_code, 400)


if __name__ == "__main__":
    unittest.main()
