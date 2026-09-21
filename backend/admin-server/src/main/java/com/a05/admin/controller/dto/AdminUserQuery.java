package com.a05.admin.controller.dto;

public class AdminUserQuery {

    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String phone;
    private String keyword;
    private Integer status;
    private Long paidServiceId;
    private Boolean hasPaidService;
    private Boolean hasResumeFile;
    private Boolean isAbnormal;
    private String activeStart;
    private String activeEnd;

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getPaidServiceId() {
        return paidServiceId;
    }

    public void setPaidServiceId(Long paidServiceId) {
        this.paidServiceId = paidServiceId;
    }

    public Boolean getHasPaidService() {
        return hasPaidService;
    }

    public void setHasPaidService(Boolean hasPaidService) {
        this.hasPaidService = hasPaidService;
    }

    public Boolean getHasResumeFile() {
        return hasResumeFile;
    }

    public void setHasResumeFile(Boolean hasResumeFile) {
        this.hasResumeFile = hasResumeFile;
    }

    public Boolean getIsAbnormal() {
        return isAbnormal;
    }

    public void setIsAbnormal(Boolean isAbnormal) {
        this.isAbnormal = isAbnormal;
    }

    public String getActiveStart() {
        return activeStart;
    }

    public void setActiveStart(String activeStart) {
        this.activeStart = activeStart;
    }

    public String getActiveEnd() {
        return activeEnd;
    }

    public void setActiveEnd(String activeEnd) {
        this.activeEnd = activeEnd;
    }
}

