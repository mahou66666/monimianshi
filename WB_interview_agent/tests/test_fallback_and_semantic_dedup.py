import os
import unittest
from unittest.mock import patch

from langchain_core.messages import AIMessage, HumanMessage

os.environ.setdefault("SILICONFLOW_API_KEY", "test-key")

import agents.domain_expert as domain_expert
import core.semantic_dedup as semantic_dedup


class _RaiseLLM:
    def invoke(self, *_args, **_kwargs):
        raise RuntimeError("mock error")


class _EmbeddingStub:
    def embed_documents(self, _docs):
        return [[1.0, 0.0, 0.0], [0.99, 0.01, 0.0]]


class FallbackAndSemanticDedupTests(unittest.TestCase):
    def test_domain_expert_question_exception_fallback(self):
        state = {
            "target_industry": "Java后端开发",
            "job_description": "负责高并发微服务。",
            "messages": [
                AIMessage(content="请介绍一次性能优化实践"),
                HumanMessage(content="我通过压测和监控把 p95 从 500ms 降到了 180ms。"),
            ],
            "question_count": 1,
            "tech_question_count": 1,
        }
        with patch.object(domain_expert, "reasoning_llm", _RaiseLLM()):
            result = domain_expert.run_domain_expert_question(state)

        self.assertIn("请说明你在分布式系统中处理数据一致性的方案", result["messages"][0].content)
        self.assertEqual(result["tech_question_count"], 2)
        self.assertTrue(result["awaiting_answer"])

    def test_domain_expert_evaluation_writes_score_without_polluting_messages(self):
        state = {
            "target_industry": "Java后端开发",
            "job_description": "负责高并发微服务。",
            "messages": [
                AIMessage(content="请介绍一次性能优化实践"),
                HumanMessage(content="我通过压测和监控把 p95 从 500ms 降到了 180ms。"),
            ],
            "question_count": 1,
            "tech_question_count": 1,
            "asked_tech_topics": ["高并发"],
        }
        mock_score = {
            "round_id": 1,
            "evaluator": "tech",
            "competency": "性能优化",
            "score": 8.2,
            "evidence": "回答包含压测指标和结果。",
            "answer_excerpt": "我通过压测...",
            "question_excerpt": "请介绍一次性能优化实践",
        }
        with patch.object(domain_expert, "build_round_score", return_value=mock_score):
            result = domain_expert.run_domain_expert_evaluation(state)

        self.assertIn("【本轮结构化评分】", result["round_feedback"])
        self.assertEqual(result["round_scores"][0]["competency"], "性能优化")
        self.assertFalse(result["awaiting_answer"])
        self.assertNotIn("messages", result)

    def test_semantic_dedup_hits_by_embedding_similarity_for_legacy_score_plus_question_message(self):
        messages = [
            AIMessage(
                content=(
                    "【本轮结构化评分】\n"
                    "competency: 性能优化\n"
                    "score: 8.2/10\n"
                    "evidence: 回答包含压测指标和结果。\n\n"
                    "请介绍一次性能优化实践"
                )
            )
        ]
        with patch.object(semantic_dedup, "embeddings_model", _EmbeddingStub()):
            duplicated = semantic_dedup.is_semantically_duplicate_question(
                "请描述你做过的性能优化案例",
                messages,
                threshold=0.9,
            )
        self.assertTrue(duplicated)


if __name__ == "__main__":
    unittest.main()
