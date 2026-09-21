package com.resumerevision.gateway;

public interface ResumeLlmGateway {

    String generateSuggestions(String targetRole, String constraints, String resumeSectionsJson);

    String reviseResume(String composedPromptJson);
}

