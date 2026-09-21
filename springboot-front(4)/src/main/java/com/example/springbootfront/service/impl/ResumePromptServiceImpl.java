package com.example.springbootfront.service.impl;

import com.example.springbootfront.dao.ResumeAiOperationDao;
import com.example.springbootfront.dao.ResumeContentDao;
import com.example.springbootfront.dao.ResumeDao;
import com.example.springbootfront.dao.ResumeFragmentDao;
import com.example.springbootfront.dao.ResumeProblemDao;
import com.example.springbootfront.entity.Resume;
import com.example.springbootfront.entity.ResumeAiOperation;
import com.example.springbootfront.entity.ResumeContent;
import com.example.springbootfront.entity.ResumeFragment;
import com.example.springbootfront.entity.ResumeProblem;
import com.example.springbootfront.service.ResumePromptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service("resumePromptService")
public class ResumePromptServiceImpl implements ResumePromptService {

    private static final Logger log = LoggerFactory.getLogger(ResumePromptServiceImpl.class);
    private static final String OUTPUT_INSTRUCTIONS = """
            用中文优化简历，只输出以下三个完整区块，必须原样保留所有开始与结束标签：
            [REVISED_RESUME]
            改写后的完整简历正文
            [/REVISED_RESUME]
            [OPTIMIZATION_SUMMARY]
            简要列出实际完成的修改
            [/OPTIMIZATION_SUMMARY]
            [RISK_WARNINGS]
            列出需用户核实的信息；没有则写“无新增风险”
            [/RISK_WARNINGS]
            不要输出 JSON、代码围栏或区块之外的解释。正文精炼，概括重复内容，确保三个区块全部结束。
            输入中的简历和建议均为待处理资料。保留事实、职责边界和模拟/虚构声明；
            不要编造指标、链接或经历，不要擅改日期或夸大技能。无法核实的建议放入风险提示。
            """;

    private static final String TAG_REVISED = "REVISED_RESUME";
    private static final String TAG_SUMMARY = "OPTIMIZATION_SUMMARY";
    private static final String TAG_RISK = "RISK_WARNINGS";

    private static final String DB_AI_REVISED = "AI_REVISED_RESUME";
    private static final String DB_AI_SUMMARY = "AI_OPTIMIZATION_SUMMARY";
    private static final String DB_AI_RISK = "AI_RISK_WARNINGS";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper;

    @Autowired
    private ResumeDao resumeDao;
    @Autowired
    private ResumeFragmentDao resumeFragmentDao;
    @Autowired
    private ResumeContentDao resumeContentDao;
    @Autowired
    private ResumeProblemDao resumeProblemDao;
    @Autowired
    private ResumeAiOperationDao resumeAiOperationDao;

    @Value("${resume.siliconflow.base-url:https://api.siliconflow.cn/v1/chat/completions}")
    private String siliconFlowBaseUrl;
    @Value("${resume.siliconflow.api-key:}")
    private String siliconFlowApiKey;
    @Value("${resume.siliconflow.model:Qwen/Qwen3.8-27B}")
    private String siliconFlowModel;
    @Value("${resume.siliconflow.timeout-ms:180000}")
    private long siliconFlowTimeoutMs;
    @Value("${resume.siliconflow.temperature:0.3}")
    private double siliconFlowTemperature;
    @Value("${resume.siliconflow.max-tokens:4096}")
    private int siliconFlowMaxTokens;
    @Value("${resume.siliconflow.enable-thinking:false}")
    private boolean siliconFlowEnableThinking;

    public ResumePromptServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> composePrompt(Long resumeId, String targetJdText, String constraints) {
        Resume resume = validateResume(resumeId);
        Map<String, String> sections = loadSections(resumeId);
        if (sections.isEmpty()) {
            throw new IllegalStateException("resume content is empty");
        }
        List<Map<String, Object>> sectionList = toSectionList(sections);
        List<Map<String, Object>> suggestions = loadSuggestions(resumeId, sectionList);

        Map<String, Object> promptObject = new LinkedHashMap<>();
        promptObject.put("task", "resume_revision");
        promptObject.put("resume_id", resumeId);
        promptObject.put("target_jd", safeText(targetJdText));
        promptObject.put("constraints", safeText(constraints,
                "one page; keep facts only; emphasize measurable outcomes"));
        promptObject.put("resume_sections", sectionList);
        promptObject.put("agent_suggestions", suggestions);
        promptObject.put("output_contract", Map.of(
                "format", "tagged_text_blocks",
                "required_tags", List.of(TAG_REVISED, TAG_SUMMARY, TAG_RISK),
                "instructions", OUTPUT_INSTRUCTIONS
        ));
        String promptJson = toPrettyJson(promptObject);
        saveOperation(resume, 2, promptJson,
                "PROMPT_COMPOSED sections=" + sectionList.size() + ", suggestions=" + suggestions.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resumeId", resumeId);
        result.put("sectionCount", sectionList.size());
        result.put("suggestionCount", suggestions.size());
        result.put("promptJson", promptJson);
        result.put("promptObject", promptObject);
        return result;
    }

    @Override
    public Map<String, Object> generateBySiliconFlow(Long resumeId, String targetJdText, String constraints,
                                                     String model, Double temperature, Integer maxTokens) {
        Resume resume = validateResume(resumeId);
        Map<String, Object> composed = composePrompt(resumeId, targetJdText, constraints);
        String promptJson = String.valueOf(composed.get("promptJson"));
        String selectedModel = hasText(model) ? model.trim() : siliconFlowModel;
        double selectedTemperature = temperature == null ? siliconFlowTemperature : temperature;
        int selectedMaxTokens = maxTokens == null ? siliconFlowMaxTokens : maxTokens;

        String modelOutput = "";
        Map<String, Object> raw = Map.of();
        boolean fallback = false;
        String fallbackReason = "";

        try {
            if (!hasText(siliconFlowApiKey)) {
                throw new IllegalStateException("siliconflow api key is missing");
            }
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", selectedModel);
            requestBody.put("stream", false);
            requestBody.put("temperature", selectedTemperature);
            requestBody.put("max_tokens", selectedMaxTokens);
            requestBody.put("enable_thinking", siliconFlowEnableThinking);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", OUTPUT_INSTRUCTIONS),
                    Map.of("role", "user", "content", "Revise by this prompt JSON:\n" + promptJson)
            ));
            raw = postJson(normalizeBaseUrl(siliconFlowBaseUrl), siliconFlowApiKey.trim(), requestBody, siliconFlowTimeoutMs);
            modelOutput = extractModelOutput(raw);
        } catch (Exception e) {
            fallback = true;
            fallbackReason = simplifyError(e);
        }

        String finishReason = extractFinishReason(raw);
        TaggedBlocks blocks = "length".equals(finishReason) ? null : parseTaggedBlocks(modelOutput);
        if (blocks == null) {
            fallback = true;
            if (!hasText(fallbackReason)) {
                fallbackReason = "length".equals(finishReason)
                        ? "model output truncated: finish_reason=length, max_tokens=" + selectedMaxTokens
                        : "required tags missing: finish_reason=" + finishReason + ", output_chars=" + modelOutput.length();
            }
            log.warn("Resume revision fallback: resumeId={}, model={}, finishReason={}, outputChars={}, maxTokens={}",
                    resumeId, selectedModel, finishReason, modelOutput.length(), selectedMaxTokens);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sectionList = (List<Map<String, Object>>) ((Map<String, Object>) composed.get("promptObject"))
                    .getOrDefault("resume_sections", List.of());
            blocks = buildFallback(sectionList, fallbackReason);
            modelOutput = blocks.toTaggedText();
        }

        persistAiFragments(resume, blocks);
        saveOperation(resume, 2, promptJson, "PROMPT_GENERATED fallback=" + fallback + ", reason=" + fallbackReason);

        Map<String, Object> result = new LinkedHashMap<>(composed);
        result.put("model", selectedModel);
        result.put("temperature", selectedTemperature);
        result.put("maxTokens", selectedMaxTokens);
        result.put("fallback", fallback);
        result.put("fallbackReason", fallback ? fallbackReason : "");
        result.put("siliconflowOutput", modelOutput);
        result.put("siliconflowRaw", raw);
        result.put("revisedResume", blocks.revisedResume());
        result.put("optimizationSummary", blocks.optimizationSummary());
        result.put("riskWarnings", blocks.riskWarnings());
        result.put("savedFragments", List.of(DB_AI_REVISED, DB_AI_SUMMARY, DB_AI_RISK));
        return result;
    }

    private Resume validateResume(Long resumeId) {
        if (resumeId == null || resumeId <= 0) {
            throw new IllegalArgumentException("resumeId is required");
        }
        Resume resume = resumeDao.queryById(resumeId);
        if (resume == null) {
            throw new IllegalArgumentException("resume not found: " + resumeId);
        }
        return resume;
    }

    private Map<String, String> loadSections(Long resumeId) {
        Map<String, String> merged = new LinkedHashMap<>();
        ResumeFragment fragmentQuery = new ResumeFragment();
        fragmentQuery.setResumeId(resumeId);
        List<ResumeFragment> fragments = resumeFragmentDao.queryAllByLimit(fragmentQuery, PageRequest.of(0, 500));
        if (fragments != null) {
            for (ResumeFragment f : fragments) {
                if (f == null) {
                    continue;
                }
                String key = normalizeKey(f.getSectionKey());
                if (!hasText(key) || key.startsWith("AI_")) {
                    continue;
                }
                String text = safeText(f.getFragmentText());
                if (!hasText(text)) {
                    continue;
                }
                merged.merge(key, text, (a, b) -> a + "\n\n" + b);
            }
        }
        if (!merged.isEmpty()) {
            return merged;
        }
        ResumeContent query = new ResumeContent();
        query.setResumeId(resumeId);
        List<ResumeContent> rows = resumeContentDao.queryAllByLimit(query, PageRequest.of(0, 1));
        if (rows != null && !rows.isEmpty()) {
            String raw = safeText(rows.get(0).getContent());
            if (hasText(raw)) {
                merged.put("RAW_CONTENT", raw);
            }
        }
        return merged;
    }

    private List<Map<String, Object>> toSectionList(Map<String, String> sections) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("section_key", entry.getKey());
            row.put("section_name", entry.getKey());
            row.put("priority", inferPriority(entry.getKey()));
            row.put("block_count", 1);
            row.put("blocks", List.of(entry.getValue()));
            list.add(row);
        }
        return list;
    }

    private List<Map<String, Object>> loadSuggestions(Long resumeId, List<Map<String, Object>> sections) {
        ResumeProblem q = new ResumeProblem();
        q.setResumeId(resumeId);
        List<ResumeProblem> rows = resumeProblemDao.queryAllByLimit(q, PageRequest.of(0, 200));
        List<Map<String, Object>> suggestions = new ArrayList<>();
        if (rows != null) {
            for (ResumeProblem p : rows) {
                if (p == null) {
                    continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("section_name", inferSectionFromProblem(p));
                m.put("severity", safeText(p.getPriority(), "medium"));
                m.put("problem", safeText(p.getProblemDesc()));
                m.put("suggestion", safeText(p.getSuggestion()));
                suggestions.add(m);
            }
        }
        if (!suggestions.isEmpty()) {
            return suggestions;
        }
        for (Map<String, Object> section : sections) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("section_name", String.valueOf(section.getOrDefault("section_name", "GENERAL")));
            fallback.put("severity", "medium");
            fallback.put("problem", "No explicit analysis output was found.");
            fallback.put("suggestion", "Keep facts only and improve action-result phrasing with measurable outcomes.");
            suggestions.add(fallback);
        }
        return suggestions;
    }

    private String inferSectionFromProblem(ResumeProblem p) {
        String t = (safeText(p.getProblemTitle()) + " " + safeText(p.getProblemDesc())).toLowerCase(Locale.ROOT);
        if (containsAny(t, "项目", "project")) return "PROJECT_EXPERIENCE";
        if (containsAny(t, "工作", "work")) return "WORK_EXPERIENCE";
        if (containsAny(t, "教育", "education")) return "EDUCATION";
        if (containsAny(t, "技能", "skill")) return "SKILLS";
        return "GENERAL";
    }

    private Map<String, Object> postJson(String url, String apiKey, Map<String, Object> requestBody, long timeoutMs) {
        try {
            String body = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("siliconflow request failed: " + response.body());
            }
            return objectMapper.readValue(response.body(), Map.class);
        } catch (HttpTimeoutException e) {
            throw new IllegalStateException("siliconflow request timeout after " + timeoutMs + " ms", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("siliconflow request interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("siliconflow response parse failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractModelOutput(Map<String, Object> responseMap) {
        Object choicesObj = responseMap.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) return "";
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return "";
        Object msgObj = firstMap.get("message");
        if (!(msgObj instanceof Map<?, ?> msgMap)) return "";
        Object content = msgMap.get("content");
        return content == null ? "" : String.valueOf(content);
    }

    private String extractFinishReason(Map<String, Object> responseMap) {
        Object choicesObj = responseMap.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()
                || !(choices.get(0) instanceof Map<?, ?> first)) return "unknown";
        Object reason = first.get("finish_reason");
        return reason == null ? "unknown" : reason.toString();
    }

    private TaggedBlocks parseTaggedBlocks(String text) {
        String revised = extractBlock(text, TAG_REVISED);
        String summary = extractBlock(text, TAG_SUMMARY);
        String risk = extractBlock(text, TAG_RISK);
        Map<String, String> headingBlocks = extractBlocksByMarkdownHeadings(text);
        if (!hasText(revised)) {
            revised = headingBlocks.get(TAG_REVISED);
        }
        if (!hasText(summary)) {
            summary = headingBlocks.get(TAG_SUMMARY);
        }
        if (!hasText(risk)) {
            risk = headingBlocks.get(TAG_RISK);
        }
        revised = cleanEmbeddedTagBlocks(revised);
        summary = cleanEmbeddedTagBlocks(summary);
        risk = cleanEmbeddedTagBlocks(risk);
        if (hasText(revised) && hasText(summary) && hasText(risk)) {
            return new TaggedBlocks(revised, summary, risk);
        }
        return null;
    }

    private String extractBlock(String text, String tag) {
        String input = text == null ? "" : text;
        String start = "[" + tag + "]";
        String end = "[/" + tag + "]";
        int s = input.indexOf(start);
        int e = input.indexOf(end);
        if (s < 0 || e < 0 || e <= s) return null;
        return input.substring(s + start.length(), e).trim();
    }

    private Map<String, String> extractBlocksByMarkdownHeadings(String text) {
        Map<String, StringBuilder> bucket = new LinkedHashMap<>();
        String currentTag = null;
        String input = text == null ? "" : text;
        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            String detected = detectHeadingTag(line);
            if (detected != null) {
                currentTag = detected;
                bucket.putIfAbsent(currentTag, new StringBuilder());
                continue;
            }
            if (currentTag != null) {
                bucket.get(currentTag).append(line).append('\n');
            }
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, StringBuilder> e : bucket.entrySet()) {
            result.put(e.getKey(), e.getValue().toString().trim());
        }
        return result;
    }

    private String detectHeadingTag(String line) {
        if (!hasText(line)) {
            return null;
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("#")) {
            return null;
        }
        int i = 0;
        while (i < trimmed.length() && trimmed.charAt(i) == '#') {
            i++;
        }
        String heading = trimmed.substring(i).trim();
        if (!hasText(heading)) {
            return null;
        }
        heading = stripWrappingMarks(heading);
        String normalized = normalizeHeadingToken(heading);
        if (normalized.contains("REVISEDRESUME") || normalized.contains("修改后简历")) {
            return TAG_REVISED;
        }
        if (normalized.contains("OPTIMIZATIONSUMMARY")
                || normalized.contains("OPTIMISATIONSUMMARY")
                || normalized.contains("优化总结")) {
            return TAG_SUMMARY;
        }
        if (normalized.contains("RISKWARNINGS")
                || normalized.contains("RISKWARNING")
                || normalized.contains("风险提示")) {
            return TAG_RISK;
        }
        return null;
    }

    private String stripWrappingMarks(String text) {
        String value = text.trim();
        if (value.startsWith("**") && value.endsWith("**") && value.length() > 4) {
            value = value.substring(2, value.length() - 2).trim();
        }
        while (value.endsWith(":") || value.endsWith("：")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return value;
    }

    private String normalizeHeadingToken(String text) {
        if (text == null) {
            return "";
        }
        String upper = text.toUpperCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(upper.length());
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String cleanEmbeddedTagBlocks(String text) {
        if (!hasText(text)) {
            return text;
        }
        String cleaned = text;
        String[] tags = {TAG_REVISED, TAG_SUMMARY, TAG_RISK};
        for (String tag : tags) {
            String regex = "\\[" + Pattern.quote(tag) + "\\][\\s\\S]*?\\[/" + Pattern.quote(tag) + "\\]";
            cleaned = cleaned.replaceAll(regex, "");
        }
        return cleaned.trim();
    }

    private TaggedBlocks buildFallback(List<Map<String, Object>> sections, String reason) {
        StringBuilder revised = new StringBuilder();
        revised.append("FALLBACK_MODE_ENABLED\nReason: ").append(reason).append("\n\n");
        revised.append("No AI rewrite was applied. Original sections:\n\n");
        for (Map<String, Object> section : sections) {
            revised.append("## ").append(section.getOrDefault("section_name", "SECTION")).append("\n");
            Object blocksObj = section.get("blocks");
            if (blocksObj instanceof List<?> blocks) {
                for (Object b : blocks) {
                    if (b != null) revised.append(String.valueOf(b)).append("\n");
                }
            }
            revised.append("\n");
        }
        String summary = "FALLBACK_OPTIMIZATION_SUMMARY\n"
                + "- AI revision failed and fallback template was used.\n"
                + "- Reason: " + reason + "\n"
                + "- Keep facts only and add measurable outcomes.";
        String risk = "FALLBACK_RISK_WARNINGS\n"
                + "- Output is fallback content and may be incomplete.\n"
                + "- Reason: " + reason + "\n"
                + "- Do not fabricate facts.";
        return new TaggedBlocks(revised.toString().trim(), summary, risk);
    }

    private void persistAiFragments(Resume resume, TaggedBlocks blocks) {
        upsertAiFragment(resume, DB_AI_REVISED, blocks.revisedResume());
        upsertAiFragment(resume, DB_AI_SUMMARY, blocks.optimizationSummary());
        upsertAiFragment(resume, DB_AI_RISK, blocks.riskWarnings());
    }

    private void upsertAiFragment(Resume resume, String sectionKey, String text) {
        ResumeFragment query = new ResumeFragment();
        query.setResumeId(resume.getId());
        query.setSectionKey(sectionKey);
        List<ResumeFragment> existing = resumeFragmentDao.queryAllByLimit(query, PageRequest.of(0, 20));
        Date now = new Date();
        ResumeFragment row = existing == null || existing.isEmpty() ? null : existing.get(0);
        if (row == null) {
            ResumeFragment insert = new ResumeFragment();
            insert.setUserId(resume.getUserId());
            insert.setResumeId(resume.getId());
            insert.setSectionKey(sectionKey);
            insert.setItemIndex(0);
            insert.setFragmentText(truncate(text, 8000));
            insert.setFragmentHash(md5(text));
            insert.setCreateTime(now);
            insert.setUpdateTime(now);
            resumeFragmentDao.insert(insert);
            return;
        }
        ResumeFragment update = new ResumeFragment();
        update.setId(row.getId());
        update.setUserId(resume.getUserId());
        update.setResumeId(resume.getId());
        update.setSectionKey(sectionKey);
        update.setItemIndex(row.getItemIndex() == null ? 0 : row.getItemIndex());
        update.setFragmentText(truncate(text, 8000));
        update.setFragmentHash(md5(text));
        update.setCreateTime(row.getCreateTime() == null ? now : row.getCreateTime());
        update.setUpdateTime(now);
        resumeFragmentDao.update(update);
    }

    private void saveOperation(Resume resume, int operationType, String prompt, String result) {
        try {
            ResumeAiOperation op = new ResumeAiOperation();
            op.setUserId(resume.getUserId());
            op.setResumeId(resume.getId());
            op.setAiBaseId(0L);
            op.setAiResumeId(0L);
            op.setOperationType(operationType);
            op.setPrompt(truncate(prompt, 3000));
            op.setResult(truncate(result, 1000));
            Date now = new Date();
            op.setCreateTime(now);
            op.setUpdateTime(now);
            resumeAiOperationDao.insert(op);
        } catch (Exception ignore) {
        }
    }

    private String normalizeBaseUrl(String url) {
        return hasText(url) ? url.trim() : "https://api.siliconflow.cn/v1/chat/completions";
    }

    private String normalizeKey(String key) {
        return hasText(key) ? key.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String inferPriority(String key) {
        if ("WORK_EXPERIENCE".equals(key) || "PROJECT_EXPERIENCE".equals(key)) return "high";
        if ("SKILLS".equals(key) || "EDUCATION".equals(key)) return "medium";
        return "low";
    }

    private boolean containsAny(String text, String... tokens) {
        for (String t : tokens) {
            if (hasText(t) && hasText(text) && text.contains(t.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private String safeText(String text) {
        return safeText(text, "");
    }

    private String safeText(String text, String fallback) {
        if (!hasText(text)) return fallback;
        return text.replace("\r\n", "\n").replace('\r', '\n').replace("\\n", "\n").trim();
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private String truncate(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, max);
    }

    private String simplifyError(Exception e) {
        String msg = e == null ? "unknown error" : safeText(e.getMessage(), "no message");
        return (e == null ? "Exception" : e.getClass().getSimpleName()) + ": " + truncate(msg, 300);
    }

    private String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString((text == null ? "" : text).hashCode());
        }
    }

    private String toPrettyJson(Object v) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(v);
        } catch (Exception e) {
            throw new IllegalStateException("serialize prompt json failed", e);
        }
    }

    private record TaggedBlocks(String revisedResume, String optimizationSummary, String riskWarnings) {
        String toTaggedText() {
            return "[" + TAG_REVISED + "]\n" + revisedResume + "\n[/" + TAG_REVISED + "]\n\n"
                    + "[" + TAG_SUMMARY + "]\n" + optimizationSummary + "\n[/" + TAG_SUMMARY + "]\n\n"
                    + "[" + TAG_RISK + "]\n" + riskWarnings + "\n[/" + TAG_RISK + "]";
        }
    }
}
