package com.example.springbootfront.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ResumePromptFormatTest {
    private final ResumePromptServiceImpl service = new ResumePromptServiceImpl(new ObjectMapper());

    @Test
    void completeTaggedChineseResponseIsAccepted() {
        String response = "[REVISED_RESUME]前端开发经历[/REVISED_RESUME]\n"
                + "[OPTIMIZATION_SUMMARY]调整表达[/OPTIMIZATION_SUMMARY]\n"
                + "[RISK_WARNINGS]保留模拟简历声明[/RISK_WARNINGS]";
        assertNotNull(ReflectionTestUtils.invokeMethod(service, "parseTaggedBlocks", response));
    }

    @Test
    void missingOrUnclosedFinalSectionIsRejected() {
        String response = "[REVISED_RESUME]经历[/REVISED_RESUME]"
                + "[OPTIMIZATION_SUMMARY]调整表达[/OPTIMIZATION_SUMMARY]";
        assertNull(ReflectionTestUtils.invokeMethod(service, "parseTaggedBlocks", response));
        assertNull(ReflectionTestUtils.invokeMethod(service, "parseTaggedBlocks", response + "[RISK_WARNINGS]未写完"));
    }

    @Test
    void existingMarkdownResponseStillWorks() {
        assertNotNull(ReflectionTestUtils.invokeMethod(service, "parseTaggedBlocks",
                "## REVISED_RESUME\n经历\n## OPTIMIZATION_SUMMARY\n调整表达\n## RISK_WARNINGS\n无新增风险"));
    }
}
