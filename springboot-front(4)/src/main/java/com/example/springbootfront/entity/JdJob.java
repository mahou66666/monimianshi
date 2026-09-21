package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * 简历JD工作岗位表(JdJob)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:44
 */
public class JdJob implements Serializable {
    private static final long serialVersionUID = 599810132623667976L;
    /**
     * JD岗位ID（主键）
     */
    private Long id;
    /**
     * 岗位编码（唯一标识，如ALI-Java-001）
     */
    private String jobCode;
    /**
     * 岗位名称（如Java开发工程师/产品经理）
     */
    private String jobName;
    /**
     * 关联company.id（所属公司）
     */
    private Long companyId;
    /**
     * jd岗位描述
     */
    private String jdDescriptions;
    /**
     * jd岗位核心要求
     */
    private String jdCorereq;
    /**
     * 岗位类型：1-校招 2-社招 3-实习
     */
    private Integer jobType;
    /**
     * 薪资范围（如15k-25k/月）
     */
    private String salaryRange;
    /**
     * 工作城市（如北京/上海-浦东新区）
     */
    private String city;
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

    public String getJobCode() {
        return jobCode;
    }

    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getJdDescriptions() {
        return jdDescriptions;
    }

    public void setJdDescriptions(String jdDescriptions) {
        this.jdDescriptions = jdDescriptions;
    }

    public String getJdCorereq() {
        return jdCorereq;
    }

    public void setJdCorereq(String jdCorereq) {
        this.jdCorereq = jdCorereq;
    }

    public Integer getJobType() {
        return jobType;
    }

    public void setJobType(Integer jobType) {
        this.jobType = jobType;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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

