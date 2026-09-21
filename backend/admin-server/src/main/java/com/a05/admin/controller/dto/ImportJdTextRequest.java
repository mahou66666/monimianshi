package com.a05.admin.controller.dto;

public class ImportJdTextRequest {

    private String text;
    private Long companyId;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
}
