from app.prompts.critique_prompt import build_prompt as build_score_critique_prompt
from app.prompts.interview_coach_prompt import build_prompt as build_interview_prompt
from app.prompts.interview_critique_prompt import build_prompt as build_interview_critique_prompt
from app.prompts.problem_analysis_prompt import build_prompt as build_problem_prompt
from app.prompts.problem_critique_prompt import build_prompt as build_problem_critique_prompt
from app.prompts.resume_analyst_prompt import build_prompt as build_resume_prompt


def _state(task_type: str = "score_only"):
    return {
        "task_type": task_type,
        "jd_text": "",
        "input_data": None,
        "question_count": 40,
    }


def test_resume_prompt_contains_garbled_policy():
    prompt = build_resume_prompt(_state("score_only"))
    assert "乱码容错" in prompt
    assert "禁止仅因乱码片段" in prompt


def test_problem_prompt_contains_garbled_policy():
    prompt = build_problem_prompt(_state("problem_only"))
    assert "乱码容错" in prompt
    assert "不得将乱码本身当作候选人问题" in prompt


def test_interview_prompt_contains_garbled_policy():
    prompt = build_interview_prompt(_state("interview_resume"))
    assert "乱码容错检查" in prompt
    assert "不得据此生成质疑题" in prompt


def test_critique_prompts_contain_garbled_policy():
    score_prompt = build_score_critique_prompt(_state("score_only"))
    problem_prompt = build_problem_critique_prompt(_state("problem_only"))
    interview_prompt = build_interview_critique_prompt(_state("interview"))
    assert "乱码容错一致性" in score_prompt
    assert "乱码容错一致性" in problem_prompt
    assert "乱码容错一致性" in interview_prompt
