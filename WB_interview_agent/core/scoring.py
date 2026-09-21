import json
import re
from datetime import datetime
from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage

from core.config import reasoning_llm
from core.state import RoundScore

JSON_BLOCK_RE = re.compile(r"\{.*\}", re.DOTALL)

TECH_DEFAULT_COMPETENCY = "系统设计与工程实现"
HR_DEFAULT_COMPETENCY = "沟通协作与执行力"
ROUND_SCORE_HEADER = "【本轮结构化评分】"


def extract_interviewer_prompt(content: str) -> str:
    text = (content or "").strip()
    if not text:
        return ""
    if text.startswith(ROUND_SCORE_HEADER):
        parts = [part.strip() for part in text.split("\n\n") if part.strip()]
        return parts[-1] if parts else ""
    return text


def extract_last_human_answer(messages: list) -> str:
    for msg in reversed(messages):
        if getattr(msg, "type", "") == "human" or isinstance(msg, HumanMessage):
            return getattr(msg, "content", "").strip()
    return ""


def extract_last_ai_question_before_answer(messages: list) -> str:
    seen_human = False
    for msg in reversed(messages):
        msg_type = getattr(msg, "type", "")
        if not seen_human and msg_type == "human":
            seen_human = True
            continue
        if seen_human and msg_type == "ai":
            return extract_interviewer_prompt(getattr(msg, "content", ""))
    return ""


def _clamp_score(raw_score: Any) -> float:
    try:
        score = float(raw_score)
    except (TypeError, ValueError):
        score = 6.0
    return round(max(0.0, min(10.0, score)), 1)


def _extract_json_payload(text: str) -> dict:
    if not text:
        return {}
    text = text.strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        match = JSON_BLOCK_RE.search(text)
        if not match:
            return {}
        try:
            return json.loads(match.group(0))
        except json.JSONDecodeError:
            return {}


def _heuristic_score(answer: str, evaluator: str) -> tuple[str, float, str]:
    answer = answer.strip()
    answer_len = len(answer)
    score = 4.5
    if answer_len >= 20:
        score = 6.0
    if answer_len >= 60:
        score = 7.2
    if any(token in answer for token in ["指标", "压测", "监控", "复盘", "数据", "结果"]):
        score += 0.8
    if evaluator == "hr" and any(token in answer for token in ["团队", "协作", "冲突", "沟通", "反馈"]):
        score += 0.6
    competency = TECH_DEFAULT_COMPETENCY if evaluator == "tech" else HR_DEFAULT_COMPETENCY
    evidence = f"候选人回答要点：{answer[:80] or '回答较短，缺少可验证细节。'}"
    return competency, _clamp_score(score), evidence


def build_round_score(
    *,
    evaluator: str,
    round_id: int,
    candidate_answer: str,
    question_excerpt: str,
    context: str,
) -> RoundScore:
    default_competency, default_score, default_evidence = _heuristic_score(candidate_answer, evaluator)
    if not candidate_answer.strip():
        return {
            "round_id": round_id,
            "evaluator": evaluator,
            "competency": default_competency,
            "score": 0.0,
            "evidence": "本轮未获取到候选人的有效回答。",
            "answer_excerpt": "",
            "question_excerpt": question_excerpt[:160],
        }

    prompt = """
你是一名严格的面试评分器。请根据提供的问题与候选人回答，输出一个 JSON：
{
  "competency": "本轮最相关的能力项，简短中文",
  "score": 0-10 的数字,
  "evidence": "必须引用回答中的具体行为/指标作为证据"
}
限制：
1. 只能输出 JSON，禁止任何额外文本。
2. score 允许 1 位小数。
3. evidence 不超过 80 字。
"""
    user_input = (
        f"面试官类型: {evaluator}\n"
        f"上下文: {context[:500]}\n"
        f"问题: {question_excerpt[:200] or '无'}\n"
        f"候选人回答: {candidate_answer[:800]}"
    )
    competency = default_competency
    score = default_score
    evidence = default_evidence
    try:
        llm_response = reasoning_llm.invoke(
            [
                SystemMessage(content=prompt),
                HumanMessage(content=user_input),
            ]
        )
        parsed = _extract_json_payload(getattr(llm_response, "content", ""))
        competency = str(parsed.get("competency") or competency).strip()[:40] or competency
        score = _clamp_score(parsed.get("score", score))
        evidence = str(parsed.get("evidence") or evidence).strip()[:120] or evidence
    except Exception:
        pass

    return {
        "round_id": round_id,
        "evaluator": evaluator,
        "competency": competency,
        "score": score,
        "evidence": evidence,
        "answer_excerpt": candidate_answer[:160],
        "question_excerpt": question_excerpt[:160],
    }


def format_round_score(score: RoundScore) -> str:
    return (
        "【本轮结构化评分】\n"
        f"competency: {score['competency']}\n"
        f"score: {score['score']}/10\n"
        f"evidence: {score['evidence']}"
    )


def build_score_summary(round_scores: list[RoundScore]) -> dict:
    if not round_scores:
        return {
            "total_rounds": 0,
            "overall_score": 0.0,
            "competencies": [],
            "strengths": [],
            "risks": ["暂无有效评分证据，建议补充面试轮次。"],
            "offer_recommendation": "信息不足，建议补面后再决策",
            "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        }

    competency_map: dict[str, dict] = {}
    for item in round_scores:
        name = str(item.get("competency") or "未分类能力项")
        bucket = competency_map.setdefault(name, {"scores": [], "evidences": []})
        bucket["scores"].append(_clamp_score(item.get("score")))
        evidence = str(item.get("evidence") or "").strip()
        if evidence:
            bucket["evidences"].append(evidence)

    competency_items = []
    all_scores = []
    for name, data in competency_map.items():
        avg_score = round(sum(data["scores"]) / len(data["scores"]), 1)
        all_scores.extend(data["scores"])
        competency_items.append(
            {
                "competency": name,
                "avg_score": avg_score,
                "rounds": len(data["scores"]),
                "evidence_samples": data["evidences"][:2],
            }
        )
    competency_items.sort(key=lambda x: x["avg_score"], reverse=True)

    overall_score = round(sum(all_scores) / len(all_scores), 1)
    strengths = [item["competency"] for item in competency_items if item["avg_score"] >= 7.0][:3]
    risks = [item["competency"] for item in competency_items if item["avg_score"] < 6.0][:3]

    if overall_score >= 7.5 and not risks:
        recommendation = "建议发放 Offer"
    elif overall_score >= 6.0:
        recommendation = "建议进入下一轮复试"
    else:
        recommendation = "暂不建议发放 Offer"

    return {
        "total_rounds": len(round_scores),
        "overall_score": overall_score,
        "competencies": competency_items,
        "strengths": strengths,
        "risks": risks,
        "offer_recommendation": recommendation,
        "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    }


def render_structured_report(candidate_name: str, summary: dict) -> str:
    lines = [
        "【结构化评估总览】",
        f"- 候选人: {candidate_name}",
        f"- 证据轮次: {summary.get('total_rounds', 0)}",
        f"- 综合得分: {summary.get('overall_score', 0.0)}/10",
        f"- Offer建议: {summary.get('offer_recommendation', '待定')}",
        "",
        "【能力项评分】",
    ]
    competency_items = summary.get("competencies", [])
    if competency_items:
        for item in competency_items:
            evidence_text = "；".join(item.get("evidence_samples", [])[:2]) or "无"
            lines.append(
                f"- {item['competency']}: {item['avg_score']}/10 (轮次: {item['rounds']}) | 证据: {evidence_text}"
            )
    else:
        lines.append("- 暂无结构化评分数据。")

    strengths = summary.get("strengths") or ["暂无明确优势项"]
    risks = summary.get("risks") or ["暂无明显风险项"]
    lines.extend(
        [
            "",
            f"【核心优势】{'、'.join(strengths)}",
            f"【风险与短板】{'、'.join(risks)}",
        ]
    )
    return "\n".join(lines)
