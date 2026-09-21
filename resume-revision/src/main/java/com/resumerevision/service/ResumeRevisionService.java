package com.resumerevision.service;

import com.resumerevision.gateway.ResumeLlmGateway;
import com.resumerevision.model.ResumeRevisionRequest;
import com.resumerevision.model.ResumeRevisionResult;
import com.resumerevision.model.ResumeSection;
import com.resumerevision.util.JsonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class ResumeRevisionService {

    private static final String REVISED_RESUME = "REVISED_RESUME";
    private static final String OPTIMIZATION_SUMMARY = "OPTIMIZATION_SUMMARY";
    private static final String RISK_WARNINGS = "RISK_WARNINGS";
    private static final int MAX_REASON_LENGTH = 240;

    private final ResumeLlmGateway llmGateway;
    private final ResumeSectionSplitter splitter;
    private final PromptComposer promptComposer;

    public ResumeRevisionService(ResumeLlmGateway llmGateway,
                                 ResumeSectionSplitter splitter,
                                 PromptComposer promptComposer) {
        this.llmGateway = requireNonNull(llmGateway, "llmGateway must not be null");
        this.splitter = requireNonNull(splitter, "splitter must not be null");
        this.promptComposer = requireNonNull(promptComposer, "promptComposer must not be null");
    }

    public ResumeRevisionService(ResumeEditorAgent editorAgent,
                                 ResumeSuggestionAgent suggestionAgent,
                                 ResumeSectionSplitter splitter,
                                 PromptComposer promptComposer) {
        this(bridgeAgents(editorAgent, suggestionAgent), splitter, promptComposer);
    }

    public ResumeRevisionResult revise(ResumeRevisionRequest request) {
        requireNonNull(request, "request must not be null");

        String targetRole = defaultIfBlank(request.targetRole(), "unknown-role");
        String originalResume = defaultIfBlank(request.originalResume(), "");
        String constraints = defaultIfBlank(request.constraints(), "none");

        List<ResumeSection> sections = safeSplit(originalResume);
        String sectionsJson = JsonUtils.toJson(Map.of("resume_sections", sections));

        String suggestionsJson = safeGenerateSuggestions(targetRole, constraints, sectionsJson, sections);
        String composedPromptJson = safeComposePrompt(targetRole, constraints, sections, suggestionsJson);

        try {
            String llmOutput = llmGateway.reviseResume(composedPromptJson);
            ParsedBlocks blocks = parseTaggedBlocks(llmOutput);
            if (blocks != null) {
                return new ResumeRevisionResult(
                        blocks.revisedResume(),
                        blocks.optimizationSummary(),
                        blocks.riskWarnings(),
                        composedPromptJson
                );
            }
            return buildFallbackResult(
                    sections,
                    composedPromptJson,
                    "model_output_missing_required_tags"
            );
        } catch (Exception e) {
            return buildFallbackResult(
                    sections,
                    composedPromptJson,
                    "llm_revision_failed: " + simplifyError(e)
            );
        }
    }

    private List<ResumeSection> safeSplit(String originalResume) {
        try {
            return splitter.split(originalResume);
        } catch (Exception e) {
            return List.of(new ResumeSection("Original Content", defaultIfBlank(originalResume, "")));
        }
    }

    private String safeGenerateSuggestions(String targetRole,
                                           String constraints,
                                           String sectionsJson,
                                           List<ResumeSection> sections) {
        try {
            return llmGateway.generateSuggestions(targetRole, constraints, sectionsJson);
        } catch (Exception e) {
            return buildFallbackSuggestionsJson(sections, "suggestion_generation_failed: " + simplifyError(e));
        }
    }

    private String safeComposePrompt(String targetRole,
                                     String constraints,
                                     List<ResumeSection> sections,
                                     String suggestionsJson) {
        try {
            return promptComposer.compose(targetRole, constraints, sections, suggestionsJson);
        } catch (Exception e) {
            Map<String, Object> fallbackPrompt = new LinkedHashMap<>();
            fallbackPrompt.put("task", "resume_revision");
            fallbackPrompt.put("target_role", targetRole);
            fallbackPrompt.put("constraints", constraints);
            fallbackPrompt.put("resume_sections", sections);
            fallbackPrompt.put("agent_suggestions", JsonUtils.parseJsonOrRaw(suggestionsJson));
            fallbackPrompt.put("prompt_compose_fallback_reason", simplifyError(e));
            return JsonUtils.toJson(fallbackPrompt);
        }
    }

    private ParsedBlocks parseTaggedBlocks(String llmOutput) {
        String revisedResume = extractBlockOrNull(llmOutput, REVISED_RESUME);
        String summary = extractBlockOrNull(llmOutput, OPTIMIZATION_SUMMARY);
        String riskWarnings = extractBlockOrNull(llmOutput, RISK_WARNINGS);
        if (revisedResume == null || summary == null || riskWarnings == null) {
            return null;
        }
        return new ParsedBlocks(revisedResume, summary, riskWarnings);
    }

    private static String extractBlockOrNull(String text, String tag) {
        String safeText = text == null ? "" : text;
        String start = "[" + tag + "]";
        String end = "[/" + tag + "]";
        int startIndex = safeText.indexOf(start);
        int endIndex = safeText.indexOf(end);

        if (startIndex < 0 || endIndex < 0 || endIndex <= startIndex) {
            return null;
        }

        int contentStart = startIndex + start.length();
        return safeText.substring(contentStart, endIndex).trim();
    }

    private ResumeRevisionResult buildFallbackResult(List<ResumeSection> sections,
                                                     String composedPromptJson,
                                                     String reason) {
        String revisedResume = buildFallbackRevisedResume(sections, reason);
        String optimizationSummary = buildFallbackOptimizationSummary(reason);
        String riskWarnings = buildFallbackRiskWarnings(reason);
        return new ResumeRevisionResult(revisedResume, optimizationSummary, riskWarnings, composedPromptJson);
    }

    private String buildFallbackRevisedResume(List<ResumeSection> sections, String reason) {
        StringBuilder sb = new StringBuilder();
        sb.append("FALLBACK_MODE_ENABLED\n");
        sb.append("Reason: ").append(limit(reason)).append("\n\n");
        sb.append("No AI rewrite was applied. Original sections are returned below:\n\n");
        for (ResumeSection section : sections) {
            sb.append("## ").append(defaultIfBlank(section.sectionName(), "Section")).append("\n");
            sb.append(defaultIfBlank(section.content(), "")).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String buildFallbackOptimizationSummary(String reason) {
        return String.join("\n",
                "FALLBACK_OPTIMIZATION_SUMMARY",
                "- AI revision step failed, fallback summary was used.",
                "- Reason: " + limit(reason),
                "- Recommendation: retry the model call and keep factual content unchanged.",
                "- Recommendation: manually add quantified impact (metrics, scale, outcomes).",
                "- Recommendation: prioritize concise action-result phrasing per section."
        );
    }

    private String buildFallbackRiskWarnings(String reason) {
        return String.join("\n",
                "FALLBACK_RISK_WARNINGS",
                "- Revision may be incomplete because the model did not return valid tagged blocks.",
                "- Reason: " + limit(reason),
                "- Do not fabricate project history, responsibilities, or achievements.",
                "- Re-run after network/API timeout is resolved."
        );
    }

    private String buildFallbackSuggestionsJson(List<ResumeSection> sections, String reason) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (ResumeSection section : sections) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("section_name", defaultIfBlank(section.sectionName(), "section"));
            item.put("suggestions", List.of(
                    "Keep all statements factual and verifiable.",
                    "Use action-result phrasing and highlight measurable outcomes.",
                    "Remove repetitive wording and improve clarity."
            ));
            item.put("keywords", List.of("impact", "metrics", "ownership"));
            item.put("priority", "medium");
            suggestions.add(item);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("agent_suggestions", suggestions);
        root.put("fallback_reason", limit(reason));
        return JsonUtils.toJson(root);
    }

    private static String simplifyError(Throwable throwable) {
        if (throwable == null) {
            return "unknown_error";
        }
        String className = throwable.getClass().getSimpleName();
        String message = defaultIfBlank(throwable.getMessage(), "no_message");
        return className + ": " + limit(message);
    }

    private static String limit(String value) {
        String safe = defaultIfBlank(value, "none");
        if (safe.length() <= MAX_REASON_LENGTH) {
            return safe;
        }
        return safe.substring(0, MAX_REASON_LENGTH) + "...";
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static ResumeLlmGateway bridgeAgents(ResumeEditorAgent editorAgent, ResumeSuggestionAgent suggestionAgent) {
        ResumeEditorAgent safeEditor = requireNonNull(editorAgent, "editorAgent must not be null");
        ResumeSuggestionAgent safeSuggestion = requireNonNull(suggestionAgent, "suggestionAgent must not be null");
        return new ResumeLlmGateway() {
            @Override
            public String generateSuggestions(String targetRole, String constraints, String resumeSectionsJson) {
                return safeSuggestion.generateSuggestions(targetRole, constraints, resumeSectionsJson);
            }

            @Override
            public String reviseResume(String composedPromptJson) {
                return safeEditor.reviseResume(composedPromptJson);
            }
        };
    }

    private record ParsedBlocks(String revisedResume, String optimizationSummary, String riskWarnings) {
    }
}

