package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * 简历评分表(ResumeScore)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:49
 */
public class ResumeScore implements Serializable {
    private static final long serialVersionUID = 803397105978952431L;
    /**
     * 评分记录ID
     */
    private Long id;
    /**
     * 关联 resume.id
     */
    private Long resumeId;
    /**
     * 关联 user.id
     */
    private Long userId;
    /**
     * 教育经历评分(0-100)
     */
    private Integer educationScore;
    /**
     * 工作/实习经历评分(0-100)
     */
    private Integer workScore;
    /**
     * 项目经历评分(0-100)
     */
    private Integer projectScore;
    /**
     * 技能评分(0-100)
     */
    private Integer skillScore;
    /**
     * 获奖与证书评分(0-100)
     */
    private Integer awardScore;
    /**
     * 岗位匹配度评分(0-100)
     */
    private Integer jobFitScore;
    /**
     * 总分(0-100)
     */
    private Integer totalScore;
    /**
     * 打分理由/维度分析
     */
    private String dimensionAnalysis;
    /**
     * 修改建议
     */
    private String advice;
    /**
     * 问题列表(JSON数组，元素为字符串)
     */
    private Object problemsJson;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getEducationScore() {
        return educationScore;
    }

    public void setEducationScore(Integer educationScore) {
        this.educationScore = educationScore;
    }

    public Integer getWorkScore() {
        return workScore;
    }

    public void setWorkScore(Integer workScore) {
        this.workScore = workScore;
    }

    public Integer getProjectScore() {
        return projectScore;
    }

    public void setProjectScore(Integer projectScore) {
        this.projectScore = projectScore;
    }

    public Integer getSkillScore() {
        return skillScore;
    }

    public void setSkillScore(Integer skillScore) {
        this.skillScore = skillScore;
    }

    public Integer getAwardScore() {
        return awardScore;
    }

    public void setAwardScore(Integer awardScore) {
        this.awardScore = awardScore;
    }

    public Integer getJobFitScore() {
        return jobFitScore;
    }

    public void setJobFitScore(Integer jobFitScore) {
        this.jobFitScore = jobFitScore;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public String getDimensionAnalysis() {
        return dimensionAnalysis;
    }

    public void setDimensionAnalysis(String dimensionAnalysis) {
        this.dimensionAnalysis = dimensionAnalysis;
    }

    public String getAdvice() {
        return advice;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }

    public Object getProblemsJson() {
        return problemsJson;
    }

    public void setProblemsJson(Object problemsJson) {
        this.problemsJson = problemsJson;
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

