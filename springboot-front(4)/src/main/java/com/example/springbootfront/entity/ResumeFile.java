package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * 简历文件存储表(ResumeFile)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:48
 */
public class ResumeFile implements Serializable {
    private static final long serialVersionUID = -10161603459373530L;
    /**
     * 文件ID（主键）
     */
    private Long id;
    /**
     * 关联resume.id
     */
    private Long resumeId;
    /**
     * 原始文件名（如“我的简历.docx”）
     */
    private String fileName;
    /**
     * 文件类型（如application/msword、application/vnd.openxmlformats-officedocument.wordprocessingml.document）
     */
    private String fileType;
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    /**
     * 文件存储路径（如/minio/resume/2025/05/10/13800138000_123.docx）
     */
    private String storagePath;
    /**
     * 文件下载链接（可带签名，有效期）
     */
    private String downloadUrl;
    /**
     * 简历预览图URL（用于列表/详情页展示）
     */
    private String previewUrl;
    /**
     * 文件MD5值（防重复上传）
     */
    private String md5;
    /**
     * 上传时间
     */
    private Date uploadTime;
    /**
     * 更新时间
     */
    private Date updateTime;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
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

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

}

