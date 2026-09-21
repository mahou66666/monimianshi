package com.a05.admin.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.a05.admin.controller.dto.UpdateAdminResumeRequest;

import java.util.List;
import java.util.Map;

public interface AdminResumeService {

    Map<String, Object> precheckMd5Batch(Long userId, List<String> md5List);

    Map<String, Object> importPdfBatch(Long userId, List<MultipartFile> files);

    Map<String, Object> listImportedResumes(Long userId, Integer pageNo, Integer pageSize, String keyword);

    Map<String, Object> getResumeContent(Long userId, Long resumeId);

    Map<String, Object> getResumeContentBatch(Long userId, List<Long> resumeIds);

    Map<String, Object> listResumeScores(Long userId, Integer pageNo, Integer pageSize, String keyword);

    Map<String, Object> getLatestResumeScore(Long userId, Long resumeId);

    Map<String, Object> parseUploadedBatch(Long userId, List<Long> resumeFileIds);

    Map<String, Object> parseResumeBatch(Long userId, List<MultipartFile> files);

    Map<String, Object> updateResume(Long userId, Long resumeId, UpdateAdminResumeRequest request);

    Map<String, Object> deleteResume(Long userId, Long resumeId);

    ResponseEntity<Resource> downloadResumeFile(Long userId, Long resumeFileId);
}
