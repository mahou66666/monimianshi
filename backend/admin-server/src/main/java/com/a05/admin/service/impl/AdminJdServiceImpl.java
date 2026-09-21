package com.a05.admin.service.impl;

import com.a05.admin.controller.dto.AdminJdQuery;
import com.a05.admin.entity.JdGuide;
import com.a05.admin.entity.JdJob;
import com.a05.admin.mapper.JdGuideMapper;
import com.a05.admin.mapper.JdJobMapper;
import com.a05.admin.service.AdminJdService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AdminJdServiceImpl implements AdminJdService {

    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter JOB_CODE_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private static final Pattern TITLE_LINE_PATTERN = Pattern.compile(
            "(?im)^(?:\\u5c97\\u4f4d\\u540d\\u79f0|\\u804c\\u4f4d\\u540d\\u79f0|\\u5c97\\u4f4d|\\u804c\\u4f4d|job\\s*title|title)\\s*[:\\uff1a]\\s*(.+)$");
    private static final Pattern REQUIRE_LINE_PATTERN = Pattern.compile(
            "(?im)^(?:\\u4efb\\u804c\\u8981\\u6c42|\\u5c97\\u4f4d\\u8981\\u6c42|\\u804c\\u4f4d\\u8981\\u6c42|\\u5c97\\u4f4d\\u6838\\u5fc3\\u8981\\u6c42|\\u6838\\u5fc3\\u8981\\u6c42|requirements?)\\s*[:\\uff1a]\\s*(.+)$");
    private static final Pattern CITY_LINE_PATTERN = Pattern.compile(
            "(?im)^(?:\\u5de5\\u4f5c\\u5730\\u70b9|\\u5730\\u70b9|\\u57ce\\u5e02|city)\\s*[:\\uff1a]\\s*([^\\n]+)$");
    private static final Pattern SALARY_PATTERN = Pattern.compile(
            "(\\d{1,2}\\s*[kK]\\s*[-~\\u81f3]\\s*\\d{1,2}\\s*[kK]|\\d{2,4}\\s*[-~\\u81f3]\\s*\\d{2,4}\\s*/?\\u5929)");

    private static final Pattern INLINE_LABEL_PATTERN = Pattern.compile(
            "(?i)(\\u5c97\\u4f4d\\u540d\\u79f0|\\u804c\\u4f4d\\u540d\\u79f0|\\u5c97\\u4f4d|\\u804c\\u4f4d|job\\s*title|title|" +
                    "\\u5de5\\u4f5c\\u5730\\u70b9|\\u5730\\u70b9|\\u57ce\\u5e02|city|" +
                    "\\u85aa\\u8d44|\\u85aa\\u916c|salary|" +
                    "\\u5c97\\u4f4d\\u63cf\\u8ff0|\\u804c\\u4f4d\\u63cf\\u8ff0|\\u5de5\\u4f5c\\u5185\\u5bb9|jd\\s*description|description|" +
                    "\\u4efb\\u804c\\u8981\\u6c42|\\u5c97\\u4f4d\\u8981\\u6c42|\\u804c\\u4f4d\\u8981\\u6c42|\\u6838\\u5fc3\\u8981\\u6c42|requirements?)\\s*[:\\uff1a]\\s*");

    private static final Pattern META_LINE_PATTERN = Pattern.compile(
            "(?i)^(?:\\u5c97\\u4f4d\\u540d\\u79f0|\\u804c\\u4f4d\\u540d\\u79f0|\\u5c97\\u4f4d|\\u804c\\u4f4d|" +
                    "\\u5de5\\u4f5c\\u5730\\u70b9|\\u5730\\u70b9|\\u57ce\\u5e02|\\u85aa\\u8d44|\\u85aa\\u916c|" +
                    "\\u4efb\\u804c\\u8981\\u6c42|\\u5c97\\u4f4d\\u8981\\u6c42|\\u804c\\u4f4d\\u8981\\u6c42|\\u6838\\u5fc3\\u8981\\u6c42)\\s*[:\\uff1a].*$");

    private static final String[] COMMON_CITIES = {
            "\u5317\u4eac", "\u4e0a\u6d77", "\u6df1\u5733", "\u5e7f\u5dde", "\u676d\u5dde", "\u6210\u90fd", "\u6b66\u6c49",
            "\u897f\u5b89", "\u82cf\u5dde", "\u5357\u4eac", "\u957f\u6c99", "\u53a6\u95e8", "\u9752\u5c9b", "\u5929\u6d25",
            "\u91cd\u5e86", "\u6c88\u9633"
    };

    @Autowired
    private JdJobMapper jdJobMapper;

    @Autowired
    private JdGuideMapper jdGuideMapper;

    @Override
    public Map<String, Object> listJobs(AdminJdQuery query) {
        AdminJdQuery safeQuery = query == null ? new AdminJdQuery() : query;
        int pageNo = normalizePageNo(safeQuery.getPageNo());
        int pageSize = normalizePageSize(safeQuery.getPageSize());

        List<JdJob> matched = queryJobs(safeQuery);
        int total = matched.size();
        List<JdJob> paged = slice(matched, pageNo, pageSize);

        List<Map<String, Object>> items = paged.stream().map(this::toJobItem).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("pageNo", pageNo);
        data.put("pageSize", pageSize);
        data.put("total", total);
        data.put("items", items);
        return data;
    }

    @Override
    public Map<String, Object> listGuideJobs(AdminJdQuery query) {
        AdminJdQuery safeQuery = query == null ? new AdminJdQuery() : query;
        int pageNo = normalizePageNo(safeQuery.getPageNo());
        int pageSize = normalizePageSize(safeQuery.getPageSize());

        List<JdJob> matched = queryJobs(safeQuery);
        int total = matched.size();
        List<JdJob> paged = slice(matched, pageNo, pageSize);
        Map<Long, JdGuide> guideMap = loadGuideMap(paged);

        List<Map<String, Object>> items = new ArrayList<>();
        for (JdJob job : paged) {
            Map<String, Object> item = toJobItem(job);
            item.put("rawText", composeRawText(job));
            JdGuide guide = guideMap.get(job.getId());
            item.put("currentGuideText", guide == null ? null : guide.getGuideText());
            item.put("guideUpdateTime", guide == null ? null : formatTime(guide.getUpdateTime()));
            items.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("pageNo", pageNo);
        data.put("pageSize", pageSize);
        data.put("total", total);
        data.put("items", items);
        return data;
    }

    @Override
    public Map<String, Object> getLatestGuide(Long jdId) {
        JdJob job = requireJob(jdId);
        JdGuide guide = jdGuideMapper.selectByJdId(jdId);

        Map<String, Object> data = new HashMap<>();
        data.put("jdId", job.getId());
        data.put("jobTitle", job.getJobName());
        data.put("rawText", composeRawText(job));
        data.put("guideId", guide == null ? null : guide.getId());
        data.put("currentGuideText", guide == null ? null : guide.getGuideText());
        data.put("updateTime", guide == null ? null : formatTime(guide.getUpdateTime()));
        return data;
    }

    @Override
    public Map<String, Object> clearGuide(Long jdId) {
        requireJob(jdId);
        int affected = jdGuideMapper.deleteByJdId(jdId);
        Map<String, Object> data = new HashMap<>();
        data.put("jdId", jdId);
        data.put("success", true);
        data.put("affected", affected);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importTextBatch(String text, Long companyId) {
        if (!StringUtils.hasText(text)) {
            throw new RuntimeException("JD text cannot be empty");
        }

        List<String> blocks = splitTextBlocks(text);
        if (blocks.isEmpty()) {
            throw new RuntimeException("No valid JD content detected");
        }

        Long targetCompanyId = (companyId == null || companyId <= 0) ? 1L : companyId;
        int createdCount = 0;
        int invalidCount = 0;
        int duplicateCount = 0;

        List<Map<String, Object>> items = new ArrayList<>();
        int serial = 0;
        for (String block : blocks) {
            String content = safeText(block);
            if (!StringUtils.hasText(content)) {
                invalidCount++;
                continue;
            }

            Map<String, String> labeledFields = parseLabeledFields(content);
            String jobName = extractJobName(content, serial + 1, labeledFields);
            String city = extractCity(content, labeledFields);
            String salary = extractSalary(content, labeledFields);
            String req = trimToMax(extractCoreRequirements(content, labeledFields), 255);
            String desc = trimToMax(extractJobDescription(content, labeledFields), 8000);

            if (isDuplicateJob(targetCompanyId, jobName, city)) {
                duplicateCount++;
                Map<String, Object> duplicateItem = new LinkedHashMap<>();
                duplicateItem.put("status", "skipped");
                duplicateItem.put("reason", "duplicate");
                duplicateItem.put("jobTitle", jobName);
                duplicateItem.put("city", city);
                items.add(duplicateItem);
                continue;
            }

            serial++;
            int jobType = inferJobType(content, jobName);
            String jobCode = generateUniqueJobCode(jobType, serial);

            JdJob job = new JdJob();
            job.setJobCode(jobCode);
            job.setJobName(trimToMax(jobName, 100));
            job.setCompanyId(targetCompanyId);
            job.setJdDescriptions(desc);
            job.setJdCorereq(req);
            job.setJobType(jobType);
            job.setSalaryRange(trimToMax(salary, 50));
            job.setCity(trimToMax(city, 50));
            job.setStatus(1);
            jdJobMapper.insert(job);
            createdCount++;

            Map<String, Object> createdItem = new LinkedHashMap<>();
            createdItem.put("status", "created");
            createdItem.put("id", job.getId());
            createdItem.put("jobCode", job.getJobCode());
            createdItem.put("jobTitle", job.getJobName());
            createdItem.put("jobType", job.getJobType());
            createdItem.put("city", job.getCity());
            items.add(createdItem);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("companyId", targetCompanyId);
        data.put("totalBlocks", blocks.size());
        data.put("createdCount", createdCount);
        data.put("duplicateCount", duplicateCount);
        data.put("invalidCount", invalidCount);
        data.put("skippedCount", duplicateCount + invalidCount);
        data.put("items", items);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> extractBatch(List<Long> jdIds) {
        Set<Long> normalizedIds = normalizeJdIds(jdIds);
        List<Map<String, Object>> items = new ArrayList<>();

        int updatedCount = 0;
        int skippedCount = 0;
        int missingCount = 0;

        for (Long jdId : normalizedIds) {
            JdJob job = jdJobMapper.selectById(jdId);
            if (job == null) {
                missingCount++;
                continue;
            }

            String newReq = trimToMax(extractCoreRequirements(job.getJdDescriptions()), 255);
            String oldReq = safeText(job.getJdCorereq());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", job.getId());
            item.put("jobTitle", job.getJobName());

            if (!StringUtils.hasText(newReq)) {
                skippedCount++;
                item.put("status", "skipped");
                item.put("reason", "no_requirement_detected");
                items.add(item);
                continue;
            }

            if (newReq.equals(oldReq)) {
                skippedCount++;
                item.put("status", "skipped");
                item.put("reason", "no_change");
                item.put("jobReq", oldReq);
                items.add(item);
                continue;
            }

            JdJob update = new JdJob();
            update.setId(job.getId());
            update.setJdCorereq(newReq);
            jdJobMapper.updateById(update);
            updatedCount++;

            item.put("status", "updated");
            item.put("jobReq", newReq);
            items.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", normalizedIds.size());
        data.put("updatedCount", updatedCount);
        data.put("skippedCount", skippedCount);
        data.put("missingCount", missingCount);
        data.put("items", items);
        return data;
    }

    private List<JdJob> queryJobs(AdminJdQuery query) {
        LambdaQueryWrapper<JdJob> wrapper = new LambdaQueryWrapper<JdJob>().orderByDesc(JdJob::getId);
        if (query.getJobType() != null) {
            wrapper.eq(JdJob::getJobType, query.getJobType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(JdJob::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(JdJob::getJobName, keyword)
                    .or().like(JdJob::getJobCode, keyword)
                    .or().like(JdJob::getJdDescriptions, keyword)
                    .or().like(JdJob::getJdCorereq, keyword)
                    .or().like(JdJob::getCity, keyword));
        }
        return jdJobMapper.selectList(wrapper);
    }

    private Map<Long, JdGuide> loadGuideMap(List<JdJob> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return Map.of();
        }
        Set<Long> jdIds = jobs.stream().map(JdJob::getId).filter(id -> id != null && id > 0).collect(Collectors.toSet());
        if (jdIds.isEmpty()) {
            return Map.of();
        }

        List<JdGuide> guides = jdGuideMapper.selectList(new LambdaQueryWrapper<JdGuide>().in(JdGuide::getJdId, jdIds));
        if (guides == null || guides.isEmpty()) {
            return Map.of();
        }

        Map<Long, JdGuide> map = new LinkedHashMap<>();
        for (JdGuide guide : guides) {
            if (guide != null && guide.getJdId() != null) {
                map.put(guide.getJdId(), guide);
            }
        }
        return map;
    }

    private Map<String, Object> toJobItem(JdJob job) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", job.getId());
        item.put("jobCode", job.getJobCode());
        item.put("jobTitle", job.getJobName());
        item.put("companyId", job.getCompanyId());
        item.put("jobDesc", job.getJdDescriptions());
        item.put("jobReq", job.getJdCorereq());
        item.put("jobType", job.getJobType());
        item.put("jobTypeText", toJobTypeText(job.getJobType()));
        item.put("salary", job.getSalaryRange());
        item.put("city", job.getCity());
        item.put("status", job.getStatus());
        item.put("createTime", formatTime(job.getCreateTime()));
        item.put("updateTime", formatTime(job.getUpdateTime()));
        return item;
    }

    private String composeRawText(JdJob job) {
        String desc = safeText(job.getJdDescriptions());
        String req = safeText(job.getJdCorereq());
        if (StringUtils.hasText(desc) && StringUtils.hasText(req)) {
            return desc + "\n\nCore Requirements:\n" + req;
        }
        if (StringUtils.hasText(desc)) {
            return desc;
        }
        return req;
    }

    private JdJob requireJob(Long jdId) {
        if (jdId == null || jdId <= 0) {
            throw new RuntimeException("jdId cannot be empty");
        }
        JdJob job = jdJobMapper.selectById(jdId);
        if (job == null) {
            throw new RuntimeException("JD does not exist");
        }
        return job;
    }

    private String toJobTypeText(Integer type) {
        if (type == null) {
            return "unknown";
        }
        return switch (type) {
            case 1 -> "campus";
            case 2 -> "social";
            case 3 -> "internship";
            default -> "unknown";
        };
    }

    private List<JdJob> slice(List<JdJob> list, int pageNo, int pageSize) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        int total = list.size();
        int fromIndex = Math.max(0, (pageNo - 1) * pageSize);
        if (fromIndex >= total) {
            return new ArrayList<>();
        }
        int toIndex = Math.min(total, fromIndex + pageSize);
        return list.subList(fromIndex, toIndex);
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 200);
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(API_TIME);
    }

    private List<String> splitTextBlocks(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] parts = normalized.split("\\n\\s*\\n+");
        List<String> blocks = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                blocks.add(part.trim());
            }
        }
        return blocks;
    }

    private int inferJobType(String content, String jobName) {
        String merged = (safeText(content) + " " + safeText(jobName)).toLowerCase(Locale.ROOT);
        if (merged.contains("intern") || merged.contains("\u5b9e\u4e60")) {
            return 3;
        }
        if (merged.contains("\u6821\u62db")) {
            return 1;
        }
        return 2;
    }

    private String extractJobName(String content, int serial, Map<String, String> labeledFields) {
        String byLabel = labeledFields.get("name");
        if (StringUtils.hasText(byLabel)) {
            return trimToMax(byLabel, 100);
        }

        Matcher m = TITLE_LINE_PATTERN.matcher(content);
        if (m.find()) {
            String title = trimToMax(m.group(1), 100);
            if (StringUtils.hasText(title)) {
                return title;
            }
        }

        String[] lines = content.split("\\n");
        for (String line : lines) {
            String cleaned = safeText(line).replaceAll("^[\\-\\*•\\d.\\s]+", "");
            if (cleaned.length() > 2) {
                return trimToMax(cleaned, 100);
            }
        }
        return "Auto Imported JD " + serial;
    }

    private String extractSalary(String content, Map<String, String> labeledFields) {
        String byLabel = labeledFields.get("salary");
        if (StringUtils.hasText(byLabel)) {
            return trimToMax(byLabel.replaceAll("\\s+", ""), 50);
        }

        Matcher m = SALARY_PATTERN.matcher(content);
        if (m.find()) {
            return trimToMax(m.group(1).replaceAll("\\s+", ""), 50);
        }
        return "";
    }

    private String extractCity(String content, Map<String, String> labeledFields) {
        String byLabel = labeledFields.get("city");
        if (StringUtils.hasText(byLabel)) {
            String city = byLabel.replaceAll("[,，;；。].*$", "").trim();
            return trimToMax(city, 50);
        }

        Matcher m = CITY_LINE_PATTERN.matcher(content);
        if (m.find()) {
            String city = m.group(1).replaceAll("[,，;；。].*$", "").trim();
            return trimToMax(city, 50);
        }

        for (String city : COMMON_CITIES) {
            if (content.contains(city)) {
                return city;
            }
        }
        return "";
    }

    private String extractCoreRequirements(String content, Map<String, String> labeledFields) {
        String byLabel = labeledFields.get("requirement");
        if (StringUtils.hasText(byLabel)) {
            return trimToMax(cleanInlineLabelValue(byLabel), 255);
        }

        Matcher reqLineMatcher = REQUIRE_LINE_PATTERN.matcher(content);
        if (reqLineMatcher.find()) {
            return trimToMax(cleanInlineLabelValue(reqLineMatcher.group(1)), 255);
        }

        List<String> candidates = new ArrayList<>();
        String[] lines = content.split("\\n");
        for (String line : lines) {
            String cleaned = safeText(line);
            if (cleaned.isEmpty()) {
                continue;
            }
            if (containsRequirementKeyword(cleaned)) {
                cleaned = cleaned.replaceAll("^[\\-\\*•\\d.\\s]+", "");
                candidates.add(cleaned);
            }
            if (candidates.size() >= 4) {
                break;
            }
        }
        if (!candidates.isEmpty()) {
            return trimToMax(String.join("；", candidates), 255);
        }
        return "";
    }

    private String extractCoreRequirements(String content) {
        return extractCoreRequirements(content, parseLabeledFields(content));
    }

    private String extractJobDescription(String content, Map<String, String> labeledFields) {
        String byLabel = labeledFields.get("description");
        if (StringUtils.hasText(byLabel)) {
            return trimToMax(cleanInlineLabelValue(byLabel), 8000);
        }

        StringBuilder builder = new StringBuilder();
        String[] lines = content.split("\\n");
        for (String line : lines) {
            String trimmed = safeText(line);
            if (trimmed.isEmpty()) {
                continue;
            }
            if (META_LINE_PATTERN.matcher(trimmed).matches()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(trimmed);
        }

        String description = safeText(builder.toString());
        if (!StringUtils.hasText(description)) {
            description = safeText(content);
        }
        return trimToMax(description, 8000);
    }

    private String cleanInlineLabelValue(String value) {
        String cleaned = safeText(value);
        cleaned = cleaned.replaceAll(
                "(?i)(?:\\u5c97\\u4f4d\\u540d\\u79f0|\\u804c\\u4f4d\\u540d\\u79f0|\\u5c97\\u4f4d|\\u804c\\u4f4d|" +
                        "\\u5de5\\u4f5c\\u5730\\u70b9|\\u5730\\u70b9|\\u57ce\\u5e02|\\u85aa\\u8d44|\\u85aa\\u916c|" +
                        "\\u5c97\\u4f4d\\u63cf\\u8ff0|\\u804c\\u4f4d\\u63cf\\u8ff0|\\u5de5\\u4f5c\\u5185\\u5bb9|" +
                        "\\u4efb\\u804c\\u8981\\u6c42|\\u5c97\\u4f4d\\u8981\\u6c42|\\u804c\\u4f4d\\u8981\\u6c42|\\u6838\\u5fc3\\u8981\\u6c42|requirements?|job\\s*title|title|salary|city|description|jd\\s*description)\\s*[:\\uff1a]",
                " ");
        return cleaned.replaceAll("\\s{2,}", " ").trim();
    }

    private Map<String, String> parseLabeledFields(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        Matcher matcher = INLINE_LABEL_PATTERN.matcher(normalized);
        List<FieldIndex> indexes = new ArrayList<>();
        while (matcher.find()) {
            String key = canonicalFieldKey(matcher.group(1));
            if (key == null) {
                continue;
            }
            indexes.add(new FieldIndex(key, matcher.start(), matcher.end()));
        }

        Map<String, String> result = new LinkedHashMap<>();
        if (indexes.isEmpty()) {
            return result;
        }

        for (int i = 0; i < indexes.size(); i++) {
            FieldIndex current = indexes.get(i);
            int valueStart = current.valueStart;
            int valueEnd = i + 1 < indexes.size() ? indexes.get(i + 1).labelStart : normalized.length();
            if (valueStart >= valueEnd) {
                continue;
            }
            String value = safeText(normalized.substring(valueStart, valueEnd));
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if (!result.containsKey(current.key)) {
                result.put(current.key, value);
            }
        }
        return result;
    }

    private String canonicalFieldKey(String rawKey) {
        String key = safeText(rawKey).toLowerCase(Locale.ROOT);
        if (key.contains("job") && key.contains("title")) {
            return "name";
        }
        if ("title".equals(key)) {
            return "name";
        }
        if (key.contains("\u540d\u79f0") || key.equals("\u5c97\u4f4d") || key.equals("\u804c\u4f4d")) {
            return "name";
        }
        if (key.contains("city") || key.contains("\u57ce\u5e02") || key.contains("\u5730\u70b9")) {
            return "city";
        }
        if (key.contains("salary") || key.contains("\u85aa")) {
            return "salary";
        }
        if (key.contains("description") || key.contains("\u63cf\u8ff0") || key.contains("\u5185\u5bb9")) {
            return "description";
        }
        if (key.contains("requirement") || key.contains("\u8981\u6c42")) {
            return "requirement";
        }
        return null;
    }

    private boolean containsRequirementKeyword(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("experience")
                || lower.contains("familiar")
                || line.contains("\u719f\u6089")
                || line.contains("\u7cbe\u901a")
                || line.contains("\u638c\u63e1")
                || line.contains("\u4e86\u89e3")
                || line.contains("\u5177\u5907")
                || line.contains("\u7ecf\u9a8c")
                || line.contains("\u80fd\u529b");
    }

    private boolean isDuplicateJob(Long companyId, String jobName, String city) {
        String normalizedName = safeText(jobName);
        if (!StringUtils.hasText(normalizedName)) {
            return false;
        }
        LambdaQueryWrapper<JdJob> wrapper = new LambdaQueryWrapper<JdJob>()
                .eq(JdJob::getCompanyId, companyId)
                .eq(JdJob::getJobName, normalizedName);

        String normalizedCity = safeText(city);
        if (StringUtils.hasText(normalizedCity)) {
            wrapper.eq(JdJob::getCity, normalizedCity);
        }

        Long count = jdJobMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private Set<Long> normalizeJdIds(List<Long> jdIds) {
        Set<Long> set = new LinkedHashSet<>();
        if (jdIds == null) {
            return set;
        }
        for (Long id : jdIds) {
            if (id != null && id > 0) {
                set.add(id);
            }
        }
        return set;
    }

    private String generateUniqueJobCode(int jobType, int serial) {
        String prefix = switch (jobType) {
            case 1 -> "CAMP";
            case 3 -> "INTERN";
            default -> "SOC";
        };
        String baseTime = LocalDateTime.now().format(JOB_CODE_TIME);
        int seed = Math.max(serial, 1);

        for (int i = 0; i < 100; i++) {
            String code = prefix + "-" + baseTime + "-" + String.format("%03d", (seed + i) % 1000);
            Long count = jdJobMapper.selectCount(new LambdaQueryWrapper<JdJob>().eq(JdJob::getJobCode, code));
            if (count == null || count == 0) {
                return code;
            }
        }
        return trimToMax(prefix + "-" + System.currentTimeMillis(), 50);
    }

    private String trimToMax(String value, int maxLen) {
        String trimmed = safeText(value);
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static class FieldIndex {
        private final String key;
        private final int labelStart;
        private final int valueStart;

        private FieldIndex(String key, int labelStart, int valueStart) {
            this.key = key;
            this.labelStart = labelStart;
            this.valueStart = valueStart;
        }
    }
}
