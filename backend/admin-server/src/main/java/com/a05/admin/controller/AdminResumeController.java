package com.a05.admin.controller;

import com.a05.admin.auth.RequirePermission;
import com.a05.admin.common.Result;
import com.a05.admin.controller.dto.Md5BatchRequest;
import com.a05.admin.controller.dto.ParseUploadedBatchRequest;
import com.a05.admin.controller.dto.ResumeContentBatchRequest;
import com.a05.admin.controller.dto.UpdateAdminResumeRequest;
import com.a05.admin.service.AdminResumeService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/interview/admin/users/{userId}/resumes")
@RequirePermission(anyOf = {"resume:view", "resume:manage"})
public class AdminResumeController {

    private final AdminResumeService adminResumeService;

    public AdminResumeController(AdminResumeService adminResumeService) {
        this.adminResumeService = adminResumeService;
    }

    @PostMapping("/import-pdf/precheck/md5/batch")
    @RequirePermission(anyOf = {"resume:manage"})
    public Result<?> precheckMd5Batch(@PathVariable Long userId, @RequestBody(required = false) Md5BatchRequest request) {
        List<String> md5List = request == null || request.getMd5List() == null
                ? new ArrayList<>()
                : request.getMd5List();
        return Result.success(adminResumeService.precheckMd5Batch(userId, md5List));
    }

    @PostMapping("/import-pdf-batch")
    @RequirePermission(anyOf = {"resume:manage"})
    public Result<?> importPdfBatch(@PathVariable Long userId, @RequestParam("files") List<MultipartFile> files) {
        return Result.success(adminResumeService.importPdfBatch(userId, files));
    }

    @PostMapping("/parse/uploaded/batch")
    @RequirePermission(anyOf = {"resume:manage"})
    public Result<?> parseUploadedBatch(@PathVariable Long userId, @RequestBody(required = false) ParseUploadedBatchRequest request) {
        List<Long> resumeFileIds = request == null || request.getResumeFileIds() == null
                ? new ArrayList<>()
                : request.getResumeFileIds();
        return Result.success(adminResumeService.parseUploadedBatch(userId, resumeFileIds));
    }

    @PostMapping("/parse/batch")
    @RequirePermission(anyOf = {"resume:manage"})
    public Result<?> parseResumeBatch(@PathVariable Long userId, @RequestParam("files") List<MultipartFile> files) {
        return Result.success(adminResumeService.parseResumeBatch(userId, files));
    }

    @PutMapping("/{resumeId}")
    @RequirePermission(anyOf = {"resume:manage"})
    public Result<?> updateResume(@PathVariable Long userId,
                                  @PathVariable Long resumeId,
                                  @RequestBody(required = false) UpdateAdminResumeRequest request) {
        return Result.success(adminResumeService.updateResume(userId, resumeId, request));
    }

    @DeleteMapping("/{resumeId}")
    @RequirePermission(anyOf = {"resume:manage"})
    public Result<?> deleteResume(@PathVariable Long userId, @PathVariable Long resumeId) {
        return Result.success(adminResumeService.deleteResume(userId, resumeId));
    }

    @GetMapping("/imported")
    public Result<?> importedResumes(@PathVariable Long userId,
                                     @RequestParam(required = false) Integer pageNo,
                                     @RequestParam(required = false) Integer pageSize,
                                     @RequestParam(required = false) String keyword) {
        return Result.success(adminResumeService.listImportedResumes(userId, pageNo, pageSize, keyword));
    }

    @GetMapping("/{resumeId}/content")
    public Result<?> resumeContent(@PathVariable Long userId, @PathVariable Long resumeId) {
        return Result.success(adminResumeService.getResumeContent(userId, resumeId));
    }

    @PostMapping("/content/batch")
    public Result<?> resumeContentBatch(@PathVariable Long userId, @RequestBody(required = false) ResumeContentBatchRequest request) {
        List<Long> resumeIds = request == null || request.getResumeIds() == null
                ? new ArrayList<>()
                : request.getResumeIds();
        return Result.success(adminResumeService.getResumeContentBatch(userId, resumeIds));
    }

    @GetMapping("/score/list")
    public Result<?> resumeScoreList(@PathVariable Long userId,
                                     @RequestParam(required = false) Integer pageNo,
                                     @RequestParam(required = false) Integer pageSize,
                                     @RequestParam(required = false) String keyword) {
        return Result.success(adminResumeService.listResumeScores(userId, pageNo, pageSize, keyword));
    }

    @GetMapping("/{resumeId}/score/latest")
    public Result<?> latestResumeScore(@PathVariable Long userId, @PathVariable Long resumeId) {
        return Result.success(adminResumeService.getLatestResumeScore(userId, resumeId));
    }

    @GetMapping("/files/{resumeFileId}/file")
    public ResponseEntity<Resource> downloadResumeFile(@PathVariable Long userId, @PathVariable Long resumeFileId) {
        return adminResumeService.downloadResumeFile(userId, resumeFileId);
    }
}
