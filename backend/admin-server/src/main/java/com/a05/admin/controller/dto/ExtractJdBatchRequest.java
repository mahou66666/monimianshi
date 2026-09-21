package com.a05.admin.controller.dto;

import java.util.List;

public class ExtractJdBatchRequest {

    private List<Long> jdIds;

    public List<Long> getJdIds() {
        return jdIds;
    }

    public void setJdIds(List<Long> jdIds) {
        this.jdIds = jdIds;
    }
}
