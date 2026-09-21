import math
import os

from core.config import embeddings_model
from core.scoring import extract_interviewer_prompt

SIMILARITY_THRESHOLD = float(os.getenv("QUESTION_DEDUP_THRESHOLD", "0.90"))
RECENT_AI_WINDOW = max(1, int(os.getenv("QUESTION_DEDUP_WINDOW", "6")))

_SEMANTIC_DISABLED = False
_SEMANTIC_WARNING_SHOWN = False


def _cosine_similarity(vec_a: list[float], vec_b: list[float]) -> float:
    numerator = sum(a * b for a, b in zip(vec_a, vec_b))
    norm_a = math.sqrt(sum(a * a for a in vec_a))
    norm_b = math.sqrt(sum(b * b for b in vec_b))
    if norm_a == 0.0 or norm_b == 0.0:
        return 0.0
    return numerator / (norm_a * norm_b)


def _recent_ai_questions(messages: list) -> list[str]:
    questions = []
    for msg in reversed(messages):
        if getattr(msg, "type", "") == "ai":
            content = extract_interviewer_prompt(getattr(msg, "content", ""))
            if content:
                questions.append(content)
            if len(questions) >= RECENT_AI_WINDOW:
                break
    questions.reverse()
    return questions


def is_semantically_duplicate_question(question: str, messages: list, threshold: float = SIMILARITY_THRESHOLD) -> bool:
    global _SEMANTIC_DISABLED, _SEMANTIC_WARNING_SHOWN

    normalized = (question or "").strip()
    if not normalized:
        return True

    history = _recent_ai_questions(messages)
    if not history:
        return False
    if normalized in history:
        return True
    if _SEMANTIC_DISABLED:
        return False

    try:
        vectors = embeddings_model.embed_documents([normalized] + history)
        base_vector = vectors[0]
        for previous_vector in vectors[1:]:
            similarity = _cosine_similarity(base_vector, previous_vector)
            if similarity >= threshold:
                return True
        return False
    except Exception as e:
        if not _SEMANTIC_WARNING_SHOWN:
            print(f"[WARN] Semantic dedup fallback to exact match mode: {e}")
            _SEMANTIC_WARNING_SHOWN = True
        _SEMANTIC_DISABLED = True
        return False
