package com.resumerevision.model;

public record ResumeRevisionResult(
        String revisedResume,
        String optimizationSummary,
        String riskWarnings,
        String composedPromptJson
) {
}
