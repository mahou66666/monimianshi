import pytest
from pydantic import ValidationError

from app.api.schemas import AnalyzeRequest
from app.utils.resume import assemble_resume_markdown, has_meaningful_resume_data


def test_legacy_input_data_is_normalized():
    req = AnalyzeRequest.model_validate(
        {
            "task_type": "score_only",
            "input_data": {
                "BASIC_INFO": {"name": "张三", "jobIntention": "后端开发"},
                "SKILLS": "Python,SQL,FastAPI",
                "EDUCATION": ["某大学 本科 计算机"],
                "WORK_EXPERIENCE": ["某公司 后端开发工程师 2024.01-2025.12"],
                "SELF_EVALUATION": "执行力强",
            },
        }
    )

    assert req.input_data is not None
    assert req.input_data.personal_info is not None
    assert req.input_data.personal_info.name == "张三"
    assert req.input_data.personal_info.job_intention == "后端开发"
    assert req.input_data.skills == "Python,SQL,FastAPI"
    assert req.input_data.work_history is not None
    assert req.input_data.self_evaluation == "执行力强"


def test_plain_text_sections_are_supported():
    req = AnalyzeRequest.model_validate(
        {
            "task_type": "score_only",
            "input_data": {
                "personal_info": {"name": "王五"},
                "education": "某大学 本科 计算机科学",
                "work_history": "某公司 后端工程师 2023-2025",
                "skills": "Python、FastAPI、MySQL",
            },
        }
    )
    assert req.input_data is not None
    assert req.input_data.education == "某大学 本科 计算机科学"
    assert req.input_data.work_history == "某公司 后端工程师 2023-2025"
    assert req.input_data.skills == "Python、FastAPI、MySQL"


def test_education_string_and_list_formats_are_supported():
    req_str = AnalyzeRequest.model_validate(
        {
            "task_type": "score_only",
            "input_data": {
                "personal_info": {"name": "甲"},
                "education": "某大学 本科 计算机科学 2019.09-2023.06",
            },
        }
    )
    assert req_str.input_data is not None
    assert req_str.input_data.education == "某大学 本科 计算机科学 2019.09-2023.06"
    md_str = assemble_resume_markdown(req_str.input_data.model_dump(exclude_none=True))
    assert "教育经历" in md_str
    assert "某大学 本科 计算机科学 2019.09-2023.06" in md_str

    req_list = AnalyzeRequest.model_validate(
        {
            "task_type": "score_only",
            "input_data": {
                "personal_info": {"name": "乙"},
                "education": [
                    {
                        "school": "某大学",
                        "degree": "本科",
                        "major": "计算机科学",
                        "start_date": "2019.09",
                        "end_date": "2023.06",
                    }
                ],
            },
        }
    )
    assert req_list.input_data is not None
    assert isinstance(req_list.input_data.education, list)
    assert req_list.input_data.education[0].school == "某大学"
    md_list = assemble_resume_markdown(req_list.input_data.model_dump(exclude_none=True))
    assert "教育经历" in md_list
    assert "某大学" in md_list


def test_raw_content_is_ignored_when_structured_fields_exist():
    req = AnalyzeRequest.model_validate(
        {
            "task_type": "score_only",
            "input_data": {
                "rawContent": "乱码或低质量原文",
                "BASIC_INFO": {"name": "李四"},
            },
        }
    )
    assert req.input_data is not None
    assert req.input_data.resume_text is None
    assert req.input_data.personal_info is not None
    assert req.input_data.personal_info.name == "李四"


def test_empty_input_data_is_rejected():
    with pytest.raises(ValidationError):
        AnalyzeRequest.model_validate({"task_type": "score_only", "input_data": {}})


def test_unknown_input_data_section_is_rejected():
    with pytest.raises(ValidationError):
        AnalyzeRequest.model_validate(
            {"task_type": "score_only", "input_data": {"UNKNOWN_SECTION": "x"}}
        )


def test_raw_content_only_is_rejected():
    with pytest.raises(ValidationError):
        AnalyzeRequest.model_validate(
            {
                "task_type": "score_only",
                "input_data": {"rawContent": "仅有原始切割文本"},
            }
        )


def test_jd_only_allows_missing_resume():
    req = AnalyzeRequest.model_validate(
        {"task_type": "interview_jd", "jd_text": "后端开发岗位 JD"}
    )
    assert req.task_type == "interview_jd"


def test_assemble_resume_markdown_returns_empty_for_meaningless_payload():
    assert has_meaningful_resume_data({}) is False
    assert assemble_resume_markdown({}) == ""
