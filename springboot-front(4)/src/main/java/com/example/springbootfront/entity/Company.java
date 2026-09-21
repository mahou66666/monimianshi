package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * 简历投递公司表(Company)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:26
 */
public class Company implements Serializable {
    private static final long serialVersionUID = 870135296217444511L;
    /**
     * 公司ID（主键）
     */
    private Long id;
    /**
     * 公司编码（唯一标识，如ALI/ByteDance）
     */
    private String companyCode;
    /**
     * 公司名称（如阿里巴巴（中国）有限公司）
     */
    private String companyName;
    /**
     * 公司地点/工作地点
     */
    private String companyLocation;
    /**
     * 所属行业（如互联网/金融/教育）
     */
    private String industry;
    /**
     * 公司规模（如500-1000人/10000人以上）
     */
    private String scale;
    /**
     * 状态：1-有效 2-无效
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

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyLocation() {
        return companyLocation;
    }

    public void setCompanyLocation(String companyLocation) {
        this.companyLocation = companyLocation;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getScale() {
        return scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
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

