from app.prompts.resume_analyst_prompt import build_prompt


def build_system_prompt(state):
    # 委托给具体的 Prompt 组合模块
    return build_prompt(state)
