package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * JD指导建议表(JdGuide)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:43
 */
public class JdGuide implements Serializable {
    private static final long serialVersionUID = -86896678658820034L;
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 关联 jd_job.id
     */
    private Long jdId;
    /**
     * JD解析与通用准备建议（无历史版本）
     */
    private String guideText;
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

    public Long getJdId() {
        return jdId;
    }

    public void setJdId(Long jdId) {
        this.jdId = jdId;
    }

    public String getGuideText() {
        return guideText;
    }

    public void setGuideText(String guideText) {
        this.guideText = guideText;
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

