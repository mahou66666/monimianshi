package com.example.springbootfront.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String schoolId;
    private Long targetJdId;
    private Long targetCompanyId;
    private String realName;
    private Integer gender;
    private Integer identity;
    private String graduationYear;
    private String idCard;
    private String wechat;
    private String qq;
    private LocalDate birthday;
    private String avatarUrl;
    private BigDecimal estimatedSalary;
    private String targetSalary;
    private String mbtiType;
    private Integer prepareScore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

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

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public Long getTargetJdId() {
        return targetJdId;
    }

    public void setTargetJdId(Long targetJdId) {
        this.targetJdId = targetJdId;
    }

    public Long getTargetCompanyId() {
        return targetCompanyId;
    }

    public void setTargetCompanyId(Long targetCompanyId) {
        this.targetCompanyId = targetCompanyId;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Integer getIdentity() {
        return identity;
    }

    public void setIdentity(Integer identity) {
        this.identity = identity;
    }

    public String getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(String graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getWechat() {
        return wechat;
    }

    public void setWechat(String wechat) {
        this.wechat = wechat;
    }

    public String getQq() {
        return qq;
    }

    public void setQq(String qq) {
        this.qq = qq;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public BigDecimal getEstimatedSalary() {
        return estimatedSalary;
    }

    public void setEstimatedSalary(BigDecimal estimatedSalary) {
        this.estimatedSalary = estimatedSalary;
    }

    public String getTargetSalary() {
        return targetSalary;
    }

    public void setTargetSalary(String targetSalary) {
        this.targetSalary = targetSalary;
    }

    public String getMbtiType() {
        return mbtiType;
    }

    public void setMbtiType(String mbtiType) {
        this.mbtiType = mbtiType;
    }

    public Integer getPrepareScore() {
        return prepareScore;
    }

    public void setPrepareScore(Integer prepareScore) {
        this.prepareScore = prepareScore;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
