import pytest
from pydantic import ValidationError

from app.api.schemas import ProblemAnalysisResult


def test_problem_analysis_result_requires_resume_overview():
    with pytest.raises(ValidationError):
        ProblemAnalysisResult(
            problems=[],
            optimization_checklist=[],
            highlights=[],
            advice="建议补充量化结果。",
        )


def test_problem_analysis_result_accepts_resume_overview():
    result = ProblemAnalysisResult(
        resume_overview="简历主体信息较完整，但项目成果量化不足。",
        problems=[],
        optimization_checklist=[],
        highlights=[],
        advice="建议补充量化结果。",
    )
    assert result.resume_overview.endswith("。")
