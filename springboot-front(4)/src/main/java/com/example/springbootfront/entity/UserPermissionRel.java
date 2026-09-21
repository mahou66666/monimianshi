package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * (UserPermissionRel)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:54
 */
public class UserPermissionRel implements Serializable {
    private static final long serialVersionUID = -95842297883561228L;

    private Long id;

    private Long userId;

    private Long permId;

    private Date createTime;

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

    public Long getPermId() {
        return permId;
    }

    public void setPermId(Long permId) {
        this.permId = permId;
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

