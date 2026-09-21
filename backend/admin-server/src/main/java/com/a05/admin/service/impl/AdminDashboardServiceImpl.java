package com.a05.admin.service.impl;

import com.a05.admin.entity.JdGuide;
import com.a05.admin.entity.JdJob;
import com.a05.admin.entity.Resume;
import com.a05.admin.entity.ResumeProblem;
import com.a05.admin.entity.UserInfo;
import com.a05.admin.mapper.JdGuideMapper;
import com.a05.admin.mapper.JdJobMapper;
import com.a05.admin.mapper.ResumeMapper;
import com.a05.admin.mapper.ResumeProblemMapper;
import com.a05.admin.mapper.UserInfoMapper;
import com.a05.admin.service.AdminDashboardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    private final UserInfoMapper userInfoMapper;
    private final ResumeMapper resumeMapper;
    private final JdJobMapper jdJobMapper;
    private final JdGuideMapper jdGuideMapper;
    private final ResumeProblemMapper resumeProblemMapper;

    public AdminDashboardServiceImpl(UserInfoMapper userInfoMapper,
                                     ResumeMapper resumeMapper,
                                     JdJobMapper jdJobMapper,
                                     JdGuideMapper jdGuideMapper,
                                     ResumeProblemMapper resumeProblemMapper) {
        this.userInfoMapper = userInfoMapper;
        this.resumeMapper = resumeMapper;
        this.jdJobMapper = jdJobMapper;
        this.jdGuideMapper = jdGuideMapper;
        this.resumeProblemMapper = resumeProblemMapper;
    }

    @Override
    public Map<String, Object> getOverview() {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(6);
        LocalDateTime fromTime = fromDate.atStartOfDay();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", buildSummary(fromTime));
        data.put("trends", buildTrendData(fromDate, today, fromTime));
        data.put("cityStats", buildCityStats());
        data.put("recentResumes", buildRecentResumes());
        data.put("recentJds", buildRecentJds());
        data.put("recentUsers", buildRecentUsers());
        return data;
    }

    private Map<String, Object> buildSummary(LocalDateTime fromTime) {
        long totalUsers = safeCount(userInfoMapper.selectCount(
                new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getIsDeleted, 0)));
        long activeUsers = safeCount(userInfoMapper.selectCount(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getIsDeleted, 0)
                        .eq(UserInfo::getStatus, 1)));
        long totalResumes = safeCount(resumeMapper.selectCount(new LambdaQueryWrapper<Resume>()));
        long totalJds = safeCount(jdJobMapper.selectCount(new LambdaQueryWrapper<JdJob>()));
        long totalGuides = safeCount(jdGuideMapper.selectCount(new LambdaQueryWrapper<JdGuide>()));
        long totalProblems = safeCount(resumeProblemMapper.selectCount(new LambdaQueryWrapper<ResumeProblem>()));
        long resumesLast7Days = safeCount(resumeMapper.selectCount(
                new LambdaQueryWrapper<Resume>().ge(Resume::getCreateTime, fromTime)));
        long jdsLast7Days = safeCount(jdJobMapper.selectCount(
                new LambdaQueryWrapper<JdJob>().ge(JdJob::getCreateTime, fromTime)));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalUsers", totalUsers);
        summary.put("activeUsers", activeUsers);
        summary.put("totalResumes", totalResumes);
        summary.put("totalJds", totalJds);
        summary.put("totalGuides", totalGuides);
        summary.put("totalProblems", totalProblems);
        summary.put("resumesLast7Days", resumesLast7Days);
        summary.put("jdsLast7Days", jdsLast7Days);
        summary.put("guideCoveragePercent", calcPercent(totalGuides, totalJds));
        return summary;
    }

    private Map<String, Object> buildTrendData(LocalDate fromDate, LocalDate toDate, LocalDateTime fromTime) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            dates.add(date);
        }

        Map<LocalDate, Integer> userMap = aggregateByDate(
                userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getIsDeleted, 0)
                        .ge(UserInfo::getCreateTime, fromTime)),
                UserInfo::getCreateTime
        );
        Map<LocalDate, Integer> resumeMap = aggregateByDate(
                resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                        .ge(Resume::getCreateTime, fromTime)),
                Resume::getCreateTime
        );
        Map<LocalDate, Integer> jdMap = aggregateByDate(
                jdJobMapper.selectList(new LambdaQueryWrapper<JdJob>()
                        .ge(JdJob::getCreateTime, fromTime)),
                JdJob::getCreateTime
        );

        List<String> labels = new ArrayList<>();
        List<Integer> users = new ArrayList<>();
        List<Integer> resumes = new ArrayList<>();
        List<Integer> jds = new ArrayList<>();

        for (LocalDate date : dates) {
            labels.add(date.format(DAY_LABEL));
            users.add(userMap.getOrDefault(date, 0));
            resumes.add(resumeMap.getOrDefault(date, 0));
            jds.add(jdMap.getOrDefault(date, 0));
        }

        Map<String, Object> trends = new LinkedHashMap<>();
        trends.put("labels", labels);
        trends.put("users", users);
        trends.put("resumes", resumes);
        trends.put("jds", jds);
        return trends;
    }

    private List<Map<String, Object>> buildCityStats() {
        List<JdJob> jobs = jdJobMapper.selectList(new LambdaQueryWrapper<JdJob>().orderByDesc(JdJob::getId));
        if (jobs == null || jobs.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Long> grouped = jobs.stream()
                .map(JdJob::getCity)
                .map(this::normalizeCity)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<Map<String, Object>> items = grouped.entrySet().stream()
                .filter(entry -> !"未填写".equals(entry.getKey()))
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(6)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        if (!items.isEmpty()) {
            return items;
        }

        return grouped.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(6)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildRecentResumes() {
        List<Resume> resumes = resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                .orderByDesc(Resume::getUpdateTime)
                .orderByDesc(Resume::getId)
                .last("LIMIT 5"));
        if (resumes == null || resumes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = resumes.stream()
                .map(Resume::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, String> userNameMap = loadUserNameMap(userIds);

        return resumes.stream().map(resume -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", resume.getId());
            item.put("title", StringUtils.hasText(resume.getTitle()) ? resume.getTitle() : "未命名简历");
            item.put("userName", userNameMap.getOrDefault(resume.getUserId(), "未知用户"));
            item.put("updateTime", formatTime(resume.getUpdateTime(), resume.getCreateTime()));
            return item;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildRecentJds() {
        List<JdJob> jobs = jdJobMapper.selectList(new LambdaQueryWrapper<JdJob>()
                .orderByDesc(JdJob::getUpdateTime)
                .orderByDesc(JdJob::getId)
                .last("LIMIT 5"));
        if (jobs == null || jobs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> jdIds = jobs.stream()
                .map(JdJob::getId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Set<Long> guidedIds = jdIds.isEmpty()
                ? Collections.emptySet()
                : jdGuideMapper.selectList(new LambdaQueryWrapper<JdGuide>().in(JdGuide::getJdId, jdIds))
                .stream()
                .map(JdGuide::getJdId)
                .collect(Collectors.toSet());

        return jobs.stream().map(job -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", job.getId());
            item.put("jobName", StringUtils.hasText(job.getJobName()) ? job.getJobName() : "未命名JD");
            item.put("city", normalizeCity(job.getCity()));
            item.put("salaryRange", StringUtils.hasText(job.getSalaryRange()) ? job.getSalaryRange() : "未填写");
            item.put("hasGuide", guidedIds.contains(job.getId()));
            item.put("updateTime", formatTime(job.getUpdateTime(), job.getCreateTime()));
            return item;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildRecentUsers() {
        List<UserInfo> users = userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getIsDeleted, 0)
                .orderByDesc(UserInfo::getLastActiveTime)
                .orderByDesc(UserInfo::getId)
                .last("LIMIT 5"));
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        return users.stream().map(user -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", user.getId());
            item.put("userName", StringUtils.hasText(user.getUserName()) ? user.getUserName() : "未命名用户");
            item.put("phone", user.getPhone());
            item.put("status", user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用");
            item.put("lastActiveTime", formatTime(user.getLastActiveTime(), user.getUpdateTime()));
            return item;
        }).collect(Collectors.toList());
    }

    private Map<Long, String> loadUserNameMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UserInfo> users = userInfoMapper.selectBatchIds(userIds);
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }

        return users.stream().collect(Collectors.toMap(
                UserInfo::getId,
                user -> StringUtils.hasText(user.getUserName()) ? user.getUserName() : "未知用户",
                (a, b) -> a,
                HashMap::new
        ));
    }

    private <T> Map<LocalDate, Integer> aggregateByDate(List<T> source, Function<T, LocalDateTime> extractor) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<LocalDate, Integer> grouped = new HashMap<>();
        for (T item : source) {
            LocalDateTime time = extractor.apply(item);
            if (time == null) {
                continue;
            }

            LocalDate date = time.toLocalDate();
            grouped.put(date, grouped.getOrDefault(date, 0) + 1);
        }
        return grouped;
    }

    private String normalizeCity(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "未填写";
        }

        String city = raw.trim().replace('，', '/').replace(',', '/');
        if (city.contains("/")) {
            city = city.split("/")[0].trim();
        }

        if ("Shanghai".equalsIgnoreCase(city)) {
            city = "上海";
        } else if ("Beijing".equalsIgnoreCase(city)) {
            city = "北京";
        } else if ("Shenzhen".equalsIgnoreCase(city)) {
            city = "深圳";
        } else if ("Guangzhou".equalsIgnoreCase(city)) {
            city = "广州";
        } else if ("Hangzhou".equalsIgnoreCase(city)) {
            city = "杭州";
        }

        return StringUtils.hasText(city) ? city : "未填写";
    }

    private String formatTime(LocalDateTime primary, LocalDateTime fallback) {
        LocalDateTime value = primary != null ? primary : fallback;
        return value == null ? "--" : value.format(API_TIME);
    }

    private double calcPercent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return BigDecimal.valueOf((double) numerator * 100D / (double) denominator)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }
}
