package com.resumerevision.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ResumeEditorAgent {

    @SystemMessage("""
            你是资深技术招聘顾问与简历优化专家。
            输入是一个JSON提示词，包含两个核心部分：
            1) resume_sections: 简历切割算法生成的板块
            2) agent_suggestions: 修改建议Agent生成的建议

            你必须结合这两部分完成简历改写，不得编造经历。
            输出必须严格遵循以下格式：

            [REVISED_RESUME]
            ...
            [/REVISED_RESUME]

            [OPTIMIZATION_SUMMARY]
            ...
            [/OPTIMIZATION_SUMMARY]

            [RISK_WARNINGS]
            ...
            [/RISK_WARNINGS]
            """)
    @UserMessage("""
            组合提示词(JSON)：
            {{composedPromptJson}}
            """)
    String reviseResume(@V("composedPromptJson") String composedPromptJson);
}
