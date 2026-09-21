package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * 简历片段表(ResumeFragment)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:48
 */
public class ResumeFragment implements Serializable {
    private static final long serialVersionUID = 357794274395169695L;
    /**
     * 片段ID
     */
    private Long id;
    /**
     * 关联 user.id
     */
    private Long userId;
    /**
     * 关联 resume.id
     */
    private Long resumeId;
    /**
     * 片段类型
     */
    private String sectionKey;
    /**
     * 该片段下的条目序号
     */
    private Integer itemIndex;
    /**
     * 片段原文
     */
    private String fragmentText;
    /**
     * 片段内容Hash
     */
    private String fragmentHash;
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

    public String getSectionKey() {
        return sectionKey;
    }

    public void setSectionKey(String sectionKey) {
        this.sectionKey = sectionKey;
    }

    public Integer getItemIndex() {
        return itemIndex;
    }

    public void setItemIndex(Integer itemIndex) {
        this.itemIndex = itemIndex;
    }

    public String getFragmentText() {
        return fragmentText;
    }

    public void setFragmentText(String fragmentText) {
        this.fragmentText = fragmentText;
    }

    public String getFragmentHash() {
        return fragmentHash;
    }

    public void setFragmentHash(String fragmentHash) {
        this.fragmentHash = fragmentHash;
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

