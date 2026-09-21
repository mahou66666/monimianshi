import os
from pathlib import Path
from typing import Any

PROJECT_ROOT = Path(__file__).resolve().parent.parent

RAG_STATE_FIELDS = (
    "rag_enabled",
    "rag_index_dir",
    "rag_top_k",
    "rag_max_context_chars",
    "rag_query_template",
    "allow_dangerous_faiss_deserialization",
)


def _parse_bool(raw_value: Any, field_name: str) -> bool:
    if isinstance(raw_value, bool):
        return raw_value
    if isinstance(raw_value, int):
        if raw_value in {0, 1}:
            return bool(raw_value)
        raise ValueError(f"{field_name} 必须是布尔值。")
    if isinstance(raw_value, str):
        normalized = raw_value.strip().lower()
        if normalized in {"1", "true", "yes", "y", "on"}:
            return True
        if normalized in {"0", "false", "no", "n", "off"}:
            return False
    raise ValueError(f"{field_name} 必须是布尔值。")


def _parse_int(raw_value: Any, field_name: str, min_value: int) -> int:
    try:
        parsed = int(raw_value)
    except (TypeError, ValueError):
        raise ValueError(f"{field_name} 必须是整数。") from None
    if parsed < min_value:
        raise ValueError(f"{field_name} 必须大于等于 {min_value}。")
    return parsed


def _env_bool(name: str, default: bool) -> bool:
    raw_value = os.getenv(name)
    if raw_value is None:
        return default
    try:
        return _parse_bool(raw_value, name)
    except ValueError:
        return default


def _env_int(name: str, default: int, min_value: int) -> int:
    raw_value = os.getenv(name, "").strip()
    if not raw_value:
        return default
    try:
        return _parse_int(raw_value, name, min_value)
    except ValueError:
        return default


def default_rag_config() -> dict[str, Any]:
    return {
        "rag_enabled": _env_bool("RAG_ENABLED", True),
        "rag_index_dir": os.getenv("RAG_INDEX_DIR", "memory/knowledge_index").strip() or "memory/knowledge_index",
        "rag_top_k": _env_int("RAG_TOP_K", 2, min_value=1),
        "rag_max_context_chars": _env_int("RAG_MAX_CONTEXT_CHARS", 1200, min_value=200),
        "rag_query_template": os.getenv("RAG_QUERY_TEMPLATE", "针对行业：{industry}。候选人提及：{answer}"),
        "allow_dangerous_faiss_deserialization": _env_bool("ALLOW_DANGEROUS_FAISS_DESERIALIZATION", False),
    }


def resolve_rag_index_path(raw_path: str) -> Path:
    normalized = (raw_path or "").strip()
    if not normalized:
        normalized = "memory/knowledge_index"
    resolved = Path(normalized).expanduser()
    if not resolved.is_absolute():
        resolved = PROJECT_ROOT / resolved
    return resolved


def extract_rag_overrides(payload: dict[str, Any]) -> dict[str, Any]:
    overrides: dict[str, Any] = {}

    if "rag_enabled" in payload and payload.get("rag_enabled") is not None:
        overrides["rag_enabled"] = _parse_bool(payload.get("rag_enabled"), "rag_enabled")
    if "rag_index_dir" in payload and payload.get("rag_index_dir") is not None:
        overrides["rag_index_dir"] = str(payload.get("rag_index_dir")).strip()
    if "rag_top_k" in payload and payload.get("rag_top_k") is not None:
        overrides["rag_top_k"] = _parse_int(payload.get("rag_top_k"), "rag_top_k", min_value=1)
    if "rag_max_context_chars" in payload and payload.get("rag_max_context_chars") is not None:
        overrides["rag_max_context_chars"] = _parse_int(
            payload.get("rag_max_context_chars"),
            "rag_max_context_chars",
            min_value=200,
        )
    if "rag_query_template" in payload and payload.get("rag_query_template") is not None:
        overrides["rag_query_template"] = str(payload.get("rag_query_template"))
    if (
        "allow_dangerous_faiss_deserialization" in payload
        and payload.get("allow_dangerous_faiss_deserialization") is not None
    ):
        overrides["allow_dangerous_faiss_deserialization"] = _parse_bool(
            payload.get("allow_dangerous_faiss_deserialization"),
            "allow_dangerous_faiss_deserialization",
        )

    return overrides


def build_effective_rag_config(state_values: dict[str, Any]) -> dict[str, Any]:
    defaults = default_rag_config()
    config = dict(defaults)
    for field in RAG_STATE_FIELDS:
        if field not in state_values:
            continue
        value = state_values.get(field)
        if value is None:
            continue
        config[field] = value

    try:
        config["rag_enabled"] = _parse_bool(config.get("rag_enabled"), "rag_enabled")
    except ValueError:
        config["rag_enabled"] = defaults["rag_enabled"]
    try:
        config["allow_dangerous_faiss_deserialization"] = _parse_bool(
            config.get("allow_dangerous_faiss_deserialization"),
            "allow_dangerous_faiss_deserialization",
        )
    except ValueError:
        config["allow_dangerous_faiss_deserialization"] = defaults["allow_dangerous_faiss_deserialization"]
    try:
        config["rag_top_k"] = _parse_int(config.get("rag_top_k"), "rag_top_k", min_value=1)
    except ValueError:
        config["rag_top_k"] = defaults["rag_top_k"]
    try:
        config["rag_max_context_chars"] = _parse_int(
            config.get("rag_max_context_chars"),
            "rag_max_context_chars",
            min_value=200,
        )
    except ValueError:
        config["rag_max_context_chars"] = defaults["rag_max_context_chars"]
    config["rag_index_dir"] = str(config.get("rag_index_dir", "")).strip() or "memory/knowledge_index"
    config["rag_query_template"] = str(config.get("rag_query_template", "")).strip() or "针对行业：{industry}。候选人提及：{answer}"
    config["rag_index_dir_resolved"] = str(resolve_rag_index_path(config["rag_index_dir"]))
    return config
