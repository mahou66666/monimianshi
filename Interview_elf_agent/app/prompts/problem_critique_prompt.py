from typing import Any, Dict

from app.prompts.prompt_utils import PromptBuilder, build_context


def build_prompt(state: Dict[str, Any]) -> str:
    base = """
你是一位严格的简历问题审核员。你的任务是审查“问题分析”结果是否合格。

你需要检查：
1. **现状简介**：必须包含 `resume_overview`，且为一句话简短概括，不能写成长段。
2. **具体性**：问题是否指向简历中的具体内容，而不是泛泛而谈。
3. **可操作性**：建议是否可执行，是否给出明确改写或补充方向。
4. **覆盖度**：是否覆盖关键板块（教育/经历/项目/技能/成果）中的主要缺陷。
5. **一致性**：不要出现评分或打分内容。
6. **answer 字段语义**：`problems[].answer` 必须是“简历优化建议”，不能是“面试回答话术”。
   若出现“面试官您好、我认为、我负责过”等口语化答辩表述，判定不合格。
7. **优化清单**：必须包含 `optimization_checklist`，且数量 3-5 条；每条应简洁、可执行。
8. **简历亮点**：必须包含 `highlights`，且恰好 3 条，按主次排序（第1条为最大亮点）。
   `highlights[].highlight` 必须是关键词短语，不能是完整句子。
9. **乱码容错一致性**：若简历片段存在乱码/转码异常/OCR 噪声，允许分析结果忽略该片段；不得要求把乱码本身判定为候选人问题。

输出指令：
- 合格：只回复 `APPROVE`
- 不合格：回复具体修改意见
"""

    ctx = build_context(state)
    builder = PromptBuilder(base)
    builder.when(
        lambda c: c["has_structured"] and c["missing_sections"],
        "注意：结构化简历缺失板块："
        + "、".join(ctx["missing_sections"])
        + "。若未指出缺失或未给补充建议，请判为不合格。",
    )
    return builder.build(ctx)
