package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * 简历基础信息表(Resume)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:45
 */
public class Resume implements Serializable {
    private static final long serialVersionUID = 515142667556919968L;
    /**
     * 简历ID（主键）
     */
    private Long id;
    /**
     * 关联user.id（所属用户）
     */
    private Long userId;
    /**
     * 简历标题（如“高级产品经理--B端方向”）
     */
    private String title;
    /**
     * 简历版本号
     */
    private String num;
    /**
     * 完成进度（0-100）
     */
    private Integer progress;
    /**
     * 行业分类（如“互联网”“金融”）
     */
    private String category;
    /**
     * 标签（逗号分隔，如“产品,中文”）
     */
    private String tags;
    /**
     * 简历语言（如“中文”“English”）
     */
    private String language;
    /**
     * 内容类型：1-富文本 2-Word文件 3-Excel文件
     */
    private Integer contentType;
    /**
     * 竞争维度名称（如“参数1”“参数2”）
     */
    private String indexName;
    /**
     * 维度评分（0-100）
     */
    private Integer score;
    /**
     * 竞争力比例（相对于其他简历的百分比，0.00-100.00）
     */
    private Double competeRatio;
    /**
     * 是否默认简历：1-是 0-否
     */
    private Integer isDefault;
    /**
     * 父简历ID（为空=原始上传；非空=由该简历派生的版本）
     */
    private Long parentResumeId;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 最近更新时间
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getContentType() {
        return contentType;
    }

    public void setContentType(Integer contentType) {
        this.contentType = contentType;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Double getCompeteRatio() {
        return competeRatio;
    }

    public void setCompeteRatio(Double competeRatio) {
        this.competeRatio = competeRatio;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public Long getParentResumeId() {
        return parentResumeId;
    }

    public void setParentResumeId(Long parentResumeId) {
        this.parentResumeId = parentResumeId;
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

