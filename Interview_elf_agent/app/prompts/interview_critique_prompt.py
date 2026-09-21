from typing import Any, Dict

from app.prompts.prompt_utils import PromptBuilder, build_context


def build_prompt(state: Dict[str, Any]) -> str:
    ctx = build_context(state)
    min_questions = ctx["min_questions"]
    base = f"""
你是一位严格的面试题审核员。你的任务是审查“问题预测”结果是否合格。

你需要检查：
1. **贴合度**：问题必须围绕输入背景（JD/简历，视模式而定），不要脱离背景。
2. **覆盖度**：至少涵盖技术深度、项目细节、行为/软技能三个方向。
3. **难度梯度**：应有基础到深入的递进，不应全是泛泛问题。
4. **可执行性**：问题清晰可问，不要过度开放或不可验证。
5. **冗余控制**：避免重复或高度相似的问题。
6. **题量要求**：预测问题总数不少于 {min_questions} 道。
7. **答案完整性**：每个问题必须附带 1–3 句参考答案，answer 不能为空。
8. **乱码容错一致性**：若简历/JD 片段存在乱码/转码异常/OCR 噪声，允许生成结果忽略该片段；不得因“未利用乱码片段”判为不合格。

输出指令：
- 合格：只回复 `APPROVE`
- 不合格：回复具体修改意见
"""

    builder = PromptBuilder(base)
    builder.when(
        lambda c: c["interview_mode"] == "resume_only",
        "注意：简历押题模式下，问题必须来源于简历项目/实习/工作经历，不得依赖 JD 或知识库内容。",
    )
    builder.when(
        lambda c: c["interview_mode"] == "jd_only",
        "注意：JD押题模式下，问题必须来源于 JD 与知识库，偏技术/八股文方向，不得依赖简历信息。",
    )
    builder.when(
        lambda c: not c["has_jd"] and c["interview_mode"] != "jd_only",
        "注意：无 JD 时，问题应围绕简历本身，避免臆测岗位要求。",
    )
    return builder.build(ctx)
