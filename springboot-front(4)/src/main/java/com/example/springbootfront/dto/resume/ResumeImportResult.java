package com.example.springbootfront.dto.resume;

import java.util.Map;

public class ResumeImportResult {

    private Long resumeId;
    private Long resumeFileId;
    private String title;
    private String fileName;
    private Integer fragmentCount;
    private Map<String, Integer> sectionCounts;
    private String storagePath;
    private String downloadUrl;
    private String previewUrl;

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public Long getResumeFileId() {
        return resumeFileId;
    }

    public void setResumeFileId(Long resumeFileId) {
        this.resumeFileId = resumeFileId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getFragmentCount() {
        return fragmentCount;
    }

    public void setFragmentCount(Integer fragmentCount) {
        this.fragmentCount = fragmentCount;
    }

    public Map<String, Integer> getSectionCounts() {
        return sectionCounts;
    }

    public void setSectionCounts(Map<String, Integer> sectionCounts) {
        this.sectionCounts = sectionCounts;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }
}
