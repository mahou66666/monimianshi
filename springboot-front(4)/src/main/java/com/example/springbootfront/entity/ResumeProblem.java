package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * 简历待优化问题表(ResumeProblem)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:49
 */
public class ResumeProblem implements Serializable {
    private static final long serialVersionUID = 565041202423795753L;
    /**
     * 问题ID（主键）
     */
    private Long id;
    /**
     * 关联resume.id
     */
    private Long resumeId;
    /**
     * 问题标题（如“缺乏项目经验”）
     */
    private String problemTitle;
    /**
     * 问题描述（如“岗位匹配度低”）
     */
    private String problemDesc;
    /**
     * 优先级
     */
    private String priority;
    /**
     * 建议
     */
    private String suggestion;
    /**
     * 问题状态：1-待解决 2-已解决
     */
    private Integer status;
    /**
     * 创建时间
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

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public String getProblemTitle() {
        return problemTitle;
    }

    public void setProblemTitle(String problemTitle) {
        this.problemTitle = problemTitle;
    }

    public String getProblemDesc() {
        return problemDesc;
    }

    public void setProblemDesc(String problemDesc) {
        this.problemDesc = problemDesc;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

