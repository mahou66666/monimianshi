# Interview_elf_agent/app/core/config.py

import os
from dotenv import load_dotenv

load_dotenv()

# ... (保留原有的 _get_float, _get_int, _get_bool 函数) ...
def _get_float(name: str, default: float) -> float:
    raw = os.getenv(name)
    if raw is None or raw == "":
        return default
    try:
        return float(raw)
    except ValueError:
        return default


def _get_int(name: str, default: int | None) -> int | None:
    raw = os.getenv(name)
    if raw is None or raw == "":
        return default
    try:
        return int(raw)
    except ValueError:
        return default


def _get_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None or raw == "":
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}

class Settings:
    # ... (保留原有的 OPENAI_API_KEY, BASE_URL 等配置) ...
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    BASE_URL = os.getenv("BASE_URL", "https://api.siliconflow.cn/v1")
    MODEL_NAME = os.getenv("MODEL_NAME", "deepseek-ai/DeepSeek-V4-Flash")

    # 模型参数
    TEMPERATURE = _get_float("TEMPERATURE", 0.0)
    TOP_P = _get_float("TOP_P", 1.0)
    TOP_K = _get_int("TOP_K", None)
    FREQUENCY_PENALTY = _get_float("FREQUENCY_PENALTY", 0.0)
    ENABLE_THINKING = _get_bool("ENABLE_THINKING", False)
    QUESTION_TEMPERATURE = _get_float("QUESTION_TEMPERATURE", 0.5)

    # 接口安全
    API_KEY = os.getenv("API_KEY", "")
    REQUIRE_API_KEY = _get_bool("REQUIRE_API_KEY", True)

    # 并发与稳定性
    MAX_CONCURRENCY = _get_int("MAX_CONCURRENCY", 4) or 4
    # [修改] 增加超时时间到 600 秒，防止模型加载或长推理导致 Timeout
    TASK_TIMEOUT_SECONDS = _get_int("TASK_TIMEOUT_SECONDS", 1200) or 1200
    RETRY_MAX = _get_int("RETRY_MAX", 2) or 2
    RETRY_BACKOFF_SECONDS = _get_float("RETRY_BACKOFF_SECONDS", 1.0)
    CB_FAILURE_THRESHOLD = _get_int("CB_FAILURE_THRESHOLD", 5) or 5
    CB_RECOVERY_SECONDS = _get_int("CB_RECOVERY_SECONDS", 30) or 30
    
    # --- [新增] RAG 与 模型配置 ---
    # Qdrant 地址 (Docker 内部互联用 'qdrant'，本地调试用 'localhost')
    QDRANT_HOST = os.getenv("QDRANT_HOST", "qdrant")
    QDRANT_PORT = _get_int("QDRANT_PORT", 6333)
    COLLECTION_NAME = os.getenv("COLLECTION_NAME", "interview_kb")
    QDRANT_WAIT_ON_STARTUP = _get_bool("QDRANT_WAIT_ON_STARTUP", False)
    QDRANT_WAIT_TIMEOUT_SECONDS = _get_int("QDRANT_WAIT_TIMEOUT_SECONDS", 30) or 30
    QDRANT_WAIT_INTERVAL_SECONDS = _get_float("QDRANT_WAIT_INTERVAL_SECONDS", 1.0)
    RAG_TOP_K = _get_int("RAG_TOP_K", 5) or 5
    RAG_SCORE_THRESHOLD = _get_float("RAG_SCORE_THRESHOLD", 0.0)
    RAG_MAX_CONTEXT_CHARS = _get_int("RAG_MAX_CONTEXT_CHARS", 2500) or 2500
    MIN_INTERVIEW_QUESTIONS = _get_int("MIN_INTERVIEW_QUESTIONS", 40) or 40

    # 简历评分权重（可在 .env 中调整）
    RESUME_WEIGHT_EDUCATION = _get_float("RESUME_WEIGHT_EDUCATION", 0.15)
    RESUME_WEIGHT_WORK = _get_float("RESUME_WEIGHT_WORK", 0.25)
    RESUME_WEIGHT_PROJECT = _get_float("RESUME_WEIGHT_PROJECT", 0.20)
    RESUME_WEIGHT_SKILL = _get_float("RESUME_WEIGHT_SKILL", 0.20)
    RESUME_WEIGHT_AWARD = _get_float("RESUME_WEIGHT_AWARD", 0.05)
    RESUME_WEIGHT_JOB_FIT = _get_float("RESUME_WEIGHT_JOB_FIT", 0.15)

    # 简历总分正态化控制（默认启用）
    RESUME_SCORE_NORMALIZE = _get_bool("RESUME_SCORE_NORMALIZE", True)
    RESUME_SCORE_CENTER = _get_float("RESUME_SCORE_CENTER", 75.0)
    RESUME_SCORE_SPREAD = _get_float("RESUME_SCORE_SPREAD", 15.0)
    RESUME_SCORE_MIN = _get_float("RESUME_SCORE_MIN", 55.0)
    RESUME_SCORE_MAX = _get_float("RESUME_SCORE_MAX", 95.0)

    # 路径配置
    BASE_DIR: str = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    MODELS_DIR: str = os.path.join(BASE_DIR, "models")
    
    # Embedding 模型
    EMBEDDING_MODEL_NAME: str = os.getenv("EMBEDDING_MODEL_NAME", "BAAI/bge-large-zh-v1.5")
    EMBEDDING_MODEL_PATH: str = os.getenv("EMBEDDING_MODEL_PATH", "")

    # Hugging Face 下载与缓存配置
    HF_ENDPOINT: str = os.getenv("HF_ENDPOINT", "")
    HF_HOME: str = os.getenv("HF_HOME", "")
    HF_CACHE_DIR: str = os.getenv("HF_CACHE_DIR", "")
    HF_HUB_OFFLINE: bool = _get_bool("HF_HUB_OFFLINE", False)

    MODEL_KWARGS = {}
    if TOP_K is not None:
        MODEL_KWARGS["top_k"] = TOP_K
    if ENABLE_THINKING:
        MODEL_KWARGS["enable_thinking"] = True

settings = Settings()
