package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * (UserHistoryCompany)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:51
 */
public class UserHistoryCompany implements Serializable {
    private static final long serialVersionUID = 375666420802437114L;

    private Long id;

    private Long userId;

    private Long companyId;

    private String companyContent;

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

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyContent() {
        return companyContent;
    }

    public void setCompanyContent(String companyContent) {
        this.companyContent = companyContent;
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

