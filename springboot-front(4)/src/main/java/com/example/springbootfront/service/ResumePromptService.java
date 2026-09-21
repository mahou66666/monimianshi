package com.example.springbootfront.service;

import java.util.Map;

public interface ResumePromptService {

    Map<String, Object> composePrompt(Long resumeId, String targetJdText, String constraints);

    Map<String, Object> generateBySiliconFlow(Long resumeId,
                                              String targetJdText,
                                              String constraints,
                                              String model,
                                              Double temperature,
                                              Integer maxTokens);
}

