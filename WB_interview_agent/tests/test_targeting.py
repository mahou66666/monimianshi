import unittest

from core.targeting import build_interview_plan, pick_next_focus_topic


class TargetingPlanTests(unittest.TestCase):
    def test_build_interview_plan_extracts_gap_topics(self):
        jd = "需要熟悉 Java、Spring Boot、MySQL、Redis、Kafka，高并发与分布式一致性经验。"
        resume = "候选人有 Java、Spring Boot、Redis 项目经验，主导过高并发优化。"
        plan = build_interview_plan(jd, resume, "Java后端开发")
        self.assertIn("MySQL", plan["gaps"])
        self.assertIn("Kafka", plan["gaps"])
        self.assertTrue(len(plan["tech_focus_topics"]) > 0)

    def test_pick_next_focus_topic_skips_asked_topics(self):
        focus = ["MySQL", "Kafka", "Redis"]
        asked = ["MySQL"]
        self.assertEqual(pick_next_focus_topic(focus, asked), "Kafka")
        self.assertEqual(pick_next_focus_topic(["MySQL"], ["MySQL"]), "")


if __name__ == "__main__":
    unittest.main()
