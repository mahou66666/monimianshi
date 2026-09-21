import json
import hashlib
import os
import re
from datetime import datetime
from pathlib import Path
from langchain_core.messages import AIMessage
from core.state import InterviewState
from core.scoring import build_score_summary, render_structured_report

PROJECT_ROOT = Path(__file__).resolve().parent.parent
ARCHIVE_DIR = PROJECT_ROOT / "archives"
FILENAME_SANITIZER = re.compile(r"[^0-9A-Za-z_\-\u4e00-\u9fff]")
EMAIL_RE = re.compile(r"([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})")
PHONE_RE = re.compile(r"(?<!\d)(?:\+?\d[\d\-\s]{6,}\d)(?!\d)")
URL_RE = re.compile(r"https?://\S+")
IP_RE = re.compile(r"(?<!\d)(?:\d{1,3}\.){3}\d{1,3}(?!\d)")
LONG_NUMBER_RE = re.compile(r"(?<!\d)\d{8,}(?!\d)")
TRUE_VALUES = {"1", "true", "yes", "on"}


def sanitize_filename(value: str) -> str:
    safe_value = FILENAME_SANITIZER.sub("_", value.strip()).strip("._")
    return safe_value[:64] or "candidate"


def _env_flag(name: str, default: bool) -> bool:
    raw_value = os.getenv(name)
    if raw_value is None:
        return default
    return raw_value.strip().lower() in TRUE_VALUES


def _session_alias(session_id: str) -> str:
    normalized = (session_id or "session").encode("utf-8")
    return f"candidate_{hashlib.sha256(normalized).hexdigest()[:10]}"


def _redact_text(text: str, candidate_name: str, alias: str) -> str:
    if not isinstance(text, str):
        return text
    redacted = text
    if candidate_name:
        redacted = redacted.replace(candidate_name, alias)
    redacted = EMAIL_RE.sub("[REDACTED_EMAIL]", redacted)
    redacted = PHONE_RE.sub("[REDACTED_PHONE]", redacted)
    redacted = URL_RE.sub("[REDACTED_URL]", redacted)
    redacted = IP_RE.sub("[REDACTED_IP]", redacted)
    redacted = LONG_NUMBER_RE.sub("[REDACTED_NUMBER]", redacted)
    return redacted


def _redact_payload(payload, candidate_name: str, alias: str):
    if isinstance(payload, dict):
        return {key: _redact_payload(value, candidate_name, alias) for key, value in payload.items()}
    if isinstance(payload, list):
        return [_redact_payload(item, candidate_name, alias) for item in payload]
    if isinstance(payload, str):
        return _redact_text(payload, candidate_name, alias)
    return payload


def run_reporter(state: InterviewState) -> dict:
    print("\n[INFO] Generating multi-dimensional interview report and archive...")
    session_id = state.get("session_id", "session")
    candidate_name = state.get("candidate_name", "未知候选人")
    industry = state.get("target_industry", "未指定")
    question_count = state.get("question_count", 0)
    messages = list(state.get("messages", []))
    round_scores = list(state.get("round_scores", []))
    summary = build_score_summary(round_scores)
    report_content = render_structured_report(candidate_name, summary)

    archive_enabled = _env_flag("ARCHIVE_REPORTS_ENABLED", False)
    archive_include_raw_chat = _env_flag("ARCHIVE_INCLUDE_RAW_CHAT", False)
    archive_redact_sensitive = _env_flag("ARCHIVE_REDACT_SENSITIVE", True)
    alias = _session_alias(session_id)
    archive_status = "归档已关闭。"

    if archive_enabled:
        now = datetime.now()
        archive_data = {
            "candidate_name": alias if archive_redact_sensitive else candidate_name,
            "industry": industry,
            "session_alias": alias,
            "timestamp": now.strftime("%Y-%m-%d %H:%M:%S"),
            "total_rounds": question_count,
            "score_summary": summary,
            "round_scores": round_scores,
            "report": report_content,
            "raw_chat": (
                [{"role": getattr(m, "type", "unknown"), "content": getattr(m, "content", "")} for m in messages]
                if archive_include_raw_chat
                else []
            ),
        }
        if archive_redact_sensitive:
            archive_data = _redact_payload(archive_data, candidate_name, alias)

        ARCHIVE_DIR.mkdir(parents=True, exist_ok=True)
        safe_session_id = sanitize_filename(alias)
        filename = ARCHIVE_DIR / f"{safe_session_id}_{now.strftime('%Y%m%d%H%M%S%f')}.json"
        try:
            with filename.open("w", encoding="utf-8") as f:
                json.dump(archive_data, f, ensure_ascii=False, indent=4)
            archive_status = f"归档已保存为脱敏文件：{filename.name}"
        except OSError as e:
            print(f"[WARN] Failed to write report archive: {e}")
            archive_status = "归档写入失败。"

    final_farewell = (
        "面试结束。感谢你的时间！后续我们的HR会与您联系。\n\n"
        f"【幕后生成的评估报告预览】：\n{report_content}\n\n【归档状态】{archive_status}"
    )

    return {
        "messages": [AIMessage(content=final_farewell)],
        "interview_completed": True,
        "awaiting_answer": False,
        "round_feedback": "",
    }
