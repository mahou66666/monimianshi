package com.example.springbootfront.service.impl;

import com.example.springbootfront.dao.ResumeContentDao;
import com.example.springbootfront.dao.ResumeDao;
import com.example.springbootfront.dao.ResumeFragmentDao;
import com.example.springbootfront.dao.ResumeProblemDao;
import com.example.springbootfront.dao.UserEducationDao;
import com.example.springbootfront.dao.UserInternshipDao;
import com.example.springbootfront.dao.UserProjectDao;
import com.example.springbootfront.dto.resume.ResumeImportResult;
import com.example.springbootfront.dto.resume.ResumeParserResponse;
import com.example.springbootfront.entity.Resume;
import com.example.springbootfront.entity.ResumeContent;
import com.example.springbootfront.entity.ResumeFragment;
import com.example.springbootfront.entity.ResumeProblem;
import com.example.springbootfront.entity.User;
import com.example.springbootfront.entity.UserEducation;
import com.example.springbootfront.entity.UserInternship;
import com.example.springbootfront.entity.UserProject;
import com.example.springbootfront.service.ResumePromptService;
import com.example.springbootfront.service.ResumeService;
import com.example.springbootfront.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("resumeService")
public class ResumeServiceImpl implements ResumeService {

    private static final Pattern SCHOOL_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fa5A-Za-z0-9()\\-]{2,40}(\\u5927\\u5b66|\\u5b66\\u9662|\\u5b66\\u6821|\\u7814\\u7a76\\u9662|\\u804c\\u4e1a\\u6280\\u672f\\u5b66\\u9662))"
    );
    private static final Pattern MAJOR_PATTERN = Pattern.compile(
            "(?:\\u4e13\\u4e1a|major)\\s*[:：]?\\s*([\\u4e00-\\u9fa5A-Za-z0-9()\\-]{2,40})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DEGREE_PATTERN = Pattern.compile(
            "(\\u535a\\u58eb\\u7814\\u7a76\\u751f|\\u7855\\u58eb\\u7814\\u7a76\\u751f|\\u7814\\u7a76\\u751f|\\u535a\\u58eb|\\u7855\\u58eb|\\u672c\\u79d1|\\u5b66\\u58eb|\\u4e13\\u79d1|\\u5927\\u4e13|\\u4e2d\\u4e13|MBA|EMBA|Bachelor|Master|PhD)"
    );
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "((?:19|20)\\d{2}(?:[./-](?:1[0-2]|0?[1-9]))?|(?:19|20)\\d{2}\\s*年\\s*(?:1[0-2]|0?[1-9])\\s*月?|(?:19|20)\\d{2})\\s*(?:-|~|—|–|到|至)\\s*((?:19|20)\\d{2}(?:[./-](?:1[0-2]|0?[1-9]))?|(?:19|20)\\d{2}\\s*年\\s*(?:1[0-2]|0?[1-9])\\s*月?|(?:19|20)\\d{2}|至今|现在|今|present|current|now)(?!\\d)",
            Pattern.CASE_INSENSITIVE
    );
    private static final int MAX_RESUME_CONTENT_LENGTH = 60000;
    private static final int MAX_FRAGMENT_LENGTH = 2000;
    private static final int MAX_INTRO_LENGTH = 120;
    private static final int RAW_FALLBACK_MAX_CHUNKS = 3;
    private static final int RAW_HEADING_MAX_LENGTH = 36;
    private static final int ANALYSIS_FRAGMENT_FETCH_SIZE = 200;
    private static final int ANALYSIS_CONTENT_FETCH_SIZE = 1;
    private static final int MAX_DB_TITLE_LENGTH = 120;
    private static final int MAX_DB_TEXT_LENGTH = 2000;
    private static final int OPERATION_STATUS_PENDING = 1;
    private static final Set<String> JD_STOP_WORDS = Set.of(
            "and", "or", "the", "with", "for", "to", "of", "in", "on", "a", "an",
            "is", "are", "be", "that", "this", "will", "can", "must", "should"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ResumeDao resumeDao;

    @Autowired
    private ResumeContentDao resumeContentDao;

    @Autowired
    private ResumeFragmentDao resumeFragmentDao;

    @Autowired
    private ResumeProblemDao resumeProblemDao;

    @Autowired
    private UserProjectDao userProjectDao;

    @Autowired
    private UserInternshipDao userInternshipDao;

    @Autowired
    private UserEducationDao userEducationDao;

    @Autowired
    private UserService userService;

    @Autowired
    private ResumePromptService resumePromptService;

    @Value("${resume.parser.base-url:http://127.0.0.1:8089}")
    private String parserBaseUrl;

    @Value("${resume.parser.timeout-ms:180000}")
    private long parserTimeoutMs;

    @Value("${resume.problem-agent.base-url:http://127.0.0.1:8000}")
    private String problemAgentBaseUrl;

    @Value("${resume.problem-agent.timeout-ms:180000}")
    private long problemAgentTimeoutMs;

    @Value("${resume.problem-agent.api-key:}")
    private String problemAgentApiKey;

    @Override
    public Resume queryById(Long id) {
        return this.resumeDao.queryById(id);
    }

    @Override
    public Page<Resume> queryByPage(Resume resume, PageRequest pageRequest) {
        long total = this.resumeDao.count(resume);
        return new PageImpl<>(this.resumeDao.queryAllByLimit(resume, pageRequest), pageRequest, total);
    }

    @Override
    public Resume insert(Resume resume) {
        this.resumeDao.insert(resume);
        return resume;
    }

    @Override
    public Resume update(Resume resume) {
        this.resumeDao.update(resume);
        return this.queryById(resume.getId());
    }

    @Override
    public boolean deleteById(Long id) {
        return this.resumeDao.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeImportResult importByParser(Long userId, MultipartFile file, String title) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("resume file is required");
        }

        User user = userService.queryById(userId);
        if (user == null) {
            throw new IllegalArgumentException("user not found: " + userId);
        }

        ResumeParserResponse parserResponse = callParser(file);
        if (parserResponse == null) {
            throw new IllegalStateException("parser response is empty");
        }
        if (hasText(parserResponse.getErrorMessage())) {
            throw new IllegalStateException("parser failed: " + parserResponse.getErrorMessage());
        }
        applyRawFallbackForImport(parserResponse);

        Date now = new Date();
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(resolveTitle(file.getOriginalFilename(), title));
        resume.setNum(String.valueOf(System.currentTimeMillis() % 10000000L));
        resume.setProgress(100);
        resume.setCategory("General");
        resume.setTags("");
        resume.setLanguage("zh-CN");
        resume.setContentType(2);
        resume.setIndexName("");
        resume.setScore(0);
        resume.setCompeteRatio(0.0);
        resume.setIsDefault(0);
        resume.setCreateTime(now);
        resume.setUpdateTime(now);
        this.resumeDao.insert(resume);

        if (resume.getId() == null) {
            throw new IllegalStateException("insert resume failed");
        }

        Long resumeId = resume.getId();
        saveResumeContent(resumeId, parserResponse, now);
        int fragmentCount = saveFragments(userId, resumeId, parserResponse, now);
        saveStructuredTables(userId, parserResponse, now);
        syncUserBasicInfo(userId, parserResponse.getBasicInfo());

        ResumeImportResult result = new ResumeImportResult();
        result.setResumeId(resumeId);
        result.setTitle(resume.getTitle());
        result.setFileName(file.getOriginalFilename());
        result.setFragmentCount(fragmentCount);
        result.setSectionCounts(buildSectionCounts(parserResponse));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> analyzeResume(Long resumeId, String targetJdText) {
        if (resumeId == null || resumeId <= 0) {
            throw new IllegalArgumentException("resumeId is required");
        }

        Resume resume = resumeDao.queryById(resumeId);
        if (resume == null) {
            throw new IllegalArgumentException("resume not found: " + resumeId);
        }

        Map<String, String> sections = loadSectionTextMap(resumeId);
        String resumeText = buildResumeTextForProblemAnalysis(sections);
        if (!hasText(resumeText)) {
            throw new IllegalStateException("resume content is empty, please import resume first");
        }

        Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("resume_text", resumeText);
        requestPayload.put("task_type", "problem_only");
        if (hasText(targetJdText)) {
            requestPayload.put("jd_text", normalizeHumanText(targetJdText));
        }

        Map<String, Object> rawResponse = callProblemAgent(requestPayload);
        Map<String, Object> data = asMap(rawResponse.get("data"));
        if (data.isEmpty()) {
            throw new IllegalStateException("problem agent returned empty analysis data");
        }

        String resumeOverview = normalizeHumanText(toStringValue(data.get("resume_overview")));
        String advice = normalizeHumanText(toStringValue(data.get("advice")));
        List<Map<String, Object>> normalizedProblems = normalizeProblemItems(data.get("problems"), advice);
        int storedCount = replaceResumeProblems(resumeId, normalizedProblems);

        Map<String, Object> revisionResult;
        boolean revisionFallback = false;
        String revisionFallbackReason = "";
        try {
            revisionResult = resumePromptService.generateBySiliconFlow(
                    resumeId,
                    targetJdText,
                    null,
                    null,
                    null,
                    null
            );
            revisionFallback = Boolean.TRUE.equals(revisionResult.get("fallback"));
            revisionFallbackReason = normalizeHumanText(toStringValue(revisionResult.get("fallbackReason")));
        } catch (Exception e) {
            revisionFallback = true;
            revisionFallbackReason = simplifyError(e);
            revisionResult = Map.of(
                    "fallback", true,
                    "fallbackReason", revisionFallbackReason,
                    "revisedResume", "",
                    "optimizationSummary", "",
                    "riskWarnings", ""
            );
        }

        String workText = joinSections(sections, "WORK_EXPERIENCE", "INTERNSHIP_EXPERIENCE");
        String projectText = joinSections(sections, "PROJECT_EXPERIENCE");
        String educationText = joinSections(sections, "EDUCATION");
        String skillsText = joinSections(sections, "SKILLS");
        String selfEvalText = joinSections(sections, "SELF_EVALUATION");

        List<String> jdKeywords = extractJdKeywords(targetJdText);
        int keywordMatches = countKeywordMatches(resumeText, jdKeywords);
        double jdMatchRatio = jdKeywords.isEmpty() ? 1.0 : (double) keywordMatches / (double) jdKeywords.size();

        int fragmentCount = Math.max(1, sections.size());
        int populatedSectionCount = countNonEmpty(workText, projectText, educationText, skillsText, selfEvalText);
        int score = calculateScore(resumeText, fragmentCount, populatedSectionCount, jdKeywords, jdMatchRatio);
        if (revisionFallback) {
            score = Math.max(0, score - 8);
        }

        List<String> badges = new ArrayList<>();
        badges.add(hasText(targetJdText) ? "JD tuned" : "General parse");
        badges.add("Fragments: " + fragmentCount);
        badges.add(revisionFallback ? "AI fallback" : "AI rewrite");
        if (!jdKeywords.isEmpty()) {
            badges.add("JD match " + Math.round(jdMatchRatio * 100) + "%");
        }

        String revisedResume = normalizeHumanText(toStringValue(revisionResult.get("revisedResume")));
        String optimizationSummary = normalizeHumanText(toStringValue(revisionResult.get("optimizationSummary")));
        String riskWarnings = normalizeHumanText(toStringValue(revisionResult.get("riskWarnings")));

        List<String> workSuggestions = buildWorkSuggestions(workText, jdKeywords, jdMatchRatio);
        for (Map<String, Object> problem : normalizedProblems) {
            if (workSuggestions.size() >= 5) {
                break;
            }
            String suggestion = normalizeHumanText(toStringValue(problem.get("answer")));
            if (hasText(suggestion) && !workSuggestions.contains(suggestion)) {
                workSuggestions.add(suggestion);
            }
        }

        String projectOptimizedText = hasText(revisedResume)
                ? safePreview(revisedResume, "", 600)
                : buildProjectOptimizedText(projectText, jdKeywords);

        List<Map<String, Object>> sectionCards = new ArrayList<>();
        Map<String, Object> workCard = new LinkedHashMap<>();
        workCard.put("id", "work-experience");
        workCard.put("title", "Work and Internship");
        workCard.put("status", hasText(workText) ? "Parsed" : "Need content");
        workCard.put("originalText", safePreview(workText, "No work or internship fragment detected.", 320));
        workCard.put("suggestions", workSuggestions);
        workCard.put("actionLabel", "Rewrite with AI");
        sectionCards.add(workCard);

        Map<String, Object> projectCard = new LinkedHashMap<>();
        projectCard.put("id", "project-experience");
        projectCard.put("title", "Project Experience");
        projectCard.put("status", hasText(projectText) ? "Parsed" : "Need content");
        projectCard.put("optimizedText", projectOptimizedText);
        projectCard.put("primaryActionLabel", "Apply changes");
        projectCard.put("secondaryActionLabel", "Undo");
        sectionCards.add(projectCard);

        Map<String, Object> educationCard = new LinkedHashMap<>();
        educationCard.put("id", "education");
        educationCard.put("title", "Education");
        educationCard.put("status", hasText(educationText) ? "Parsed" : "Need content");
        sectionCards.add(educationCard);

        Map<String, Integer> sectionCounts = new LinkedHashMap<>();
        sectionCounts.put("WORK_EXPERIENCE", hasText(workText) ? 1 : 0);
        sectionCounts.put("INTERNSHIP_EXPERIENCE", hasText(joinSections(sections, "INTERNSHIP_EXPERIENCE")) ? 1 : 0);
        sectionCounts.put("PROJECT_EXPERIENCE", hasText(projectText) ? 1 : 0);
        sectionCounts.put("EDUCATION", hasText(educationText) ? 1 : 0);
        sectionCounts.put("SKILLS", hasText(skillsText) ? 1 : 0);
        sectionCounts.put("SELF_EVALUATION", hasText(selfEvalText) ? 1 : 0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resumeId", resumeId);
        result.put("resumeOverview", resumeOverview);
        result.put("advice", advice);
        result.put("optimizationChecklist", asStringList(data.get("optimization_checklist")));
        result.put("highlights", asListOfMap(data.get("highlights")));
        result.put("problems", normalizedProblems);
        result.put("problemCount", normalizedProblems.size());
        result.put("storedProblemCount", storedCount);
        result.put("agentCode", rawResponse.get("code"));
        result.put("agentMsg", rawResponse.get("msg"));
        result.put("traceLog", rawResponse.get("trace_log"));
        result.put("score", score);
        result.put("total", 100);
        result.put("badges", badges);
        result.put("sections", sectionCards);
        result.put("finalActionLabel", revisionFallback ? "Review and regenerate" : "Generate final resume");
        result.put("sectionCounts", sectionCounts);
        result.put("resumeRevisionPrompt", revisionResult.get("promptJson"));
        result.put("resumeRevisionSummary", optimizationSummary);
        result.put("resumeRevisionRiskWarnings", riskWarnings);
        result.put("revisedResume", revisedResume);
        result.put("fallback", revisionFallback);
        result.put("fallbackReason", revisionFallbackReason);
        return result;
    }

    private String buildResumeTextForProblemAnalysis(Map<String, String> sections) {
        String workText = joinSections(sections, "WORK_EXPERIENCE", "INTERNSHIP_EXPERIENCE");
        String projectText = joinSections(sections, "PROJECT_EXPERIENCE");
        String educationText = joinSections(sections, "EDUCATION");
        String skillsText = joinSections(sections, "SKILLS");
        String selfEvalText = joinSections(sections, "SELF_EVALUATION");
        String rawText = joinSections(sections, "RAW_CONTENT");
        return joinNonEmpty(workText, projectText, educationText, skillsText, selfEvalText, rawText);
    }

    private Map<String, Object> callProblemAgent(Map<String, Object> payload) {
        String url = normalizeBaseUrl(problemAgentBaseUrl) + "/api/v1/agent/process";
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(problemAgentTimeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (hasText(problemAgentApiKey)) {
                builder.header("X-API-Key", problemAgentApiKey.trim());
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, Object> parsed = objectMapper.readValue(response.body(), Map.class);
            if (response.statusCode() >= 400) {
                String detail = toStringValue(parsed.get("detail"));
                if (!hasText(detail)) {
                    detail = toStringValue(parsed.get("msg"));
                }
                if (!hasText(detail)) {
                    detail = "problem agent call failed";
                }
                throw new IllegalStateException(detail);
            }
            return parsed;
        } catch (HttpTimeoutException e) {
            throw new IllegalStateException("problem agent request timeout after " + problemAgentTimeoutMs + " ms", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("problem agent call interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("failed to parse problem agent response: " + e.getMessage(), e);
        }
    }

    private int replaceResumeProblems(Long resumeId, List<Map<String, Object>> problems) {
        resumeProblemDao.deleteByResumeId(resumeId);
        if (problems == null || problems.isEmpty()) {
            return 0;
        }

        Date now = new Date();
        List<ResumeProblem> rows = new ArrayList<>();
        for (Map<String, Object> item : problems) {
            ResumeProblem row = new ResumeProblem();
            row.setResumeId(resumeId);
            row.setProblemTitle(safeDbText(toStringValue(item.get("title")), MAX_DB_TITLE_LENGTH));
            row.setProblemDesc(safeDbText(toStringValue(item.get("problem")), MAX_DB_TEXT_LENGTH));
            row.setPriority(mapPriority(toStringValue(item.get("severity"))));
            row.setSuggestion(safeDbText(toStringValue(item.get("answer")), MAX_DB_TEXT_LENGTH));
            row.setStatus(OPERATION_STATUS_PENDING);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            rows.add(row);
        }

        if (rows.isEmpty()) {
            return 0;
        }
        resumeProblemDao.insertBatch(rows);
        return rows.size();
    }

    private List<Map<String, Object>> normalizeProblemItems(Object rawProblems, String fallbackAdvice) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> item : asListOfMap(rawProblems)) {
            String title = normalizeHumanText(toStringValue(item.get("title")));
            String severity = normalizeHumanText(toStringValue(item.get("severity")));
            String problem = normalizeHumanText(toStringValue(item.get("problem")));
            String answer = normalizeHumanText(toStringValue(item.get("answer")));
            List<String> tags = asStringList(item.get("tags"));

            if (!hasText(problem) && !hasText(title)) {
                continue;
            }
            if (!hasText(title)) {
                title = truncate(problem, MAX_DB_TITLE_LENGTH);
            }
            if (!hasText(problem)) {
                problem = title;
            }
            if (!hasText(answer)) {
                answer = hasText(fallbackAdvice) ? fallbackAdvice : "补充量化指标与关键技术细节，提升岗位匹配度。";
            }
            if (!hasText(severity)) {
                severity = "中";
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title", title);
            row.put("severity", severity);
            row.put("problem", problem);
            row.put("answer", answer);
            row.put("tags", tags);
            normalized.add(row);
        }
        return normalized;
    }

    private List<Map<String, Object>> asListOfMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> map = asMap(item);
            if (!map.isEmpty()) {
                result.add(map);
            }
        }
        return result;
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            String text = normalizeHumanText(toStringValue(item));
            if (hasText(text)) {
                result.add(text);
            }
        }
        return result;
    }

    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, Object> converted = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            converted.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return converted;
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String mapPriority(String severity) {
        String normalized = normalizeHumanText(severity).toLowerCase(Locale.ROOT);
        if (normalized.contains("high") || normalized.contains("严重") || normalized.contains("高")) {
            return "高";
        }
        if (normalized.contains("low") || normalized.contains("低")) {
            return "低";
        }
        return "中";
    }

    private String safeDbText(String text, int maxLength) {
        String normalized = normalizeHumanText(text);
        if (!hasText(normalized)) {
            return "";
        }
        return truncate(normalized, maxLength);
    }

    private Map<String, String> loadSectionTextMap(Long resumeId) {
        Map<String, String> sectionMap = new LinkedHashMap<>();

        ResumeFragment query = new ResumeFragment();
        query.setResumeId(resumeId);
        List<ResumeFragment> fragments = resumeFragmentDao.queryAllByLimit(
                query,
                PageRequest.of(0, ANALYSIS_FRAGMENT_FETCH_SIZE)
        );

        if (fragments != null) {
            for (ResumeFragment fragment : fragments) {
                if (fragment == null) {
                    continue;
                }
                String sectionKey = normalizeSectionKey(fragment.getSectionKey());
                String text = normalizeHumanText(fragment.getFragmentText());
                if (!hasText(sectionKey) || !hasText(text)) {
                    continue;
                }
                sectionMap.merge(sectionKey, text, (left, right) -> left + "\n\n" + right);
            }
        }

        if (!sectionMap.isEmpty()) {
            return sectionMap;
        }

        ResumeContent contentQuery = new ResumeContent();
        contentQuery.setResumeId(resumeId);
        List<ResumeContent> contentRows = resumeContentDao.queryAllByLimit(
                contentQuery,
                PageRequest.of(0, ANALYSIS_CONTENT_FETCH_SIZE)
        );
        if (contentRows == null || contentRows.isEmpty()) {
            return sectionMap;
        }

        String content = normalizeHumanText(contentRows.get(0).getContent());
        if (hasText(content)) {
            sectionMap.put("RAW_CONTENT", content);
        }
        return sectionMap;
    }

    private String normalizeSectionKey(String sectionKey) {
        if (!hasText(sectionKey)) {
            return "";
        }
        return sectionKey.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeHumanText(String text) {
        if (!hasText(text)) {
            return "";
        }
        String normalized = decodeUnicodeEscapes(text.trim());
        normalized = normalized.replace("\\n", "\n").replace("\\r", "\n").replace("\\t", " ");
        return normalized.trim();
    }

    private String decodeUnicodeEscapes(String text) {
        if (!hasText(text) || text.indexOf("\\u") < 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char ch = text.charAt(i);
            if (ch == '\\' && i + 5 < text.length() && text.charAt(i + 1) == 'u') {
                String hex = text.substring(i + 2, i + 6);
                try {
                    int codePoint = Integer.parseInt(hex, 16);
                    sb.append((char) codePoint);
                    i += 6;
                    continue;
                } catch (NumberFormatException ignore) {
                    // keep original chars below
                }
            }
            sb.append(ch);
            i++;
        }
        return sb.toString();
    }

    private String joinSections(Map<String, String> sections, String... keys) {
        if (sections == null || sections.isEmpty() || keys == null || keys.length == 0) {
            return "";
        }
        List<String> blocks = new ArrayList<>();
        for (String key : keys) {
            String value = sections.get(normalizeSectionKey(key));
            if (hasText(value)) {
                blocks.add(value.trim());
            }
        }
        return blocks.isEmpty() ? "" : String.join("\n\n", blocks);
    }

    private String joinNonEmpty(String... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        List<String> blocks = new ArrayList<>();
        for (String value : values) {
            if (hasText(value)) {
                blocks.add(value.trim());
            }
        }
        return blocks.isEmpty() ? "" : String.join("\n\n", blocks);
    }

    private int countNonEmpty(String... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        int count = 0;
        for (String value : values) {
            if (hasText(value)) {
                count++;
            }
        }
        return count;
    }

    private List<String> extractJdKeywords(String jdText) {
        if (!hasText(jdText)) {
            return List.of();
        }

        String normalized = normalizeHumanText(jdText).toLowerCase(Locale.ROOT);
        String[] tokens = normalized.split("[^\\p{IsHan}A-Za-z0-9+#.]+");
        LinkedHashSet<String> keywordSet = new LinkedHashSet<>();
        for (String token : tokens) {
            if (!hasText(token)) {
                continue;
            }
            String word = token.trim();
            if (word.length() < 2 || word.length() > 24) {
                continue;
            }
            if (JD_STOP_WORDS.contains(word)) {
                continue;
            }
            keywordSet.add(word);
            if (keywordSet.size() >= 24) {
                break;
            }
        }
        return new ArrayList<>(keywordSet);
    }

    private int countKeywordMatches(String content, List<String> keywords) {
        if (!hasText(content) || keywords == null || keywords.isEmpty()) {
            return 0;
        }
        String normalized = content.toLowerCase(Locale.ROOT);
        int count = 0;
        for (String keyword : keywords) {
            if (hasText(keyword) && normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                count++;
            }
        }
        return count;
    }

    private int calculateScore(String content,
                               int fragmentCount,
                               int populatedSectionCount,
                               List<String> jdKeywords,
                               double jdMatchRatio) {
        int fragmentPoints = Math.min(24, fragmentCount * 4);
        int contentPoints = Math.min(24, hasText(content) ? content.length() / 180 : 0);
        int sectionPoints = Math.min(12, populatedSectionCount * 3);
        int jdPoints = jdKeywords == null || jdKeywords.isEmpty()
                ? 6
                : Math.min(20, (int) Math.round(jdMatchRatio * 20));

        int raw = 40 + fragmentPoints + contentPoints + sectionPoints + jdPoints;
        return Math.max(0, Math.min(100, raw));
    }

    private List<String> buildWorkSuggestions(String workText, List<String> jdKeywords, double jdMatchRatio) {
        List<String> suggestions = new ArrayList<>();
        String normalized = hasText(workText) ? workText : "";

        if (!normalized.matches("(?s).*\\d+.*")) {
            suggestions.add("Add measurable outcomes (latency, throughput, conversion, or cost saving).");
        } else {
            suggestions.add("Keep quantified impact and move metrics to the first sentence.");
        }

        if (jdKeywords != null && !jdKeywords.isEmpty()) {
            if (jdMatchRatio < 0.35) {
                suggestions.add("Increase JD keyword alignment in work bullets.");
            } else {
                suggestions.add("JD keyword alignment is acceptable; improve depth with concrete trade-offs.");
            }
        } else {
            suggestions.add("Provide target JD to generate stronger role-specific suggestions.");
        }

        if (!normalized.matches("(?is).*(led|designed|implemented|optimized|owned|delivered).*")) {
            suggestions.add("Use stronger action verbs to highlight ownership.");
        } else {
            suggestions.add("Keep action verbs and add architecture constraints for context.");
        }

        return suggestions;
    }

    private String buildProjectOptimizedText(String projectText, List<String> jdKeywords) {
        if (!hasText(projectText)) {
            return "No project fragment found. Add one project with architecture, challenge, and measurable impact.";
        }

        String preview = safePreview(projectText, "", 220);
        if (jdKeywords == null || jdKeywords.isEmpty()) {
            return preview + "\n\nSuggested rewrite: start with business context, then architecture decisions, and end with measurable outcomes.";
        }

        String keywords = String.join(", ", jdKeywords.subList(0, Math.min(3, jdKeywords.size())));
        return preview + "\n\nSuggested rewrite: add explicit alignment with JD keywords (" + keywords + ") and quantify the final result.";
    }

    private String safePreview(String text, String fallback, int maxLength) {
        if (!hasText(text)) {
            return fallback;
        }
        String compact = text.replace('\r', '\n').trim();
        compact = compact.replaceAll("\\n{3,}", "\n\n");
        return truncate(compact, maxLength);
    }

    private ResumeParserResponse callParser(MultipartFile file) {
        String url = normalizeBaseUrl(parserBaseUrl) + "/api/resume/parse";

        try {
            String boundary = "----ResumeBoundary" + UUID.randomUUID().toString().replace("-", "");
            byte[] payload = buildMultipartPayload(boundary, file);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(parserTimeoutMs))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                String detail = response.body() == null ? "" : response.body();
                throw new IllegalStateException("parser call failed (" + response.statusCode() + "): " + truncate(detail, 300));
            }
            if (!hasText(response.body())) {
                throw new IllegalStateException("parser response body is empty");
            }

            return objectMapper.readValue(response.body(), ResumeParserResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException("parser call error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("parser call interrupted", e);
        }
    }

    private byte[] buildMultipartPayload(String boundary, MultipartFile file) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String safeFileName = sanitizeFileName(file.getOriginalFilename());

        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + (hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream") + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(file.getBytes());
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private void saveResumeContent(Long resumeId, ResumeParserResponse parserResponse, Date now) {
        ResumeContent resumeContent = new ResumeContent();
        resumeContent.setResumeId(resumeId);
        resumeContent.setContent(buildContentText(parserResponse));
        resumeContent.setCreateTime(now);
        resumeContent.setUpdateTime(now);
        resumeContentDao.insert(resumeContent);
    }

    private String buildContentText(ResumeParserResponse parserResponse) {
        StringBuilder sb = new StringBuilder();

        appendSectionText(sb, "BASIC_INFO", toJsonSafely(parserResponse.getBasicInfo()));
        appendSectionText(sb, "SKILLS", objectToText(parserResponse.getSkills()));
        appendSectionText(sb, "EDUCATION", joinList(parserResponse.getEducation()));
        appendSectionText(sb, "WORK_EXPERIENCE", joinList(parserResponse.getWorkExperience()));
        appendSectionText(sb, "INTERNSHIP_EXPERIENCE", joinList(parserResponse.getInternshipExperience()));
        appendSectionText(sb, "PROJECT_EXPERIENCE", joinList(parserResponse.getProjectExperience()));
        appendSectionText(sb, "AWARDS", objectToText(parserResponse.getAwards()));
        appendSectionText(sb, "SELF_EVALUATION", objectToText(parserResponse.getSelfEvaluation()));
        appendSectionText(sb, "RAW_CONTENT", parserResponse.getRawContent());

        return truncate(sb.toString(), MAX_RESUME_CONTENT_LENGTH);
    }

    private void appendSectionText(StringBuilder sb, String section, String text) {
        if (!hasText(text)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append("\n\n");
        }
        sb.append("[").append(section).append("]\n").append(text.trim());
    }

    private int saveFragments(Long userId, Long resumeId, ResumeParserResponse parserResponse, Date now) {
        List<ResumeFragment> fragments = new ArrayList<>();

        appendObjectFragment(fragments, userId, resumeId, "BASIC_INFO", parserResponse.getBasicInfo(), now);
        appendObjectFragment(fragments, userId, resumeId, "SKILLS", parserResponse.getSkills(), now);
        appendListFragments(fragments, userId, resumeId, "EDUCATION", parserResponse.getEducation(), now);
        appendListFragments(fragments, userId, resumeId, "WORK_EXPERIENCE", parserResponse.getWorkExperience(), now);
        appendListFragments(fragments, userId, resumeId, "INTERNSHIP_EXPERIENCE", parserResponse.getInternshipExperience(), now);
        appendListFragments(fragments, userId, resumeId, "PROJECT_EXPERIENCE", parserResponse.getProjectExperience(), now);
        appendObjectFragment(fragments, userId, resumeId, "AWARDS", parserResponse.getAwards(), now);
        appendObjectFragment(fragments, userId, resumeId, "SELF_EVALUATION", parserResponse.getSelfEvaluation(), now);
        appendRawFallbackFragments(fragments, userId, resumeId, parserResponse, now);

        if (!fragments.isEmpty()) {
            resumeFragmentDao.insertBatch(fragments);
        }
        return fragments.size();
    }

    private void appendListFragments(List<ResumeFragment> target,
                                     Long userId,
                                     Long resumeId,
                                     String sectionKey,
                                     List<String> values,
                                     Date now) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (hasText(value)) {
                normalized.add(value.trim());
            }
        }
        if (normalized.isEmpty()) {
            return;
        }

        // DB has unique key on (user_id, resume_id, section_key), so merge same-section items.
        target.add(buildFragment(userId, resumeId, sectionKey, 1, String.join("\n\n", normalized), now));
    }

    private void appendObjectFragment(List<ResumeFragment> target,
                                      Long userId,
                                      Long resumeId,
                                      String sectionKey,
                                      Object value,
                                      Date now) {
        String text = objectToText(value);
        if (!hasText(text)) {
            return;
        }
        target.add(buildFragment(userId, resumeId, sectionKey, 1, text, now));
    }

    private void applyRawFallbackForImport(ResumeParserResponse parserResponse) {
        if (parserResponse == null || hasStructuredSectionData(parserResponse)) {
            return;
        }
        String rawText = normalizeHumanText(parserResponse.getRawContent());
        if (!hasText(rawText)) {
            return;
        }

        Map<String, String> rawSections = collectRawSections(rawText);
        if (parserResponse.getEducation() == null || parserResponse.getEducation().isEmpty()) {
            parserResponse.setEducation(toChunkList(rawSections.get("EDUCATION"), 2, 700));
        }
        if (parserResponse.getWorkExperience() == null || parserResponse.getWorkExperience().isEmpty()) {
            parserResponse.setWorkExperience(toChunkList(rawSections.get("WORK_EXPERIENCE"), 3, 700));
        }
        if (parserResponse.getInternshipExperience() == null || parserResponse.getInternshipExperience().isEmpty()) {
            parserResponse.setInternshipExperience(toChunkList(rawSections.get("INTERNSHIP_EXPERIENCE"), 2, 700));
        }
        if (parserResponse.getProjectExperience() == null || parserResponse.getProjectExperience().isEmpty()) {
            parserResponse.setProjectExperience(toChunkList(rawSections.get("PROJECT_EXPERIENCE"), 3, 700));
        }
        if (!hasText(objectToText(parserResponse.getSkills()))) {
            String skillText = rawSections.get("SKILLS");
            if (hasText(skillText)) {
                parserResponse.setSkills(skillText);
            }
        }
        if (!hasText(objectToText(parserResponse.getAwards()))) {
            String awardsText = rawSections.get("AWARDS");
            if (hasText(awardsText)) {
                parserResponse.setAwards(awardsText);
            }
        }
        if (!hasText(objectToText(parserResponse.getSelfEvaluation()))) {
            String selfEvalText = rawSections.get("SELF_EVALUATION");
            if (hasText(selfEvalText)) {
                parserResponse.setSelfEvaluation(selfEvalText);
            }
        }
        if (!hasStructuredSectionData(parserResponse)) {
            parserResponse.setWorkExperience(toChunkList(rawText, RAW_FALLBACK_MAX_CHUNKS, 700));
        }
    }

    private boolean hasStructuredSectionData(ResumeParserResponse parserResponse) {
        if (parserResponse == null) {
            return false;
        }
        return hasText(objectToText(parserResponse.getSkills()))
                || hasText(objectToText(parserResponse.getAwards()))
                || hasText(objectToText(parserResponse.getSelfEvaluation()))
                || safeSize(parserResponse.getEducation()) > 0
                || safeSize(parserResponse.getWorkExperience()) > 0
                || safeSize(parserResponse.getInternshipExperience()) > 0
                || safeSize(parserResponse.getProjectExperience()) > 0;
    }

    private void appendRawFallbackFragments(List<ResumeFragment> target,
                                            Long userId,
                                            Long resumeId,
                                            ResumeParserResponse parserResponse,
                                            Date now) {
        if (target == null || !target.isEmpty() || parserResponse == null) {
            return;
        }

        String rawText = normalizeHumanText(parserResponse.getRawContent());
        if (!hasText(rawText)) {
            return;
        }

        Map<String, String> rawSections = collectRawSections(rawText);
        appendObjectFragment(target, userId, resumeId, "EDUCATION", rawSections.get("EDUCATION"), now);
        appendObjectFragment(target, userId, resumeId, "WORK_EXPERIENCE", rawSections.get("WORK_EXPERIENCE"), now);
        appendObjectFragment(target, userId, resumeId, "INTERNSHIP_EXPERIENCE", rawSections.get("INTERNSHIP_EXPERIENCE"), now);
        appendObjectFragment(target, userId, resumeId, "PROJECT_EXPERIENCE", rawSections.get("PROJECT_EXPERIENCE"), now);
        appendObjectFragment(target, userId, resumeId, "SKILLS", rawSections.get("SKILLS"), now);
        appendObjectFragment(target, userId, resumeId, "AWARDS", rawSections.get("AWARDS"), now);
        appendObjectFragment(target, userId, resumeId, "SELF_EVALUATION", rawSections.get("SELF_EVALUATION"), now);

        if (target.isEmpty()) {
            String fallback = truncate(rawText, MAX_FRAGMENT_LENGTH);
            appendObjectFragment(target, userId, resumeId, "WORK_EXPERIENCE", fallback, now);
        }
    }

    private Map<String, String> collectRawSections(String rawText) {
        Map<String, StringBuilder> bufferMap = new LinkedHashMap<>();
        String currentSection = null;

        for (String lineRaw : rawText.split("\\r?\\n")) {
            String line = lineRaw == null ? "" : lineRaw.trim();
            if (!hasText(line)) {
                continue;
            }

            String sectionKey = detectRawSectionKey(line);
            if (sectionKey != null) {
                currentSection = sectionKey;
                bufferMap.putIfAbsent(currentSection, new StringBuilder());
                continue;
            }

            if (currentSection == null) {
                continue;
            }
            StringBuilder sb = bufferMap.computeIfAbsent(currentSection, k -> new StringBuilder());
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line);
        }

        Map<String, String> sections = new LinkedHashMap<>();
        for (Map.Entry<String, StringBuilder> entry : bufferMap.entrySet()) {
            String value = entry.getValue() == null ? null : entry.getValue().toString().trim();
            if (hasText(value)) {
                sections.put(entry.getKey(), value);
            }
        }
        return sections;
    }

    private String detectRawSectionKey(String line) {
        if (!hasText(line)) {
            return null;
        }
        String compact = line.replaceAll("\\s+", "");
        if (compact.length() > RAW_HEADING_MAX_LENGTH) {
            return null;
        }
        String lower = compact.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "教育背景", "教育经历", "学历", "education")) {
            return "EDUCATION";
        }
        if (containsAny(lower, "项目经历", "项目经验", "项目", "project")) {
            return "PROJECT_EXPERIENCE";
        }
        if (containsAny(lower, "实习经历", "实习经验", "internship", "intern")) {
            return "INTERNSHIP_EXPERIENCE";
        }
        if (containsAny(lower, "工作经历", "工作经验", "工作", "experience")) {
            return "WORK_EXPERIENCE";
        }
        if (containsAny(lower, "技能", "专业技能", "技术栈", "skill")) {
            return "SKILLS";
        }
        if (containsAny(lower, "奖项", "荣誉", "证书", "award", "honor")) {
            return "AWARDS";
        }
        if (containsAny(lower, "自我评价", "个人评价", "个人总结", "summary", "profile")) {
            return "SELF_EVALUATION";
        }
        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        if (!hasText(text) || keywords == null || keywords.length == 0) {
            return false;
        }
        for (String keyword : keywords) {
            if (hasText(keyword) && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> toChunkList(String text, int maxChunks, int chunkLength) {
        if (!hasText(text) || maxChunks <= 0 || chunkLength <= 0) {
            return null;
        }

        List<String> chunks = new ArrayList<>();
        String normalized = normalizeHumanText(text);
        for (String blockRaw : normalized.split("\\n\\s*\\n")) {
            String block = blockRaw == null ? "" : blockRaw.trim();
            if (!hasText(block)) {
                continue;
            }
            chunks.add(truncate(block, chunkLength));
            if (chunks.size() >= maxChunks) {
                break;
            }
        }

        if (chunks.isEmpty()) {
            chunks.add(truncate(normalized, chunkLength));
        }
        return chunks;
    }

    private ResumeFragment buildFragment(Long userId,
                                         Long resumeId,
                                         String sectionKey,
                                         int index,
                                         String rawText,
                                         Date now) {
        String normalized = truncate(rawText.trim(), MAX_FRAGMENT_LENGTH);

        ResumeFragment fragment = new ResumeFragment();
        fragment.setUserId(userId);
        fragment.setResumeId(resumeId);
        fragment.setSectionKey(sectionKey);
        fragment.setItemIndex(index);
        fragment.setFragmentText(normalized);
        fragment.setFragmentHash(sha256(normalized));
        fragment.setCreateTime(now);
        fragment.setUpdateTime(now);
        return fragment;
    }

    private void saveStructuredTables(Long userId, ResumeParserResponse parserResponse, Date now) {
        saveProjects(userId, parserResponse.getProjectExperience(), now);
        saveInternships(userId, parserResponse.getInternshipExperience(), now);
        saveEducations(userId, parserResponse.getEducation(), now);
    }

    private void saveProjects(Long userId, List<String> projectList, Date now) {
        if (projectList == null || projectList.isEmpty()) {
            return;
        }

        List<UserProject> rows = new ArrayList<>();
        int idx = 1;
        for (String projectText : projectList) {
            if (!hasText(projectText)) {
                continue;
            }
            DateRange range = extractDateRange(projectText, now);

            UserProject row = new UserProject();
            row.setUserId(userId);
            row.setProjectName(extractTitle(projectText, "Project", idx));
            row.setStartTime(range.startTime());
            row.setEndTime(range.endTime());
            row.setIntro(truncate(projectText.trim(), MAX_INTRO_LENGTH));
            row.setContent(projectText.trim());
            row.setCreateTime(now);
            row.setUpdateTime(now);
            rows.add(row);
            idx++;
        }

        if (!rows.isEmpty()) {
            userProjectDao.insertBatch(rows);
        }
    }

    private void saveInternships(Long userId, List<String> internshipList, Date now) {
        if (internshipList == null || internshipList.isEmpty()) {
            return;
        }

        List<UserInternship> rows = new ArrayList<>();
        int idx = 1;
        for (String internshipText : internshipList) {
            if (!hasText(internshipText)) {
                continue;
            }
            DateRange range = extractDateRange(internshipText, now);

            UserInternship row = new UserInternship();
            row.setUserId(userId);
            row.setCompany(extractTitle(internshipText, "Internship", idx));
            row.setStartTime(range.startTime());
            row.setEndTime(range.endTime());
            row.setIntro(truncate(internshipText.trim(), MAX_INTRO_LENGTH));
            row.setContent(internshipText.trim());
            row.setCreateTime(now);
            row.setUpdateTime(now);
            rows.add(row);
            idx++;
        }

        if (!rows.isEmpty()) {
            userInternshipDao.insertBatch(rows);
        }
    }

    private void saveEducations(Long userId, List<String> educationList, Date now) {
        if (educationList == null || educationList.isEmpty()) {
            return;
        }

        List<UserEducation> rows = new ArrayList<>();
        for (String educationText : educationList) {
            if (!hasText(educationText)) {
                continue;
            }
            DateRange range = extractDateRange(educationText, now);

            UserEducation row = new UserEducation();
            row.setUserId(userId);
            String school = extractSchool(educationText);
            String degree = extractDegree(educationText);
            row.setSchoolId(hasText(school) ? school : "UNKNOWN_SCHOOL");
            row.setMajor(extractMajor(educationText));
            row.setDegree(hasText(degree) ? degree : "UNKNOWN_DEGREE");
            row.setStartTime(range.startTime());
            row.setEndTime(range.endTime());
            row.setIntro(truncate(educationText.trim(), MAX_INTRO_LENGTH));
            row.setContent(educationText.trim());
            row.setCreateTime(now);
            row.setUpdateTime(now);
            rows.add(row);
        }

        if (!rows.isEmpty()) {
            userEducationDao.insertBatch(rows);
        }
    }

    private void syncUserBasicInfo(Long userId, ResumeParserResponse.BasicInfo basicInfo) {
        if (basicInfo == null) {
            return;
        }

        User patch = new User();
        patch.setId(userId);
        patch.setName(hasText(basicInfo.getName()) ? basicInfo.getName().trim() : null);
        patch.setPhone(hasText(basicInfo.getPhone()) ? basicInfo.getPhone().trim() : null);
        patch.setGender(hasText(basicInfo.getGender()) ? basicInfo.getGender().trim() : null);
        userService.save(patch);
    }

    private Map<String, Integer> buildSectionCounts(ResumeParserResponse parserResponse) {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("EDUCATION", safeSize(parserResponse.getEducation()));
        map.put("WORK_EXPERIENCE", safeSize(parserResponse.getWorkExperience()));
        map.put("INTERNSHIP_EXPERIENCE", safeSize(parserResponse.getInternshipExperience()));
        map.put("PROJECT_EXPERIENCE", safeSize(parserResponse.getProjectExperience()));
        map.put("SKILLS", hasText(objectToText(parserResponse.getSkills())) ? 1 : 0);
        map.put("AWARDS", hasText(objectToText(parserResponse.getAwards())) ? 1 : 0);
        map.put("SELF_EVALUATION", hasText(objectToText(parserResponse.getSelfEvaluation())) ? 1 : 0);
        return map;
    }

    private int safeSize(List<String> list) {
        return list == null ? 0 : (int) list.stream().filter(this::hasText).count();
    }

    private String resolveTitle(String fileName, String title) {
        if (hasText(title)) {
            return title.trim();
        }
        if (!hasText(fileName)) {
            return "Resume";
        }

        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            return fileName.substring(0, dot);
        }
        return fileName;
    }

    private String sanitizeFileName(String fileName) {
        if (!hasText(fileName)) {
            return "resume.bin";
        }
        return fileName.replace("\"", "").trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://127.0.0.1:8089";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String objectToText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof List<?> list) {
            List<String> textList = new ArrayList<>();
            for (Object item : list) {
                if (item == null) {
                    continue;
                }
                String text = String.valueOf(item).trim();
                if (!text.isEmpty()) {
                    textList.add(text);
                }
            }
            return String.join("\n", textList);
        }
        return toJsonSafely(value);
    }

    private String joinList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        List<String> normalized = new ArrayList<>();
        for (String item : list) {
            if (hasText(item)) {
                normalized.add(item.trim());
            }
        }
        return normalized.isEmpty() ? null : String.join("\n\n", normalized);
    }

    private String toJsonSafely(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String extractTitle(String text, String fallbackPrefix, int index) {
        if (!hasText(text)) {
            return fallbackPrefix + index;
        }
        String cleaned = text.trim();
        int lineBreak = cleaned.indexOf('\n');
        String firstLine = lineBreak > 0 ? cleaned.substring(0, lineBreak).trim() : cleaned;
        return truncate(firstLine.isEmpty() ? fallbackPrefix + index : firstLine, 80);
    }

    private String extractSchool(String educationText) {
        if (!hasText(educationText)) {
            return null;
        }
        Matcher matcher = SCHOOL_PATTERN.matcher(educationText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractDegree(String educationText) {
        if (!hasText(educationText)) {
            return null;
        }
        Matcher matcher = DEGREE_PATTERN.matcher(educationText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractMajor(String educationText) {
        if (!hasText(educationText)) {
            return "UNKNOWN_MAJOR";
        }
        Matcher matcher = MAJOR_PATTERN.matcher(educationText);
        if (matcher.find()) {
            String major = matcher.group(1);
            if (hasText(major)) {
                return truncate(major.trim(), 40);
            }
        }
        return "UNKNOWN_MAJOR";
    }

    private DateRange extractDateRange(String text, Date fallback) {
        if (!hasText(text)) {
            return new DateRange(fallback, fallback);
        }

        Matcher matcher = DATE_RANGE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return new DateRange(fallback, fallback);
        }

        Date start = parseDateToken(matcher.group(1), false);
        Date end = parseDateToken(matcher.group(2), true);

        if (start == null && end == null) {
            return new DateRange(fallback, fallback);
        }
        if (start == null) {
            start = end;
        }
        if (end == null) {
            end = start;
        }
        if (start.after(end)) {
            Date tmp = start;
            start = end;
            end = tmp;
        }
        return new DateRange(start, end);
    }

    private Date parseDateToken(String token, boolean isEnd) {
        if (!hasText(token)) {
            return null;
        }

        String value = token.trim().toLowerCase();
        if ("至今".equals(value) || "现在".equals(value) || "今".equals(value)
                || "present".equals(value) || "current".equals(value) || "now".equals(value)) {
            return new Date();
        }

        String normalized = value
                .replaceAll("\\s+", "")
                .replace("年", "-")
                .replace("月", "")
                .replace(".", "-")
                .replace("/", "-");
        while (normalized.endsWith("-")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.matches("^(19|20)\\d{2}$")) {
            int year = Integer.parseInt(normalized);
            LocalDate date = isEnd ? LocalDate.of(year, 12, 31) : LocalDate.of(year, 1, 1);
            return toDate(date);
        }
        if (normalized.matches("^(19|20)\\d{2}-(0?[1-9]|1[0-2])$")) {
            String[] parts = normalized.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            YearMonth ym = YearMonth.of(year, month);
            LocalDate date = isEnd ? ym.atEndOfMonth() : ym.atDay(1);
            return toDate(date);
        }
        return null;
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safeText(String text, String fallback) {
        if (!hasText(text)) return fallback;
        return text.replace("\r\n", "\n").replace('\r', '\n').replace("\\n", "\n").trim();
    }

    private String simplifyError(Exception e) {
        String msg = e == null ? "unknown error" : safeText(e.getMessage(), "no message");
        return (e == null ? "Exception" : e.getClass().getSimpleName()) + ": " + truncate(msg, 300);
    }

    private record DateRange(Date startTime, Date endTime) {
    }
}
