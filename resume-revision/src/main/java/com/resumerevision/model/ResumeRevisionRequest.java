package com.resumerevision.model;

public record ResumeRevisionRequest(
        String targetRole,
        String originalResume,
        String constraints
) {
}
