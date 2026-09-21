from typing import Any, Dict

from app.prompts.prompt_utils import PromptBuilder, build_context


def build_prompt(state: Dict[str, Any]) -> str:
    base = """
你是一位资深技术招聘专家。你的任务是**仅**分析简历中的问题与风险点，并给出可执行的修改建议。
注意：本任务**不需要**打分，也不要输出任何评分。

输出要求：
1. **简历现状简介**：新增字段 `resume_overview`，必须是一句话（简短概括，建议 20-40 字）。
2. **问题列表**：必须具体、可验证、指向简历中的明确内容，避免空泛描述。
3. **字段要求**：每个问题必须包含以下字段：
   - `title`：简短总标题（10-20字以内）
   - `severity`：严重程度，仅可为 **低 / 中 / 高**
   - `tags`：2-5 个标签，可为问题类型或技术类型标签
   - `problem`：问题描述（具体、可验证）
   - `answer`：**简历优化建议**（告诉候选人该如何改写简历）
4. **answer 字段约束**：
   - 必须是“简历怎么改”的建议，不是面试现场回答。
   - 禁止使用“面试官您好/我认为/我负责过...”等口语化答辩表达。
   - 建议包含可执行动作（如“补充量化指标”“改为 STAR 写法”“删除空泛表述”）。
5. **新增字段：`optimization_checklist`**：
   - 输出 3-5 条优化项；
   - 每条必须简洁直观，优先使用动词开头（如“补充/统一/量化/删除/突出”）；
   - 每条只写一个动作，不要写成长段落。
6. **新增字段：`highlights`**：
   - 固定输出 3 条简历亮点；
   - 每条包含 `rank` 和 `highlight`；
   - `highlight` 必须是一个“关键词短语”（例如“后端项目经验”“技术栈覆盖”），不要写完整句；
   - `rank=1` 必须是最大亮点，`rank=2/3` 依次递减。
7. **修改建议**：针对问题给出可执行的改写或补充建议。
8. **反思机制**：在 <reflection> 标签中自查是否出现泛化问题或缺乏证据的问题点。
9. **乱码容错**：若原文存在乱码/转码异常/OCR 噪声，需忽略该片段，不得将乱码本身当作候选人问题或风险点。
"""

    ctx = build_context(state)
    builder = PromptBuilder(base)
    builder.when(
        lambda c: not c["has_jd"],
        "注意：未提供 JD 时，问题分析应更偏通用简历质量与叙述完整性。",
    )
    builder.when(
        lambda c: c["has_structured"] and c["missing_sections"],
        "注意：结构化简历缺失板块："
        + "、".join(ctx["missing_sections"])
        + "。缺失部分需明确指出并建议补充。",
    )
    return builder.build(ctx)
