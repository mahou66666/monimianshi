package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * AI简历操作记录表(ResumeAiOperation)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:46
 */
public class ResumeAiOperation implements Serializable {
    private static final long serialVersionUID = 472896960164616297L;
    /**
     * 操作ID（主键）
     */
    private Long id;
    /**
     * 关联user.id
     */
    private Long userId;
    /**
     * 关联resume.id（若为新生成则为NULL）
     */
    private Long resumeId;
    /**
     * 关联ai_base_info.id
     */
    private Long aiBaseId;
    /**
     * 关联简历分段id
     */
    private Long aiResumeId;
    /**
     * 操作类型：1-AI生成简历 2-AI修改简历 3-AI分析问题
     */
    private Integer operationType;
    /**
     * AI输入的提示词（如“生成产品经理校招简历”）
     */
    private String prompt;
    /**
     * 操作结果（如“生成成功”“修改完成”）
     */
    private String result;
    /**
     * 操作时间
     */
    private Date createTime;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public Long getAiBaseId() {
        return aiBaseId;
    }

    public void setAiBaseId(Long aiBaseId) {
        this.aiBaseId = aiBaseId;
    }

    public Long getAiResumeId() {
        return aiResumeId;
    }

    public void setAiResumeId(Long aiResumeId) {
        this.aiResumeId = aiResumeId;
    }

    public Integer getOperationType() {
        return operationType;
    }

    public void setOperationType(Integer operationType) {
        this.operationType = operationType;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

}

