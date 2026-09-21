import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient

from api import app


class ApiTests(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_healthcheck(self):
        response = self.client.get("/health")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ok"})

    def test_start_endpoint_returns_service_payload(self):
        payload = {
            "session_id": "session_api_001",
            "candidate_name": "候选人A",
            "target_industry": "Java后端开发",
            "job_description": "负责高并发微服务。",
            "resume_highlights": "主导过性能优化项目。",
            "max_questions": 3,
        }
        service_result = {
            "session_id": "session_api_001",
            "session_created": True,
            "status": "in_progress",
            "candidate_name": "候选人A",
            "target_industry": "Java后端开发",
            "question_count": 1,
            "max_questions": 3,
            "last_interviewer": "tech",
            "awaiting_answer": True,
            "interview_completed": False,
            "reply_type": "question",
            "reply": "请介绍一次性能优化实践。",
            "round_feedback": "",
            "latest_round_score": None,
            "score_summary": None,
            "final_report": None,
            "event_log": [],
        }

        with patch("api.start_interview_session", return_value=service_result):
            response = self.client.post("/api/interviews/start", json=payload)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["reply"], "请介绍一次性能优化实践。")

    def test_answer_endpoint_accepts_rag_route_fields(self):
        payload = {
            "session_id": "session_api_001",
            "answer": "这是我的回答",
            "rag_enabled": True,
            "rag_index_dir": "memory/knowledge_index/computer_backend",
            "rag_top_k": 3,
            "rag_max_context_chars": 1200,
        }
        service_result = {
            "session_id": "session_api_001",
            "session_created": False,
            "status": "in_progress",
            "candidate_name": "候选人A",
            "target_industry": "Java后端开发",
            "question_count": 2,
            "max_questions": 3,
            "last_interviewer": "tech",
            "awaiting_answer": True,
            "interview_completed": False,
            "reply_type": "question",
            "reply": "下一题：请说明你如何做容量评估。",
            "round_feedback": "【本轮结构化评分】...",
            "latest_round_score": {"score": 8.0},
            "score_summary": None,
            "final_report": None,
            "event_log": [],
            "rag_config": {
                "rag_enabled": True,
                "rag_index_dir": "memory/knowledge_index/computer_backend",
                "rag_top_k": 3,
                "rag_max_context_chars": 1200,
            },
        }

        with patch("api.submit_interview_answer", return_value=service_result):
            response = self.client.post("/api/interviews/answer", json=payload)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["rag_config"]["rag_top_k"], 3)


if __name__ == "__main__":
    unittest.main()
