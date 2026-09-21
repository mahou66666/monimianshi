package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * (UserHistoryJd)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:52
 */
public class UserHistoryJd implements Serializable {
    private static final long serialVersionUID = -30700685743944666L;

    private Long id;

    private Long userId;

    private Long jdId;

    private String jdContent;

    private Integer isDeleted;

    private Date createTime;


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

    public Long getJdId() {
        return jdId;
    }

    public void setJdId(Long jdId) {
        this.jdId = jdId;
    }

    public String getJdContent() {
        return jdContent;
    }

    public void setJdContent(String jdContent) {
        this.jdContent = jdContent;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

}

