package com.resumerevision.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ResumeSuggestionAgent {

    @SystemMessage("""
            你是简历诊断Agent，负责根据目标岗位与简历板块给出修改建议。
            你必须只输出合法JSON，不要输出任何额外文本。
            JSON格式：
            {
              "agent_suggestions": [
                {
                  "section_name": "板块名",
                  "suggestions": ["建议1", "建议2"],
                  "keywords": ["关键词1", "关键词2"],
                  "priority": "high|medium|low"
                }
              ]
            }
            要求：
            1) 不得建议编造经历。
            2) 每个板块2-4条建议。
            3) 关键词要贴合目标岗位JD语义。
            """)
    @UserMessage("""
            目标岗位：{{targetRole}}
            附加约束：{{constraints}}
            简历板块(JSON)：
            {{resumeSectionsJson}}
            """)
    String generateSuggestions(@V("targetRole") String targetRole,
                               @V("constraints") String constraints,
                               @V("resumeSectionsJson") String resumeSectionsJson);
}
