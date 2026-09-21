import os
import unittest

from langchain_core.messages import AIMessage, HumanMessage

os.environ.setdefault("SILICONFLOW_API_KEY", "test-key")

from agents.orchestrator import calc_tech_target, run_orchestrator


class RoutingStrategyTests(unittest.TestCase):
    def test_calc_tech_target(self):
        self.assertEqual(calc_tech_target(1), 1)
        self.assertEqual(calc_tech_target(2), 1)
        self.assertEqual(calc_tech_target(3), 2)
        self.assertEqual(calc_tech_target(6), 4)

    def test_orchestrator_stage_routing(self):
        greeting_state = {
            "candidate_name": "A",
            "messages": [],
            "question_count": 0,
            "max_questions": 3,
            "tech_question_count": 0,
            "hr_question_count": 0,
            "awaiting_answer": False,
        }
        greeting_result = run_orchestrator(greeting_state)
        self.assertEqual(greeting_result["current_stage"], "greeting")
        self.assertTrue(greeting_result["awaiting_answer"])
        self.assertEqual(greeting_result["last_interviewer"], "intro")

        intro_answer_state = {
            "candidate_name": "A",
            "messages": [AIMessage(content="请先做一个简短自我介绍。"), HumanMessage(content="我是后端工程师...")],
            "question_count": 0,
            "max_questions": 3,
            "tech_question_count": 0,
            "hr_question_count": 0,
            "last_interviewer": "intro",
            "awaiting_answer": True,
        }
        self.assertEqual(run_orchestrator(intro_answer_state)["current_stage"], "ask_tech")

        evaluate_tech_state = {
            "candidate_name": "A",
            "messages": [AIMessage(content="技术问题"), HumanMessage(content="技术回答")],
            "question_count": 1,
            "max_questions": 3,
            "tech_question_count": 1,
            "hr_question_count": 0,
            "last_interviewer": "tech",
            "awaiting_answer": True,
        }
        self.assertEqual(run_orchestrator(evaluate_tech_state)["current_stage"], "eval_tech")

        ask_hr_state = {
            "candidate_name": "A",
            "messages": [AIMessage(content="技术问题"), HumanMessage(content="技术回答")],
            "question_count": 2,
            "max_questions": 3,
            "tech_question_count": 2,
            "hr_question_count": 0,
            "awaiting_answer": False,
        }
        self.assertEqual(run_orchestrator(ask_hr_state)["current_stage"], "ask_hr")

        evaluate_hr_state = {
            "candidate_name": "A",
            "messages": [AIMessage(content="HR问题"), HumanMessage(content="HR回答")],
            "question_count": 3,
            "max_questions": 3,
            "tech_question_count": 2,
            "hr_question_count": 1,
            "last_interviewer": "hr",
            "awaiting_answer": True,
        }
        self.assertEqual(run_orchestrator(evaluate_hr_state)["current_stage"], "eval_hr")

        end_state = {
            "candidate_name": "A",
            "messages": [AIMessage(content="HR问题"), HumanMessage(content="HR回答")],
            "question_count": 3,
            "max_questions": 3,
            "tech_question_count": 2,
            "hr_question_count": 1,
            "awaiting_answer": False,
        }
        self.assertEqual(run_orchestrator(end_state)["current_stage"], "end")


if __name__ == "__main__":
    unittest.main()
