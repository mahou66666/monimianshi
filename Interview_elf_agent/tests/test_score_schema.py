import pytest
from pydantic import ValidationError

from app.api.schemas import EvaluationResult, Scores


def _scores():
    return Scores(
        education_score=80,
        work_score=75,
        project_score=78,
        skill_score=82,
        award_score=60,
        job_fit_score=70,
        total_score=77,
    )


def test_evaluation_result_accepts_structured_problems():
    result = EvaluationResult(
        scores=_scores(),
        dimension_analysis="各维度评分理由...",
        dimension_briefs=[
            {
                "dimension": "教育经历",
                "score": 80,
                "deduction_summary": "院校层次信息不够完整。",
                "strength_summary": "专业方向与岗位相关。",
            }
        ],
        problems=[
            {
                "title": "项目描述缺少量化结果",
                "severity": "中",
                "tags": ["项目经历", "量化"],
                "problem": "项目描述偏概括，缺少关键结果。",
                "answer": "建议：补充性能指标和业务影响。",
            }
        ],
        advice="建议补充量化成果。",
    )
    assert result.problems[0].title == "项目描述缺少量化结果"
    assert result.dimension_briefs[0].dimension == "教育经历"


def test_evaluation_result_rejects_plain_string_problem():
    with pytest.raises(ValidationError):
        EvaluationResult(
            scores=_scores(),
            dimension_analysis="各维度评分理由...",
            problems=["问题1"],
            advice="建议补充量化成果。",
        )
