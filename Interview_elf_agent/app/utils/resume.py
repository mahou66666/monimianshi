from typing import Any, Dict, List


def _format_date_range(start: Any, end: Any) -> str:
    if start and end:
        return f"{start} - {end}"
    if start and not end:
        return f"{start} - 至今"
    return ""


def _format_personal_info(info: Dict[str, Any]) -> str:
    label_map = [
        ("name", "姓名"),
        ("phone", "电话"),
        ("email", "邮箱"),
        ("location", "所在地"),
        ("website", "网站"),
        ("linkedin", "LinkedIn"),
        ("github", "GitHub"),
        ("age", "年龄"),
        ("gender", "性别"),
        ("university", "毕业院校"),
        ("degree", "学历"),
        ("job_intention", "求职意向"),
    ]
    lines: List[str] = []
    for key, label in label_map:
        value = info.get(key)
        if value:
            lines.append(f"{label}：{value}")
    summary = info.get("summary")
    if summary:
        lines.append(f"简介：{summary}")
    return "\n".join(lines)


def _format_education(items: List[Dict[str, Any]]) -> str:
    lines: List[str] = []
    for idx, item in enumerate(items, start=1):
        school = item.get("school", "")
        degree = item.get("degree")
        major = item.get("major")
        time_range = _format_date_range(item.get("start_date"), item.get("end_date"))
        title = f"{idx}. {school}" if school else f"{idx}."
        if time_range:
            title += f" ({time_range})"
        lines.append(title)
        detail_parts = []
        if degree:
            detail_parts.append(f"学历：{degree}")
        if major:
            detail_parts.append(f"专业：{major}")
        if item.get("gpa"):
            detail_parts.append(f"GPA：{item.get('gpa')}")
        if detail_parts:
            lines.append("；".join(detail_parts))
        if item.get("description"):
            lines.append(str(item["description"]))
        lines.append("")
    return "\n".join(lines).strip()


def _format_experiences(items: List[Dict[str, Any]]) -> str:
    lines: List[str] = []
    for idx, item in enumerate(items, start=1):
        company = item.get("company", "")
        position = item.get("position")
        location = item.get("location")
        time_range = _format_date_range(item.get("start_date"), item.get("end_date"))
        title = f"{idx}. {company}" if company else f"{idx}."
        if time_range:
            title += f" ({time_range})"
        lines.append(title)
        detail_parts = []
        if position:
            detail_parts.append(f"岗位：{position}")
        if location:
            detail_parts.append(f"地点：{location}")
        if detail_parts:
            lines.append("；".join(detail_parts))
        if item.get("description"):
            lines.append(str(item["description"]))
        highlights = item.get("highlights") or []
        for h in highlights:
            lines.append(f"- {h}")
        lines.append("")
    return "\n".join(lines).strip()


def _format_projects(items: List[Dict[str, Any]]) -> str:
    lines: List[str] = []
    for idx, item in enumerate(items, start=1):
        name = item.get("name", "")
        role = item.get("role")
        time_range = _format_date_range(item.get("start_date"), item.get("end_date"))
        title = f"{idx}. {name}" if name else f"{idx}."
        if role:
            title += f" ({role})"
        if time_range:
            title += f" | {time_range}"
        lines.append(title)
        if item.get("description"):
            lines.append(str(item["description"]))
        tech_stack = item.get("tech_stack") or []
        if tech_stack:
            lines.append("技术栈：" + ", ".join([str(t) for t in tech_stack]))
        highlights = item.get("highlights") or []
        for h in highlights:
            lines.append(f"- {h}")
        if item.get("link"):
            lines.append(f"链接：{item.get('link')}")
        lines.append("")
    return "\n".join(lines).strip()


def _format_skills(items: List[Dict[str, Any]]) -> str:
    lines: List[str] = []
    for item in items:
        if isinstance(item, str):
            text = item.strip()
            if text:
                lines.append(text)
            continue
        if not isinstance(item, dict):
            continue
        name = item.get("name")
        skills = item.get("items") or []
        if isinstance(skills, str):
            skills = [skills]
        if name and skills:
            lines.append(f"{name}：{', '.join([str(s) for s in skills])}")
        elif skills:
            lines.append(", ".join([str(s) for s in skills]))
        elif name:
            lines.append(str(name))
    return "\n".join(lines).strip()


def _format_awards(items: List[Dict[str, Any]]) -> str:
    lines: List[str] = []
    for idx, item in enumerate(items, start=1):
        title = item.get("title", "")
        issuer = item.get("issuer")
        date = item.get("date")
        line = f"{idx}. {title}" if title else f"{idx}."
        detail_parts = []
        if issuer:
            detail_parts.append(str(issuer))
        if date:
            detail_parts.append(str(date))
        if detail_parts:
            line += f" ({' / '.join(detail_parts)})"
        lines.append(line)
        if item.get("description"):
            lines.append(str(item["description"]))
        lines.append("")
    return "\n".join(lines).strip()


def _format_campus(items: List[Dict[str, Any]]) -> str:
    lines: List[str] = []
    for idx, item in enumerate(items, start=1):
        org = item.get("organization", "")
        role = item.get("role")
        time_range = _format_date_range(item.get("start_date"), item.get("end_date"))
        title = f"{idx}. {org}" if org else f"{idx}."
        if time_range:
            title += f" ({time_range})"
        lines.append(title)
        if role:
            lines.append(f"角色：{role}")
        if item.get("description"):
            lines.append(str(item["description"]))
        highlights = item.get("highlights") or []
        for h in highlights:
            lines.append(f"- {h}")
        lines.append("")
    return "\n".join(lines).strip()


def _format_self_evaluation(value: Any) -> str:
    return str(value).strip() if value is not None else ""


def _has_real_content(value: Any) -> bool:
    if value is None:
        return False
    if isinstance(value, str):
        return bool(value.strip())
    if isinstance(value, dict):
        return any(_has_real_content(v) for v in value.values())
    if isinstance(value, list):
        return any(_has_real_content(v) for v in value)
    return True


def has_meaningful_resume_data(data: Dict[str, Any]) -> bool:
    if not isinstance(data, dict):
        return _has_real_content(data)

    if _has_real_content(data.get("resume_text")):
        return True
    for key in (
        "personal_info",
        "education",
        "work_history",
        "internship_history",
        "project_experience",
        "skills",
        "campus_experience",
        "awards",
        "self_evaluation",
    ):
        if _has_real_content(data.get(key)):
            return True
    return False


def assemble_resume_markdown(data: Dict[str, Any]) -> str:
    """
    将结构化字典组装为 Markdown 简历文本。
    """
    if not isinstance(data, dict):
        return str(data).strip()

    if data.get("resume_text"):
        return str(data["resume_text"]).strip()

    sections = [
        ("personal_info", "个人信息", _format_personal_info),
        ("education", "教育经历", _format_education),
        ("work_history", "工作经历", _format_experiences),
        ("internship_history", "实习经历", _format_experiences),
        ("project_experience", "项目经历", _format_projects),
        ("skills", "个人能力", _format_skills),
        ("campus_experience", "校园经历", _format_campus),
        ("awards", "获奖记录", _format_awards),
        ("self_evaluation", "自我评价", _format_self_evaluation),
    ]

    section_blocks: List[str] = []
    for key, title, formatter in sections:
        content = data.get(key)
        if not content:
            continue
        if isinstance(content, str):
            body = content.strip()
        elif isinstance(content, dict):
            body = formatter(content)
        elif isinstance(content, list):
            body = formatter(list(content))
        else:
            body = str(content).strip()
        if body and body != "无":
            section_blocks.append(f"## {title}\n{body}")

    if not section_blocks:
        return ""
    return "# 候选人简历\n---\n" + "\n\n".join(section_blocks)
