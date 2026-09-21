package com.a05.admin.controller.dto;

import java.util.List;

public class AssignPermissionsRequest {

    private List<Long> permissionIds;

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }
}

