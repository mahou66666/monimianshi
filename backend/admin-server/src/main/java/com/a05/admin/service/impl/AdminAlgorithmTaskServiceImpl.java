package com.a05.admin.service.impl;

import com.a05.admin.entity.JdGuide;
import com.a05.admin.entity.JdJob;
import com.a05.admin.entity.Resume;
import com.a05.admin.entity.ResumeContent;
import com.a05.admin.entity.ResumeFile;
import com.a05.admin.mapper.JdGuideMapper;
import com.a05.admin.mapper.JdJobMapper;
import com.a05.admin.mapper.ResumeContentMapper;
import com.a05.admin.mapper.ResumeFileMapper;
import com.a05.admin.mapper.ResumeMapper;
import com.a05.admin.service.AdminAlgorithmTaskService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class AdminAlgorithmTaskServiceImpl implements AdminAlgorithmTaskService {

    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String TASK_QUEUED = "QUEUED";
    private static final String TASK_RUNNING = "RUNNING";
    private static final String TASK_SUCCESS = "SUCCESS";
    private static final String TASK_FAILED = "FAILED";
    private static final String TASK_CANCELED = "CANCELED";

    private static final String ALGO_JD_GUIDE = "jd_guide";
    private static final String ALGO_RESUME_PARSE = "resume_parse";

    private final ConcurrentMap<String, TaskState> taskStore = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private final JdJobMapper jdJobMapper;
    private final JdGuideMapper jdGuideMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ResumeContentMapper resumeContentMapper;

    @Value("${app.storage.resume-dir:storage/resumes}")
    private String resumeStorageDir;

    @Value("${app.resume-parse.enabled:true}")
    private boolean resumeParseEnabled;

    @Value("${app.resume-parse.base-url:http://localhost:8089}")
    private String resumeParseBaseUrl;

    @Value("${app.resume-parse.connect-timeout-ms:3000}")
    private int resumeParseConnectTimeoutMs;

    @Value("${app.resume-parse.read-timeout-ms:120000}")
    private int resumeParseReadTimeoutMs;

    public AdminAlgorithmTaskServiceImpl(JdJobMapper jdJobMapper,
                                         JdGuideMapper jdGuideMapper,
                                         ResumeMapper resumeMapper,
                                         ResumeFileMapper resumeFileMapper,
                                         ResumeContentMapper resumeContentMapper) {
        this.jdJobMapper = jdJobMapper;
        this.jdGuideMapper = jdGuideMapper;
        this.resumeMapper = resumeMapper;
        this.resumeFileMapper = resumeFileMapper;
        this.resumeContentMapper = resumeContentMapper;
    }

    @Override
    public Map<String, Object> createTask(Long userId, String algoKey, Map<String, Object> request) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("userId cannot be empty");
        }
        if (!StringUtils.hasText(algoKey)) {
            throw new RuntimeException("algoKey cannot be empty");
        }

        String normalizedAlgoKey = algoKey.trim().toLowerCase();
        Map<String, Object> safeRequest = safeMap(request);

        if (ALGO_JD_GUIDE.equals(normalizedAlgoKey)) {
            return createJdGuideTask(userId, safeRequest);
        }
        if (ALGO_RESUME_PARSE.equals(normalizedAlgoKey)) {
            return createResumeParseTask(userId, safeRequest);
        }
        throw new RuntimeException("Unsupported algorithm key: " + algoKey);
    }

    @Override
    public Map<String, Object> getTask(Long userId, String taskId) {
        TaskState task = findTask(userId, taskId);
        if (task == null) {
            return buildExpiredTaskResponse(userId, taskId);
        }
        return toTaskMap(task);
    }

    @Override
    public Map<String, Object> cancelTask(Long userId, String taskId) {
        TaskState task = requireTask(userId, taskId);
        task.cancelRequested = true;

        if (TASK_QUEUED.equals(task.status) || TASK_RUNNING.equals(task.status)) {
            task.status = TASK_CANCELED;
            task.updateTime = LocalDateTime.now();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.taskId);
        data.put("status", task.status);
        data.put("cancelRequested", true);
        return data;
    }

    @Override
    public Map<String, Object> getPendingResumeIds(Long userId, String taskId) {
        requireTask(userId, taskId);
        Map<String, Object> data = new HashMap<>();
        data.put("resumeIds", new ArrayList<>());
        return data;
    }

    @Override
    public Map<String, Object> getPendingResumeFileIds(Long userId, String taskId) {
        TaskState task = requireTask(userId, taskId);

        List<Long> pending = new ArrayList<>();
        if (ALGO_RESUME_PARSE.equals(task.algoKey)) {
            pending = getPendingResumeFileIds(task);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("pendingResumeFileIds", pending);
        data.put("resumeFileIds", pending);
        return data;
    }

    @PreDestroy
    public void shutdownExecutor() {
        executor.shutdownNow();
    }

    // region Task creation

    private Map<String, Object> createJdGuideTask(Long userId, Map<String, Object> safeRequest) {
        List<Long> jdIds = parseJdIds(safeRequest);
        if (jdIds.isEmpty()) {
            throw new RuntimeException("jdIds cannot be empty");
        }

        TaskState task = newTask(userId, ALGO_JD_GUIDE, safeRequest, jdIds.size());
        task.jdIds = jdIds;
        taskStore.put(task.taskId, task);
        executor.submit(() -> runJdGuideTask(task.taskId));
        return buildTaskCreatedResponse(task);
    }

    private Map<String, Object> createResumeParseTask(Long userId, Map<String, Object> safeRequest) {
        List<Long> resumeFileIds = parseResumeFileIds(safeRequest);
        if (resumeFileIds.isEmpty()) {
            throw new RuntimeException("resumeFileIds cannot be empty");
        }

        TaskState task = newTask(userId, ALGO_RESUME_PARSE, safeRequest, resumeFileIds.size());
        task.resumeFileIds = resumeFileIds;
        taskStore.put(task.taskId, task);
        executor.submit(() -> runResumeParseTask(task.taskId));
        return buildTaskCreatedResponse(task);
    }

    private TaskState newTask(Long userId, String algoKey, Map<String, Object> request, int total) {
        TaskState task = new TaskState();
        task.taskId = generateTaskId();
        task.userId = userId;
        task.algoKey = algoKey;
        task.status = TASK_QUEUED;
        task.request = request;
        task.total = Math.max(0, total);
        task.createTime = LocalDateTime.now();
        task.updateTime = task.createTime;
        return task;
    }

    private Map<String, Object> buildTaskCreatedResponse(TaskState task) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.taskId);
        data.put("status", task.status);
        data.put("progress", 0);
        data.put("processedCount", 0);
        data.put("request", task.request);
        data.put("createTime", formatTime(task.createTime));
        return data;
    }

    // endregion

    // region task runners

    private void runJdGuideTask(String taskId) {
        TaskState task = taskStore.get(taskId);
        if (task == null || TASK_CANCELED.equals(task.status)) {
            return;
        }

        task.status = TASK_RUNNING;
        task.updateTime = LocalDateTime.now();

        List<Map<String, Object>> resultItems = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try {
            for (Long jdId : task.jdIds) {
                if (task.cancelRequested) {
                    task.status = TASK_CANCELED;
                    task.updateTime = LocalDateTime.now();
                    break;
                }

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("jdId", jdId);

                try {
                    JdJob job = jdJobMapper.selectById(jdId);
                    if (job == null) {
                        failCount++;
                        item.put("status", "failed");
                        item.put("errorMessage", "JD not found");
                    } else {
                        String guideText = buildGuideText(job);
                        upsertGuide(jdId, guideText);
                        successCount++;
                        item.put("status", "success");
                        item.put("jobTitle", job.getJobName());
                    }
                } catch (Exception ex) {
                    failCount++;
                    item.put("status", "failed");
                    item.put("errorMessage", ex.getMessage() == null ? "Generate failed" : ex.getMessage());
                }

                resultItems.add(item);
                advanceTaskProgress(task);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", task.total);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("failedCount", failCount);
            result.put("items", resultItems);
            task.result = result;

            if (!TASK_CANCELED.equals(task.status)) {
                task.status = TASK_SUCCESS;
                task.progress = 100;
            }
            task.updateTime = LocalDateTime.now();
        } catch (Exception ex) {
            task.status = TASK_FAILED;
            task.errorMessage = ex.getMessage() == null ? "Task execution failed" : ex.getMessage();
            task.updateTime = LocalDateTime.now();
        }
    }

    private void runResumeParseTask(String taskId) {
        TaskState task = taskStore.get(taskId);
        if (task == null || TASK_CANCELED.equals(task.status)) {
            return;
        }

        task.status = TASK_RUNNING;
        task.updateTime = LocalDateTime.now();

        List<Map<String, Object>> resultItems = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try {
            for (Long resumeFileId : task.resumeFileIds) {
                if (task.cancelRequested) {
                    task.status = TASK_CANCELED;
                    task.updateTime = LocalDateTime.now();
                    break;
                }

                Map<String, Object> item;
                try {
                    item = processOneResumeFile(task.userId, resumeFileId);
                    Boolean ok = item.get("ok") instanceof Boolean ? (Boolean) item.get("ok") : Boolean.FALSE;
                    if (ok) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (ResumeParseTaskException ex) {
                    failCount++;
                    item = buildParseFailureItem(resumeFileId, ex.stage, ex.errorCode, ex.retryable, ex.userHint, ex.getMessage());
                } catch (Exception ex) {
                    failCount++;
                    item = buildParseFailureItem(
                            resumeFileId,
                            "SYSTEM",
                            "SYSTEM_ERROR",
                            true,
                            "System error, please retry later.",
                            ex.getMessage() == null ? "Unexpected error" : ex.getMessage()
                    );
                }

                resultItems.add(item);
                task.completedResumeFileIds.add(resumeFileId);
                advanceTaskProgress(task);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", task.total);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("failedCount", failCount);
            result.put("items", resultItems);
            task.result = result;

            if (!TASK_CANCELED.equals(task.status)) {
                task.status = TASK_SUCCESS;
                task.progress = 100;
            }
            task.updateTime = LocalDateTime.now();
        } catch (Exception ex) {
            task.status = TASK_FAILED;
            task.errorMessage = ex.getMessage() == null ? "Task execution failed" : ex.getMessage();
            task.updateTime = LocalDateTime.now();
        }
    }

    // endregion

    // region core processors

    private Map<String, Object> processOneResumeFile(Long userId, Long resumeFileId) {
        ResumeFile resumeFile = resumeFileMapper.selectById(resumeFileId);
        if (resumeFile == null) {
            throw new ResumeParseTaskException("VALIDATION", "RESUME_FILE_NOT_FOUND", false, "Resume file not found.", "Resume file not found.");
        }

        Resume resume = resumeMapper.selectById(resumeFile.getResumeId());
        if (resume == null) {
            throw new ResumeParseTaskException("VALIDATION", "RESUME_NOT_FOUND", false, "Resume not found.", "Resume not found.");
        }
        if (!userId.equals(resume.getUserId())) {
            throw new ResumeParseTaskException("AUTH", "USER_MISMATCH", false, "No permission for this resume.", "Resume does not belong to current user.");
        }

        Path filePath = resolveStoragePath(resumeFile.getStoragePath());
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ResumeParseTaskException("VALIDATION", "FILE_NOT_FOUND", false, "Local file not found.", "Local resume file not found: " + resumeFile.getStoragePath());
        }

        ResumeContent existingContent = resumeContentMapper.selectByResumeId(resume.getId());
        if (existingContent != null && StringUtils.hasText(existingContent.getContent())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ok", true);
            item.put("status", "success");
            item.put("skipped", true);
            item.put("resumeFileId", resumeFileId);
            item.put("resumeId", resume.getId());
            item.put("resumeTitle", resume.getTitle());
            item.put("message", "Existing parsed content reused.");
            return item;
        }

        Map<String, Object> parsed = requestResumeParse(filePath, resumeFile.getFileName());
        String contentHtml = buildResumeContentHtml(resume, parsed);
        upsertResumeContent(resume.getId(), contentHtml);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("ok", true);
        item.put("status", "success");
        item.put("resumeFileId", resumeFileId);
        item.put("resumeId", resume.getId());
        item.put("resumeTitle", resume.getTitle());
        return item;
    }

    private Map<String, Object> requestResumeParse(Path filePath, String fileName) {
        if (!resumeParseEnabled) {
            throw new ResumeParseTaskException(
                    "TASK",
                    "ALGO_DISABLED",
                    false,
                    "Resume parse algorithm is disabled.",
                    "Resume parse algorithm is disabled by config."
            );
        }

        String baseUrl = normalizeBaseUrl(resumeParseBaseUrl);
        if (!StringUtils.hasText(baseUrl)) {
            throw new ResumeParseTaskException(
                    "TASK",
                    "ALGO_URL_MISSING",
                    false,
                    "Algorithm service URL is not configured.",
                    "app.resume-parse.base-url is empty."
            );
        }

        String endpoint = baseUrl + "/api/resume/parse";
        RestTemplate restTemplate = buildResumeParseRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        FileSystemResource resource = new FileSystemResource(filePath.toFile()) {
            @Override
            public String getFilename() {
                if (StringUtils.hasText(fileName)) {
                    return fileName;
                }
                return super.getFilename();
            }
        };
        body.add("file", resource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(endpoint, requestEntity, Map.class);
        } catch (ResourceAccessException ex) {
            throw new ResumeParseTaskException(
                    "NETWORK",
                    "ALGO_UNREACHABLE",
                    true,
                    "Algorithm service is unreachable. Please retry later.",
                    ex.getMessage() == null ? "Algorithm service unreachable" : ex.getMessage()
            );
        } catch (HttpStatusCodeException ex) {
            boolean retryable = ex.getStatusCode().is5xxServerError();
            throw new ResumeParseTaskException(
                    "NETWORK",
                    "ALGO_HTTP_" + ex.getStatusCode().value(),
                    retryable,
                    retryable ? "Algorithm service is busy, please retry." : "Algorithm request failed.",
                    ex.getResponseBodyAsString()
            );
        } catch (RestClientException ex) {
            throw new ResumeParseTaskException(
                    "NETWORK",
                    "ALGO_REQUEST_ERROR",
                    true,
                    "Algorithm request failed, please retry later.",
                    ex.getMessage() == null ? "Algorithm request error" : ex.getMessage()
            );
        }

        Map<String, Object> responseBody = response == null ? null : response.getBody();
        if (responseBody == null) {
            throw new ResumeParseTaskException(
                    "ALGORITHM",
                    "ALGO_EMPTY_RESPONSE",
                    true,
                    "Algorithm returned empty response.",
                    "Algorithm returned empty response body."
            );
        }

        String errorMessage = asText(pickMapValue(responseBody, "errorMessage", "error_message"));
        if (StringUtils.hasText(errorMessage)) {
            throw new ResumeParseTaskException(
                    "ALGORITHM",
                    "ALGO_RESPONSE_ERROR",
                    false,
                    "Algorithm parse failed for this file.",
                    errorMessage
            );
        }

        return responseBody;
    }

    private RestTemplate buildResumeParseRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(0, resumeParseConnectTimeoutMs));
        factory.setReadTimeout(Math.max(0, resumeParseReadTimeoutMs));
        return new RestTemplate(factory);
    }

    private String buildResumeContentHtml(Resume resume, Map<String, Object> parsed) {
        List<String> education = toStringList(pickMapValue(parsed, "EDUCATION", "education"));
        List<String> internship = toStringList(pickMapValue(parsed, "INTERNSHIP_EXPERIENCE", "internshipExperience"));
        List<String> project = toStringList(pickMapValue(parsed, "PROJECT_EXPERIENCE", "projectExperience"));
        List<String> work = toStringList(pickMapValue(parsed, "WORK_EXPERIENCE", "workExperience"));
        List<String> skills = toStringList(pickMapValue(parsed, "SKILLS", "skills"));
        List<String> awards = toStringList(pickMapValue(parsed, "AWARDS", "awards"));
        List<String> selfEval = toStringList(pickMapValue(parsed, "SELF_EVALUATION", "selfEvaluation"));

        StringBuilder sb = new StringBuilder();
        sb.append("<h1>").append(escapeHtml(safeText(resume == null ? null : resume.getTitle()))).append("</h1>");
        appendListSection(sb, "教育经历", education, true);
        appendListSection(sb, "实习经历", internship, true);
        appendListSection(sb, "项目经历", project, true);

        if (!work.isEmpty()) {
            appendListSection(sb, "工作经历", work, false);
        }
        if (!skills.isEmpty()) {
            appendListSection(sb, "技能", skills, false);
        }
        if (!awards.isEmpty()) {
            appendListSection(sb, "获奖经历", awards, false);
        }
        if (!selfEval.isEmpty()) {
            appendListSection(sb, "自我评价", selfEval, false);
        }
        return sb.toString();
    }

    private void appendListSection(StringBuilder sb, String title, List<String> items, boolean useFallback) {
        List<String> safeItems = items == null ? new ArrayList<>() : items;
        sb.append("<h2>").append(escapeHtml(title)).append("</h2>");
        sb.append("<ul>");
        if (safeItems.isEmpty()) {
            if (useFallback) {
                sb.append("<li>待补充</li>");
            }
        } else {
            for (String item : safeItems) {
                if (!StringUtils.hasText(item)) {
                    continue;
                }
                sb.append("<li>").append(escapeHtml(item)).append("</li>");
            }
        }
        sb.append("</ul>");
    }

    private List<String> toStringList(Object raw) {
        if (raw == null) {
            return new ArrayList<>();
        }

        List<String> out = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                collectListItem(out, item);
            }
            return out;
        }

        collectListItem(out, raw);
        return out;
    }

    private void collectListItem(List<String> out, Object raw) {
        if (raw == null) {
            return;
        }
        String text = String.valueOf(raw).trim();
        if (!StringUtils.hasText(text)) {
            return;
        }
        String[] parts = text.split("\\r?\\n");
        for (String part : parts) {
            String normalized = normalizeLine(part);
            if (StringUtils.hasText(normalized)) {
                out.add(normalized);
            }
        }
    }

    private String normalizeLine(String line) {
        if (line == null) {
            return "";
        }
        String text = line.trim();
        while (text.startsWith("-") || text.startsWith("*") || text.startsWith("•") || text.startsWith("·")) {
            text = text.substring(1).trim();
        }
        return text;
    }

    private void upsertResumeContent(Long resumeId, String contentHtml) {
        ResumeContent current = resumeContentMapper.selectByResumeId(resumeId);
        if (current == null) {
            ResumeContent insert = new ResumeContent();
            insert.setResumeId(resumeId);
            insert.setContent(contentHtml);
            resumeContentMapper.insert(insert);
            return;
        }
        current.setContent(contentHtml);
        resumeContentMapper.updateById(current);
    }

    // endregion

    // region helpers

    private List<Long> getPendingResumeFileIds(TaskState task) {
        List<Long> pending = new ArrayList<>();
        for (Long id : task.resumeFileIds) {
            if (id == null) {
                continue;
            }
            if (!task.completedResumeFileIds.contains(id)) {
                pending.add(id);
            }
        }
        return pending;
    }

    private void advanceTaskProgress(TaskState task) {
        task.processedCount++;
        if (task.total > 0) {
            task.progress = Math.max(0, Math.min(100, (int) Math.floor(task.processedCount * 100.0 / task.total)));
        }
        task.updateTime = LocalDateTime.now();
    }

    private void upsertGuide(Long jdId, String guideText) {
        JdGuide existing = jdGuideMapper.selectByJdId(jdId);
        if (existing == null) {
            JdGuide guide = new JdGuide();
            guide.setJdId(jdId);
            guide.setGuideText(guideText);
            jdGuideMapper.insert(guide);
            return;
        }
        JdGuide update = new JdGuide();
        update.setId(existing.getId());
        update.setGuideText(guideText);
        jdGuideMapper.updateById(update);
    }

    private String buildGuideText(JdJob job) {
        List<String> requirements = splitRequirement(job == null ? null : job.getJdCorereq());

        StringBuilder sb = new StringBuilder();
        sb.append("Role: ").append(safeText(job == null ? null : job.getJobName())).append("\n");
        sb.append("Type: ").append(jobTypeText(job == null ? null : job.getJobType())).append("\n");
        sb.append("City: ").append(defaultText(job == null ? null : job.getCity(), "N/A")).append("\n");
        sb.append("Salary: ").append(defaultText(job == null ? null : job.getSalaryRange(), "N/A")).append("\n\n");

        sb.append("Job understanding:\n");
        sb.append(defaultText(job == null ? null : job.getJdDescriptions(), "No detailed job description provided.")).append("\n\n");

        sb.append("Preparation checklist:\n");
        if (requirements.isEmpty()) {
            sb.append("1) Clarify the core skill stack and business context.\n");
            sb.append("2) Prepare 2-3 project stories with measurable outcomes.\n");
        } else {
            int idx = 1;
            for (String req : requirements) {
                sb.append(idx++).append(") Prepare project evidence for: ").append(req).append("\n");
            }
        }

        sb.append("\nInterview tips:\n");
        sb.append("1) Use STAR structure for scenario questions.\n");
        sb.append("2) Prepare a concise self-introduction aligned to this role.\n");
        sb.append("3) Prepare 2-3 thoughtful questions for the interviewer.\n");
        return sb.toString();
    }

    private TaskState requireTask(Long userId, String taskId) {
        TaskState task = findTask(userId, taskId);
        if (task == null) {
            throw new RuntimeException("Task not found");
        }
        return task;
    }

    private TaskState findTask(Long userId, String taskId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("userId cannot be empty");
        }
        if (!StringUtils.hasText(taskId)) {
            throw new RuntimeException("taskId cannot be empty");
        }
        TaskState task = taskStore.get(taskId.trim());
        if (task == null || task.userId == null || !task.userId.equals(userId)) {
            return null;
        }
        return task;
    }

    private Map<String, Object> buildExpiredTaskResponse(Long userId, String taskId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", 0);
        result.put("successCount", 0);
        result.put("failCount", 0);
        result.put("failedCount", 0);
        result.put("items", Collections.emptyList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", taskId == null ? "" : taskId.trim());
        data.put("userId", userId);
        data.put("algoKey", "");
        data.put("status", TASK_SUCCESS);
        data.put("progress", 100);
        data.put("processedCount", 0);
        data.put("request", Collections.emptyMap());
        data.put("result", result);
        data.put("errorMessage", null);
        data.put("createTime", null);
        data.put("updateTime", null);
        return data;
    }

    private Map<String, Object> toTaskMap(TaskState task) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", task.taskId);
        data.put("userId", task.userId);
        data.put("algoKey", task.algoKey);
        data.put("status", task.status);
        data.put("progress", task.progress);
        data.put("processedCount", task.processedCount);
        data.put("request", task.request);
        data.put("result", task.result);
        data.put("errorMessage", task.errorMessage);
        data.put("createTime", formatTime(task.createTime));
        data.put("updateTime", formatTime(task.updateTime));
        return data;
    }

    private Map<String, Object> safeMap(Map<String, Object> source) {
        if (source == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(source);
    }

    private List<Long> parseJdIds(Map<String, Object> request) {
        Map<String, Object> options = asMap(request.get("options"));
        Set<Long> idSet = new LinkedHashSet<>();
        addLongList(idSet, options.get("jdIds"));
        Long oneId = toLong(options.get("jdId"));
        if (oneId != null && oneId > 0) {
            idSet.add(oneId);
        }
        return new ArrayList<>(idSet);
    }

    private List<Long> parseResumeFileIds(Map<String, Object> request) {
        Set<Long> idSet = new LinkedHashSet<>();
        addLongList(idSet, request.get("resumeFileIds"));
        Map<String, Object> options = asMap(request.get("options"));
        addLongList(idSet, options.get("resumeFileIds"));
        return new ArrayList<>(idSet);
    }

    private void addLongList(Set<Long> sink, Object raw) {
        if (!(raw instanceof List<?> list)) {
            return;
        }
        for (Object value : list) {
            Long id = toLong(value);
            if (id != null && id > 0) {
                sink.add(id);
            }
        }
    }

    private Map<String, Object> asMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private Object pickMapValue(Map<String, Object> data, String... keys) {
        if (data == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (!StringUtils.hasText(key)) {
                continue;
            }
            if (data.containsKey(key)) {
                return data.get(key);
            }
        }
        return null;
    }

    private String asText(Object raw) {
        return raw == null ? "" : String.valueOf(raw).trim();
    }

    private Map<String, Object> buildParseFailureItem(Long resumeFileId,
                                                      String stage,
                                                      String errorCode,
                                                      boolean retryable,
                                                      String userHint,
                                                      String errorMessage) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("ok", false);
        item.put("status", "failed");
        item.put("resumeFileId", resumeFileId);
        item.put("stage", stage);
        item.put("errorCode", errorCode);
        item.put("retryable", retryable);
        item.put("userHint", userHint);
        item.put("errorMessage", errorMessage);
        return item;
    }

    private Path resolveStoragePath(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            throw new ResumeParseTaskException(
                    "VALIDATION",
                    "FILE_PATH_EMPTY",
                    false,
                    "File path is empty.",
                    "Resume file storage path is empty."
            );
        }
        String normalized = storagePath.replace("\\", "/");
        Path path = Paths.get(normalized);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        Path base = Paths.get(resumeStorageDir).toAbsolutePath().normalize();
        return base.resolve(normalized).normalize();
    }

    private String normalizeBaseUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String v = raw.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private List<String> splitRequirement(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new ArrayList<>();
        }
        String normalized = raw.replace("，", ",").replace("。", ",").replace("/", ",");
        String[] parts = normalized.split("[,;]");
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            String text = safeText(part);
            if (!StringUtils.hasText(text)) {
                continue;
            }
            list.add(text);
            if (list.size() >= 5) {
                break;
            }
        }
        if (list.isEmpty()) {
            list.add(safeText(raw));
        }
        return list;
    }

    private String jobTypeText(Integer jobType) {
        if (jobType == null) {
            return "Unknown";
        }
        return switch (jobType) {
            case 1 -> "Campus";
            case 2 -> "Experienced";
            case 3 -> "Internship";
            default -> "Other";
        };
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultText(String value, String fallback) {
        String text = safeText(value);
        return text.isEmpty() ? fallback : text;
    }

    private String escapeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(API_TIME);
    }

    private String generateTaskId() {
        return "task_" + UUID.randomUUID().toString().replace("-", "");
    }

    // endregion

    private static class ResumeParseTaskException extends RuntimeException {
        private final String stage;
        private final String errorCode;
        private final boolean retryable;
        private final String userHint;

        private ResumeParseTaskException(String stage, String errorCode, boolean retryable, String userHint, String message) {
            super(message);
            this.stage = stage;
            this.errorCode = errorCode;
            this.retryable = retryable;
            this.userHint = userHint;
        }
    }

    private static class TaskState {
        private String taskId;
        private Long userId;
        private String algoKey;
        private String status;
        private int progress;
        private int processedCount;
        private int total;
        private List<Long> jdIds = new ArrayList<>();
        private List<Long> resumeFileIds = new ArrayList<>();
        private Set<Long> completedResumeFileIds = ConcurrentHashMap.newKeySet();
        private Map<String, Object> request = new LinkedHashMap<>();
        private Map<String, Object> result;
        private String errorMessage;
        private boolean cancelRequested;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }
}
