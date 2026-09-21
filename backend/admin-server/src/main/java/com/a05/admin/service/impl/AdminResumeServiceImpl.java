package com.a05.admin.service.impl;

import com.a05.admin.controller.dto.UpdateAdminResumeRequest;
import com.a05.admin.entity.Resume;
import com.a05.admin.entity.ResumeContent;
import com.a05.admin.entity.ResumeFile;
import com.a05.admin.entity.ResumeScore;
import com.a05.admin.entity.UserInfo;
import com.a05.admin.mapper.ResumeContentMapper;
import com.a05.admin.mapper.ResumeFileMapper;
import com.a05.admin.mapper.ResumeMapper;
import com.a05.admin.mapper.ResumeScoreMapper;
import com.a05.admin.mapper.UserInfoMapper;
import com.a05.admin.service.AdminResumeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminResumeServiceImpl implements AdminResumeService {

    private final ResumeMapper resumeMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ResumeContentMapper resumeContentMapper;
    private final ResumeScoreMapper resumeScoreMapper;
    private final UserInfoMapper userInfoMapper;
    private final ObjectMapper objectMapper;

    @Value("${app.storage.resume-dir:storage/resumes}")
    private String resumeStorageDir;

    public AdminResumeServiceImpl(ResumeMapper resumeMapper,
                                  ResumeFileMapper resumeFileMapper,
                                  ResumeContentMapper resumeContentMapper,
                                  ResumeScoreMapper resumeScoreMapper,
                                  UserInfoMapper userInfoMapper,
                                  ObjectMapper objectMapper) {
        this.resumeMapper = resumeMapper;
        this.resumeFileMapper = resumeFileMapper;
        this.resumeContentMapper = resumeContentMapper;
        this.resumeScoreMapper = resumeScoreMapper;
        this.userInfoMapper = userInfoMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> precheckMd5Batch(Long userId, List<String> md5List) {
        requireUser(userId);

        List<Map<String, Object>> items = new ArrayList<>();
        if (md5List == null) {
            Map<String, Object> data = new HashMap<>();
            data.put("items", items);
            return data;
        }

        for (String md5Raw : md5List) {
            String md5 = md5Raw == null ? "" : md5Raw.trim().toLowerCase();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("md5", md5Raw);

            if (!StringUtils.hasText(md5)) {
                item.put("exists", false);
                items.add(item);
                continue;
            }

            ResumeFile existing = resumeFileMapper.selectByMd5(md5);
            if (existing == null) {
                item.put("exists", false);
                items.add(item);
                continue;
            }

            Resume resume = resumeMapper.selectById(existing.getResumeId());
            Long ownerUserId = resume == null ? userId : resume.getUserId();
            item.put("exists", true);
            item.put("resumeId", existing.getResumeId());
            item.put("resumeFileId", existing.getId());
            item.put("storagePath", existing.getStoragePath());
            item.put("downloadUrl", buildDownloadUrl(ownerUserId, existing.getId()));
            items.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return data;
    }

    @Override
    public Map<String, Object> importPdfBatch(Long userId, List<MultipartFile> files) {
        requireUser(userId);
        List<Map<String, Object>> items = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("items", items);
            return data;
        }

        for (MultipartFile file : files) {
            Map<String, Object> item = new LinkedHashMap<>();
            String originalName = file == null ? null : file.getOriginalFilename();
            item.put("fileName", originalName);

            if (file == null || file.isEmpty()) {
                item.put("status", "fail");
                item.put("errorMessage", "空文件，无法上传");
                items.add(item);
                continue;
            }

            String lowerName = originalName == null ? "" : originalName.toLowerCase();
            if (!lowerName.endsWith(".pdf")) {
                item.put("status", "fail");
                item.put("errorMessage", "仅支持 PDF 文件");
                items.add(item);
                continue;
            }

            String md5;
            try {
                md5 = md5Hex(file);
            } catch (Exception ex) {
                item.put("status", "fail");
                item.put("errorMessage", "计算文件 MD5 失败");
                items.add(item);
                continue;
            }
            item.put("md5", md5);

            ResumeFile existing = resumeFileMapper.selectByMd5(md5);
            if (existing != null) {
                Resume resume = resumeMapper.selectById(existing.getResumeId());
                Long ownerUserId = resume == null ? userId : resume.getUserId();
                item.put("status", "duplicate");
                item.put("resumeId", existing.getResumeId());
                item.put("resumeFileId", existing.getId());
                item.put("storagePath", existing.getStoragePath());
                item.put("downloadUrl", buildDownloadUrl(ownerUserId, existing.getId()));
                items.add(item);
                continue;
            }

            try {
                String relativePath = buildRelativeStoragePath(userId, originalName);
                Path absolutePath = resolveStoragePath(relativePath);
                Files.createDirectories(absolutePath.getParent());
                file.transferTo(absolutePath.toFile());

                Resume resume = new Resume();
                resume.setUserId(userId);
                resume.setTitle(stripExtension(originalName));
                resume.setNum("V1.0");
                resume.setProgress(100);
                resume.setCategory("未分类");
                resume.setTags("");
                resume.setLanguage("中文");
                resume.setContentType(2);
                resume.setScore(0);
                resume.setCompeteRatio(BigDecimal.ZERO);
                resume.setIsDefault(1);
                resumeMapper.insert(resume);

                ResumeFile resumeFile = new ResumeFile();
                resumeFile.setResumeId(resume.getId());
                resumeFile.setFileName(originalName);
                resumeFile.setFileType(file.getContentType() == null ? "application/pdf" : file.getContentType());
                resumeFile.setFileSize(file.getSize());
                resumeFile.setStoragePath(relativePath);
                resumeFile.setDownloadUrl(null);
                resumeFile.setMd5(md5);
                resumeFileMapper.insert(resumeFile);
                String downloadUrl = buildDownloadUrl(userId, resumeFile.getId());
                resumeFile.setDownloadUrl(downloadUrl);
                resumeFileMapper.updateById(resumeFile);

                // Insert basic content so the admin page can show a useful summary before parsing.
                ResumeContent content = new ResumeContent();
                content.setResumeId(resume.getId());
                content.setContent(defaultResumeHtml(originalName));
                resumeContentMapper.insert(content);

                item.put("status", "success");
                item.put("resumeId", resume.getId());
                item.put("resumeFileId", resumeFile.getId());
                item.put("storagePath", relativePath);
                item.put("downloadUrl", downloadUrl);
            } catch (DuplicateKeyException duplicateKeyException) {
                ResumeFile raceExisting = resumeFileMapper.selectByMd5(md5);
                if (raceExisting != null) {
                    Resume resume = resumeMapper.selectById(raceExisting.getResumeId());
                    Long ownerUserId = resume == null ? userId : resume.getUserId();
                    item.put("status", "duplicate");
                    item.put("resumeId", raceExisting.getResumeId());
                    item.put("resumeFileId", raceExisting.getId());
                    item.put("storagePath", raceExisting.getStoragePath());
                    item.put("downloadUrl", buildDownloadUrl(ownerUserId, raceExisting.getId()));
                } else {
                    item.put("status", "fail");
                    item.put("errorMessage", "上传冲突，请重试");
                }
            } catch (Exception ex) {
                item.put("status", "fail");
                item.put("errorMessage", ex.getMessage() == null ? "上传失败" : ex.getMessage());
            }

            items.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return data;
    }

    @Override
    public Map<String, Object> listImportedResumes(Long userId, Integer pageNo, Integer pageSize, String keyword) {
        requireUser(userId);
        int pNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int pSize = pageSize == null || pageSize < 1 ? 100 : Math.min(pageSize, 200);

        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Resume::getTitle, keyword.trim());
        }

        IPage<Resume> page = resumeMapper.selectPage(new Page<>(pNo, pSize), wrapper);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Resume resume : page.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("resumeId", resume.getId());
            row.put("title", resume.getTitle());

            ResumeFile resumeFile = resumeFileMapper.selectFirstByResumeId(resume.getId());
            List<Map<String, Object>> files = new ArrayList<>();
            boolean hasLocalFile = false;
            String fileCheckMessage = "缺少文件记录";
            if (resumeFile != null) {
                hasLocalFile = hasLocalResumeFile(resumeFile);
                fileCheckMessage = hasLocalFile ? "本地文件可用，可直接提取" : "本地文件不存在，仅可查看记录";
                Map<String, Object> file = new LinkedHashMap<>();
                file.put("id", resumeFile.getId());
                file.put("fileName", resumeFile.getFileName());
                file.put("fileType", resumeFile.getFileType());
                file.put("fileSize", resumeFile.getFileSize());
                file.put("storagePath", resumeFile.getStoragePath());
                file.put("downloadUrl", buildDownloadUrl(userId, resumeFile.getId()));
                file.put("hasLocalFile", hasLocalFile);
                file.put("fileCheckMessage", fileCheckMessage);
                files.add(file);
            }
            row.put("files", files);
            row.put("hasLocalFile", hasLocalFile);
            row.put("canParse", resumeFile != null && hasLocalFile);
            row.put("fileCheckMessage", fileCheckMessage);
            items.add(row);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("pageNo", pNo);
        data.put("pageSize", pSize);
        data.put("total", page.getTotal());
        data.put("items", items);
        return data;
    }

    @Override
    public Map<String, Object> getResumeContent(Long userId, Long resumeId) {
        Resume resume = requireResume(userId, resumeId);
        ResumeContent content = resumeContentMapper.selectByResumeId(resume.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("resumeId", resume.getId());
        data.put("ok", true);
        data.put("content", content == null ? "" : content.getContent());
        return data;
    }

    @Override
    public Map<String, Object> getResumeContentBatch(Long userId, List<Long> resumeIds) {
        requireUser(userId);
        List<Map<String, Object>> items = new ArrayList<>();
        if (resumeIds == null) {
            Map<String, Object> data = new HashMap<>();
            data.put("items", items);
            return data;
        }

        for (Long resumeId : resumeIds) {
            Map<String, Object> item = new HashMap<>();
            item.put("resumeId", resumeId);
            Resume resume = resumeMapper.selectByUserIdAndResumeId(userId, resumeId);
            if (resume == null) {
                item.put("ok", false);
                item.put("message", "简历不存在");
                items.add(item);
                continue;
            }
            ResumeContent content = resumeContentMapper.selectByResumeId(resumeId);
            item.put("ok", true);
            item.put("content", content == null ? "" : content.getContent());
            items.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return data;
    }

    @Override
    public Map<String, Object> listResumeScores(Long userId, Integer pageNo, Integer pageSize, String keyword) {
        requireUser(userId);
        int pNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int pSize = pageSize == null || pageSize < 1 ? 100 : Math.min(pageSize, 200);

        List<ResumeScore> allScores = resumeScoreMapper.selectList(new LambdaQueryWrapper<ResumeScore>()
                .eq(ResumeScore::getUserId, userId)
                .orderByDesc(ResumeScore::getCreateTime)
                .orderByDesc(ResumeScore::getId));

        Map<String, Object> data = new HashMap<>();
        if (allScores == null || allScores.isEmpty()) {
            data.put("pageNo", pNo);
            data.put("pageSize", pSize);
            data.put("total", 0);
            data.put("items", new ArrayList<>());
            return data;
        }

        List<Long> scoredResumeIds = allScores.stream()
                .map(ResumeScore::getResumeId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (scoredResumeIds.isEmpty()) {
            data.put("pageNo", pNo);
            data.put("pageSize", pSize);
            data.put("total", 0);
            data.put("items", new ArrayList<>());
            return data;
        }

        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .in(Resume::getId, scoredResumeIds)
                .orderByDesc(Resume::getUpdateTime)
                .orderByDesc(Resume::getId);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            Long keywordId = parseLongSafely(kw);
            wrapper.and(w -> {
                if (keywordId != null) {
                    w.like(Resume::getTitle, kw).or().eq(Resume::getId, keywordId);
                } else {
                    w.like(Resume::getTitle, kw);
                }
            });
        }

        IPage<Resume> page = resumeMapper.selectPage(new Page<>(pNo, pSize), wrapper);
        List<Resume> resumes = page.getRecords();
        List<Long> pageResumeIds = resumes.stream().map(Resume::getId).collect(Collectors.toList());

        Map<Long, ResumeScore> latestMap = new HashMap<>();
        Map<Long, Integer> countMap = new HashMap<>();
        if (!pageResumeIds.isEmpty()) {
            List<ResumeScore> scoreRows = resumeScoreMapper.selectList(new LambdaQueryWrapper<ResumeScore>()
                    .eq(ResumeScore::getUserId, userId)
                    .in(ResumeScore::getResumeId, pageResumeIds)
                    .orderByDesc(ResumeScore::getCreateTime)
                    .orderByDesc(ResumeScore::getId));

            for (ResumeScore score : scoreRows) {
                if (score == null || score.getResumeId() == null) {
                    continue;
                }
                Long resumeId = score.getResumeId();
                countMap.put(resumeId, countMap.getOrDefault(resumeId, 0) + 1);
                if (!latestMap.containsKey(resumeId)) {
                    latestMap.put(resumeId, score);
                }
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Resume resume : resumes) {
            ResumeScore latest = latestMap.get(resume.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("resumeId", resume.getId());
            row.put("resumeName", StringUtils.hasText(resume.getTitle()) ? resume.getTitle() : "未命名简历");
            row.put("generateCount", countMap.getOrDefault(resume.getId(), 0));
            row.put("totalScore", latest == null ? 0 : safeInt(latest.getTotalScore()));
            row.put("scores", buildScoreMap(latest));
            row.put("latestScoreId", latest == null ? null : latest.getId());
            row.put("latestScoreTime", latest == null || latest.getCreateTime() == null ? null : latest.getCreateTime().toString());
            items.add(row);
        }

        data.put("pageNo", pNo);
        data.put("pageSize", pSize);
        data.put("total", page.getTotal());
        data.put("items", items);
        return data;
    }

    @Override
    public Map<String, Object> getLatestResumeScore(Long userId, Long resumeId) {
        requireResume(userId, resumeId);
        ResumeScore score = resumeScoreMapper.selectLatestByUserIdAndResumeId(userId, resumeId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resumeId", resumeId);
        data.put("hasScore", score != null);
        data.put("scoreId", score == null ? null : score.getId());
        data.put("totalScore", score == null ? 0 : safeInt(score.getTotalScore()));
        data.put("scores", buildScoreMap(score));
        data.put("dimensionAnalysis", score == null ? "" : nullToEmpty(score.getDimensionAnalysis()));
        data.put("advice", score == null ? "" : nullToEmpty(score.getAdvice()));
        data.put("problems", score == null ? new ArrayList<>() : parseProblemsJson(score.getProblemsJson()));
        data.put("createTime", score == null || score.getCreateTime() == null ? null : score.getCreateTime().toString());
        return data;
    }

    @Override
    public Map<String, Object> parseUploadedBatch(Long userId, List<Long> resumeFileIds) {
        requireUser(userId);
        int processedCount = 0;

        if (resumeFileIds != null) {
            for (Long resumeFileId : resumeFileIds) {
                ResumeFile resumeFile = resumeFileMapper.selectById(resumeFileId);
                if (resumeFile == null) {
                    continue;
                }
                Resume resume = resumeMapper.selectById(resumeFile.getResumeId());
                if (resume == null || !userId.equals(resume.getUserId())) {
                    continue;
                }
                ResumeContent content = resumeContentMapper.selectByResumeId(resume.getId());
                if (content == null) {
                    content = new ResumeContent();
                    content.setResumeId(resume.getId());
                    content.setContent(defaultResumeHtml(resumeFile.getFileName()));
                    resumeContentMapper.insert(content);
                }
                processedCount++;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("processedCount", processedCount);
        return data;
    }

    @Override
    public Map<String, Object> parseResumeBatch(Long userId, List<MultipartFile> files) {
        Map<String, Object> importResult = importPdfBatch(userId, files);
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("importResult", importResult);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateResume(Long userId, Long resumeId, UpdateAdminResumeRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body must not be null");
        }

        Resume resume = requireResume(userId, resumeId);
        boolean changed = false;

        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (!StringUtils.hasText(title)) {
                throw new RuntimeException("Resume title must not be blank");
            }
            resume.setTitle(title);
            changed = true;
        }

        if (request.getContent() != null) {
            ResumeContent content = resumeContentMapper.selectByResumeId(resumeId);
            if (content == null) {
                content = new ResumeContent();
                content.setResumeId(resumeId);
                content.setContent(request.getContent());
                resumeContentMapper.insert(content);
            } else {
                content.setContent(request.getContent());
                resumeContentMapper.updateById(content);
            }
            changed = true;
        }

        if (!changed) {
            throw new RuntimeException("At least one field should be updated");
        }

        resumeMapper.updateById(resume);
        return buildResumeDetail(userId, resumeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteResume(Long userId, Long resumeId) {
        Resume resume = requireResume(userId, resumeId);

        LambdaQueryWrapper<ResumeFile> fileWrapper = new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getResumeId, resumeId);
        List<ResumeFile> resumeFiles = resumeFileMapper.selectList(fileWrapper);

        List<String> deletedStoragePaths = new ArrayList<>();
        for (ResumeFile resumeFile : resumeFiles) {
            if (!StringUtils.hasText(resumeFile.getStoragePath())) {
                continue;
            }
            Path path = resolveStoragePath(resumeFile.getStoragePath());
            try {
                Files.deleteIfExists(path);
                deletedStoragePaths.add(resumeFile.getStoragePath());
            } catch (IOException ex) {
                throw new RuntimeException("Failed to delete local resume file: " + ex.getMessage());
            }
        }

        ResumeContent resumeContent = resumeContentMapper.selectByResumeId(resumeId);
        if (resumeContent != null) {
            resumeContentMapper.deleteById(resumeContent.getId());
        }
        if (!resumeFiles.isEmpty()) {
            resumeFileMapper.delete(fileWrapper);
        }
        resumeMapper.deleteById(resume.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("resumeId", resumeId);
        data.put("deletedStoragePaths", deletedStoragePaths);
        return data;
    }

    @Override
    public ResponseEntity<Resource> downloadResumeFile(Long userId, Long resumeFileId) {
        requireUser(userId);
        ResumeFile resumeFile = resumeFileMapper.selectById(resumeFileId);
        if (resumeFile == null) {
            throw new RuntimeException("文件不存在");
        }
        Resume resume = resumeMapper.selectById(resumeFile.getResumeId());
        if (resume == null || !userId.equals(resume.getUserId())) {
            throw new RuntimeException("无权限访问该文件");
        }

        Path path = resolveStoragePath(resumeFile.getStoragePath());
        if (!Files.exists(path)) {
            throw new RuntimeException("文件不存在或已删除");
        }

        Resource resource;
        try {
            resource = new UrlResource(path.toUri());
        } catch (Exception e) {
            throw new RuntimeException("文件读取失败");
        }

        String filename = StringUtils.hasText(resumeFile.getFileName()) ? resumeFile.getFileName() : "resume.pdf";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (StringUtils.hasText(resumeFile.getFileType())) {
            try {
                mediaType = MediaType.parseMediaType(resumeFile.getFileType());
            } catch (Exception ignore) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        long contentLength = 0L;
        try {
            contentLength = Files.size(path);
        } catch (IOException ignore) {
            contentLength = 0L;
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
        if (contentLength > 0) {
            builder.contentLength(contentLength);
        }
        return builder.body(resource);
    }

    private UserInfo requireUser(Long userId) {
        if (userId == null) {
            throw new RuntimeException("userId 不能为空");
        }
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private Resume requireResume(Long userId, Long resumeId) {
        if (resumeId == null) {
            throw new RuntimeException("resumeId 不能为空");
        }
        Resume resume = resumeMapper.selectByUserIdAndResumeId(userId, resumeId);
        if (resume == null) {
            throw new RuntimeException("简历不存在");
        }
        return resume;
    }

    private Map<String, Object> buildScoreMap(ResumeScore score) {
        Map<String, Object> scores = new LinkedHashMap<>();
        scores.put("education", score == null ? 0 : safeInt(score.getEducationScore()));
        scores.put("educationScore", score == null ? 0 : safeInt(score.getEducationScore()));
        scores.put("work", score == null ? 0 : safeInt(score.getWorkScore()));
        scores.put("workScore", score == null ? 0 : safeInt(score.getWorkScore()));
        scores.put("project", score == null ? 0 : safeInt(score.getProjectScore()));
        scores.put("projectScore", score == null ? 0 : safeInt(score.getProjectScore()));
        scores.put("skill", score == null ? 0 : safeInt(score.getSkillScore()));
        scores.put("skillScore", score == null ? 0 : safeInt(score.getSkillScore()));
        scores.put("award", score == null ? 0 : safeInt(score.getAwardScore()));
        scores.put("awardScore", score == null ? 0 : safeInt(score.getAwardScore()));
        scores.put("jobFit", score == null ? 0 : safeInt(score.getJobFitScore()));
        scores.put("jobFitScore", score == null ? 0 : safeInt(score.getJobFitScore()));
        return scores;
    }

    private List<String> parseProblemsJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new ArrayList<>();
        }
        String text = raw.trim();
        if ("null".equalsIgnoreCase(text)) {
            return new ArrayList<>();
        }
        try {
            List<String> items = objectMapper.readValue(text, new TypeReference<List<String>>() {});
            return items == null ? new ArrayList<>() : items.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            List<String> fallback = new ArrayList<>();
            fallback.add(text);
            return fallback;
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Long parseLongSafely(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildRelativeStoragePath(Long userId, String originalFilename) {
        String ext = ".pdf";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            int idx = originalFilename.lastIndexOf('.');
            ext = originalFilename.substring(idx);
        }
        LocalDate now = LocalDate.now();
        String safeName = userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        return now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/" + String.format("%02d", now.getDayOfMonth()) + "/" + safeName;
    }

    private Path resolveStoragePath(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            throw new RuntimeException("文件路径为空");
        }
        String normalized = storagePath.replace("\\", "/");
        Path path = Paths.get(normalized);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        Path base = resolveResumeStorageBase();
        return base.resolve(normalized).normalize();
    }

    private Path resolveResumeStorageBase() {
        String configuredDir = StringUtils.hasText(resumeStorageDir) ? resumeStorageDir : "storage/resumes";
        Path configuredPath = Paths.get(configuredDir.replace("\\", "/"));
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        Path currentDir = Paths.get("").toAbsolutePath().normalize();
        Path currentDirName = currentDir.getFileName();
        if (currentDirName != null && "admin-server".equalsIgnoreCase(currentDirName.toString())) {
            return currentDir.resolve(configuredPath).normalize();
        }

        Path moduleDir = currentDir.resolve("backend").resolve("admin-server").normalize();
        if (Files.exists(moduleDir) && Files.isDirectory(moduleDir)) {
            return moduleDir.resolve(configuredPath).normalize();
        }

        return currentDir.resolve(configuredPath).normalize();
    }

    private String buildDownloadUrl(Long userId, Long resumeFileId) {
        return "/interview/admin/users/" + userId + "/resumes/files/" + resumeFileId + "/file";
    }

    private boolean hasLocalResumeFile(ResumeFile resumeFile) {
        if (resumeFile == null || !StringUtils.hasText(resumeFile.getStoragePath())) {
            return false;
        }
        try {
            Path path = resolveStoragePath(resumeFile.getStoragePath());
            return Files.exists(path) && Files.isRegularFile(path);
        } catch (Exception ex) {
            return false;
        }
    }

    private Map<String, Object> buildResumeDetail(Long userId, Long resumeId) {
        Resume resume = requireResume(userId, resumeId);
        ResumeFile resumeFile = resumeFileMapper.selectFirstByResumeId(resumeId);
        ResumeContent resumeContent = resumeContentMapper.selectByResumeId(resumeId);

        Map<String, Object> data = new HashMap<>();
        data.put("resumeId", resume.getId());
        data.put("title", resume.getTitle());
        data.put("content", resumeContent == null ? "" : resumeContent.getContent());

        if (resumeFile != null) {
            boolean hasLocalFile = hasLocalResumeFile(resumeFile);
            data.put("resumeFileId", resumeFile.getId());
            data.put("fileName", resumeFile.getFileName());
            data.put("storagePath", resumeFile.getStoragePath());
            data.put("downloadUrl", buildDownloadUrl(userId, resumeFile.getId()));
            data.put("hasLocalFile", hasLocalFile);
            data.put("canParse", hasLocalFile);
            data.put("fileCheckMessage", hasLocalFile ? "本地文件可用，可直接提取" : "本地文件不存在，仅可查看记录");
        } else {
            data.put("resumeFileId", null);
            data.put("fileName", null);
            data.put("storagePath", null);
            data.put("downloadUrl", null);
            data.put("hasLocalFile", false);
            data.put("canParse", false);
            data.put("fileCheckMessage", "缺少文件记录");
        }
        return data;
    }

    private String md5Hex(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        int len;
        try (InputStream in = file.getInputStream()) {
            while ((len = in.read(buffer)) > 0) {
                digest.update(buffer, 0, len);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String stripExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "未命名简历";
        }
        int index = fileName.lastIndexOf('.');
        if (index <= 0) {
            return fileName;
        }
        return fileName.substring(0, index);
    }

    private String defaultResumeHtml(String fileName) {
        String title = stripExtension(fileName);
        return "<h1>" + title + "</h1>"
                + "<h2>鏁欒偛缁忓巻</h2><ul><li>寰呰ˉ鍏?/li></ul>"
                + "<h2>瀹炰範缁忓巻</h2><ul><li>寰呰ˉ鍏?/li></ul>"
                + "<h2>椤圭洰缁忓巻</h2><ul><li>寰呰ˉ鍏?/li></ul>";
    }
}

