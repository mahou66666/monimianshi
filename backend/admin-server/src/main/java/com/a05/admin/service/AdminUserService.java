package com.a05.admin.service;

import com.a05.admin.controller.dto.AdminUserQuery;
import com.a05.admin.controller.dto.CreateAdminUserRequest;
import com.a05.admin.controller.dto.UpdateAdminUserRequest;

import java.util.List;
import java.util.Map;

public interface AdminUserService {

    Map<String, Object> listUsers(AdminUserQuery query);

    List<Map<String, Object>> listPermissionOptions();

    Map<String, Object> createUser(CreateAdminUserRequest request);

    Map<String, Object> updateUser(Long userId, UpdateAdminUserRequest request);

    Map<String, Object> getUserDetail(Long userId);

    Map<String, Object> deleteUser(Long userId);

    Map<String, Object> disableUser(Long userId);

    Map<String, Object> enableUser(Long userId);

    Map<String, Object> getUserStatusPanel(Long userId);

    Map<String, Object> getUserPermissions(Long userId);

    Map<String, Object> getUserPermissionsBatch(List<Long> userIds);

    Map<String, Object> assignUserPermissions(Long userId, List<Long> permissionIds);
}
