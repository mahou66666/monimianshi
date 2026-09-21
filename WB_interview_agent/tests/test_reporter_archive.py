import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from langchain_core.messages import AIMessage, HumanMessage

os.environ.setdefault("SILICONFLOW_API_KEY", "test-key")

import agents.reporter as reporter


class ReporterArchiveTests(unittest.TestCase):
    def test_reporter_archives_redacted_scores_only_when_enabled(self):
        state = {
            "session_id": "session_test_001",
            "candidate_name": "候选人A",
            "target_industry": "Java后端开发",
            "question_count": 2,
            "round_scores": [
                {
                    "round_id": 1,
                    "evaluator": "tech",
                    "competency": "系统设计",
                    "score": 8.0,
                    "evidence": "给出了容量估算与降级策略。",
                    "answer_excerpt": "容量估算...",
                    "question_excerpt": "如何做高并发设计",
                },
                {
                    "round_id": 2,
                    "evaluator": "hr",
                    "competency": "沟通协作",
                    "score": 7.0,
                    "evidence": "清楚描述了跨团队冲突处理。",
                    "answer_excerpt": "我先对齐目标...",
                    "question_excerpt": "冲突如何处理",
                },
            ],
            "messages": [
                AIMessage(content="请介绍你的项目"),
                HumanMessage(content="我负责过高并发系统改造"),
            ],
        }
        with tempfile.TemporaryDirectory() as temp_dir:
            with patch.dict(
                os.environ,
                {
                    "ARCHIVE_REPORTS_ENABLED": "true",
                    "ARCHIVE_INCLUDE_RAW_CHAT": "false",
                    "ARCHIVE_REDACT_SENSITIVE": "true",
                },
                clear=False,
            ), patch.object(reporter, "ARCHIVE_DIR", Path(temp_dir)):
                result = reporter.run_reporter(state)

            archive_files = list(Path(temp_dir).glob("*.json"))
            self.assertEqual(len(archive_files), 1)
            with archive_files[0].open("r", encoding="utf-8") as file_obj:
                payload = json.load(file_obj)

        self.assertIn("score_summary", payload)
        self.assertIn("round_scores", payload)
        self.assertEqual(payload["score_summary"]["overall_score"], 7.5)
        self.assertTrue(payload["candidate_name"].startswith("candidate_"))
        self.assertEqual(payload["raw_chat"], [])
        self.assertNotIn("候选人A", json.dumps(payload, ensure_ascii=False))
        self.assertIn("结构化评估总览", result["messages"][0].content)
        self.assertIn("归档已保存为脱敏文件", result["messages"][0].content)

    def test_reporter_does_not_write_archive_by_default(self):
        state = {
            "session_id": "session_test_002",
            "candidate_name": "候选人B",
            "target_industry": "Java后端开发",
            "question_count": 1,
            "round_scores": [],
            "messages": [],
        }
        with tempfile.TemporaryDirectory() as temp_dir:
            with patch.dict(
                os.environ,
                {
                    "ARCHIVE_REPORTS_ENABLED": "false",
                    "ARCHIVE_INCLUDE_RAW_CHAT": "false",
                    "ARCHIVE_REDACT_SENSITIVE": "true",
                },
                clear=False,
            ), patch.object(reporter, "ARCHIVE_DIR", Path(temp_dir)):
                result = reporter.run_reporter(state)

            archive_files = list(Path(temp_dir).glob("*.json"))

        self.assertEqual(archive_files, [])
        self.assertIn("归档已关闭", result["messages"][0].content)


if __name__ == "__main__":
    unittest.main()
