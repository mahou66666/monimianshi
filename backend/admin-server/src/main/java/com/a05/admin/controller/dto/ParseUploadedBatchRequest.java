package com.a05.admin.controller.dto;

import java.util.List;

public class ParseUploadedBatchRequest {

    private List<Long> resumeFileIds;

    public List<Long> getResumeFileIds() {
        return resumeFileIds;
    }

    public void setResumeFileIds(List<Long> resumeFileIds) {
        this.resumeFileIds = resumeFileIds;
    }
}

