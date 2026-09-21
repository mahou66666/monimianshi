package com.example.springbootfront.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * 简历投递关系表（含投递次数统计）(ResumeDeliveryRel)实体类
 *
 * @author makejava
 * @since 2026-03-20 01:19:47
 */
public class ResumeDeliveryRel implements Serializable {
    private static final long serialVersionUID = 640388574088498330L;
    /**
     * 投递记录ID（主键）
     */
    private Long id;
    /**
     * 关联user.id（投递用户）
     */
    private Long userId;
    /**
     * 关联resume.id（投递的简历）
     */
    private Long resumeId;
    /**
     * 关联company.id（投递的公司）
     */
    private Long companyId;
    /**
     * 关联jd_job.id（投递的JD岗位）
     */
    private Long jdJobId;
    /**
     * 该简历投递该岗位的次数（默认1次，重复投递自增）
     */
    private Integer deliveryCount;
    /**
     * 首次投递时间
     */
    private Date firstDeliveryTime;
    /**
     * 最新投递时间
     */
    private Date latestDeliveryTime;
    /**
     * 投递状态：1-已投递 2-已查看 3-面试邀请 4-已拒绝 5-已录用
     */
    private Integer deliveryStatus;
    /**
     * 投递备注（如“内推/官网投递”）
     */
    private String remark;
    /**
     * 记录创建时间
     */
    private Date createTime;
    /**
     * 记录更新时间
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

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getJdJobId() {
        return jdJobId;
    }

    public void setJdJobId(Long jdJobId) {
        this.jdJobId = jdJobId;
    }

    public Integer getDeliveryCount() {
        return deliveryCount;
    }

    public void setDeliveryCount(Integer deliveryCount) {
        this.deliveryCount = deliveryCount;
    }

    public Date getFirstDeliveryTime() {
        return firstDeliveryTime;
    }

    public void setFirstDeliveryTime(Date firstDeliveryTime) {
        this.firstDeliveryTime = firstDeliveryTime;
    }

    public Date getLatestDeliveryTime() {
        return latestDeliveryTime;
    }

    public void setLatestDeliveryTime(Date latestDeliveryTime) {
        this.latestDeliveryTime = latestDeliveryTime;
    }

    public Integer getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(Integer deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

