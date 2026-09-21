package com.a05.admin.controller.dto;

import java.util.List;

public class ResumeContentBatchRequest {

    private List<Long> resumeIds;

    public List<Long> getResumeIds() {
        return resumeIds;
    }

    public void setResumeIds(List<Long> resumeIds) {
        this.resumeIds = resumeIds;
    }
}

