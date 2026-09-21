import os
import unittest
from unittest.mock import patch

from langchain_core.messages import AIMessage

import main


class MainSessionSafetyTests(unittest.TestCase):
    def test_resolve_session_id_generates_unique_value_when_env_missing(self):
        with patch.dict(os.environ, {"CANDIDATE_ID": ""}, clear=False):
            session_id, generated = main.resolve_session_id()

        self.assertTrue(generated)
        self.assertTrue(session_id.startswith("session_"))
        self.assertNotEqual(session_id, "candidate_001")

    def test_detect_session_conflicts_rejects_cross_candidate_resume(self):
        stored_state = {
            "candidate_name": "候选人A",
            "target_industry": "Java后端开发",
            "job_description": "负责高并发微服务架构设计",
            "resume_highlights": "拥有 5 年开发经验",
        }
        expected_profile = {
            "candidate_name": "候选人B",
            "target_industry": "Java后端开发",
            "job_description": "负责高并发微服务架构设计",
            "resume_highlights": "拥有 5 年开发经验",
        }

        conflicts = main.detect_session_conflicts(stored_state, expected_profile)

        self.assertEqual(conflicts, ["candidate_name"])

    def test_infer_resume_state_patch_migrates_legacy_active_session(self):
        legacy_state = {
            "current_stage": "tech_eval",
            "tech_question_count": 1,
            "hr_question_count": 0,
            "messages": [AIMessage(content="请介绍一次性能优化实践")],
        }

        patch_payload = main.infer_resume_state_patch(legacy_state, "session_test_legacy")

        self.assertEqual(patch_payload["session_id"], "session_test_legacy")
        self.assertTrue(patch_payload["awaiting_answer"])
        self.assertEqual(patch_payload["last_interviewer"], "tech")
        self.assertFalse(patch_payload["interview_completed"])

    def test_infer_resume_state_patch_marks_legacy_finished_session_completed(self):
        legacy_state = {
            "current_stage": "end",
            "messages": [AIMessage(content="面试结束。感谢你的时间！\n\n【幕后生成的评估报告预览】：\n...")],
        }

        patch_payload = main.infer_resume_state_patch(legacy_state, "session_test_done")

        self.assertTrue(patch_payload["interview_completed"])


if __name__ == "__main__":
    unittest.main()
