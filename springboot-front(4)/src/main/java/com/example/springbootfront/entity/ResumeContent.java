package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * 简历内容详情表(ResumeContent)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:47
 */
public class ResumeContent implements Serializable {
    private static final long serialVersionUID = -35516822364424351L;
    /**
     * 内容ID（主键）
     */
    private Long id;
    /**
     * 关联resume.id
     */
    private Long resumeId;
    /**
     * 简历内容（富文本/HTML）
     */
    private String content;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

