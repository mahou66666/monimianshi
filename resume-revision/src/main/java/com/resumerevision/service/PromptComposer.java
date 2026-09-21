package com.resumerevision.service;

import com.resumerevision.model.ResumeSection;
import com.resumerevision.util.JsonUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PromptComposer {

    public String compose(String targetRole,
                          String constraints,
                          List<ResumeSection> resumeSections,
                          String agentSuggestionsJson) {

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("task", "resume_revision");
        root.put("target_role", targetRole);
        root.put("constraints", constraints);
        root.put("resume_sections", resumeSections);
        root.put("agent_suggestions", JsonUtils.parseJsonOrRaw(agentSuggestionsJson));
        root.put("output_contract", outputContract());

        return JsonUtils.toJson(root);
    }

    private static Map<String, Object> outputContract() {
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("format", "tagged_text_blocks");
        contract.put("required_tags", List.of(
                "REVISED_RESUME",
                "OPTIMIZATION_SUMMARY",
                "RISK_WARNINGS"
        ));
        return contract;
    }
}
