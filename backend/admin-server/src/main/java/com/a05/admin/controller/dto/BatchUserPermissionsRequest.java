package com.a05.admin.controller.dto;

import java.util.List;

public class BatchUserPermissionsRequest {

    private List<Long> userIds;

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}

