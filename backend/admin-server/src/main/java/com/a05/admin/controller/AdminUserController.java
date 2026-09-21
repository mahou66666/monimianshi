package com.a05.admin.controller;

import com.a05.admin.auth.RequirePermission;
import com.a05.admin.common.Result;
import com.a05.admin.controller.dto.AdminUserQuery;
import com.a05.admin.controller.dto.AssignPermissionsRequest;
import com.a05.admin.controller.dto.BatchUserPermissionsRequest;
import com.a05.admin.controller.dto.CreateAdminUserRequest;
import com.a05.admin.controller.dto.UpdateAdminUserRequest;
import com.a05.admin.service.AdminUserService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/interview/admin/users")
@RequirePermission(anyOf = {"company:manage"})
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Result<?> listUsers(AdminUserQuery query) {
        return Result.success(adminUserService.listUsers(query));
    }

    @GetMapping("/paid-service-options")
    public Result<?> paidServiceOptions() {
        return Result.success(adminUserService.listPermissionOptions());
    }

    @GetMapping("/permissions/options")
    public Result<?> permissionOptions() {
        return Result.success(adminUserService.listPermissionOptions());
    }

    @PostMapping
    public Result<?> createUser(@RequestBody CreateAdminUserRequest request) {
        return Result.success(adminUserService.createUser(request));
    }

    @GetMapping("/{userId}")
    public Result<?> userDetail(@PathVariable Long userId) {
        return Result.success(adminUserService.getUserDetail(userId));
    }

    @PutMapping("/{userId}")
    public Result<?> updateUser(@PathVariable Long userId, @RequestBody UpdateAdminUserRequest request) {
        return Result.success(adminUserService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    public Result<?> deleteUser(@PathVariable Long userId) {
        return Result.success(adminUserService.deleteUser(userId));
    }

    @PostMapping("/{userId}/disable")
    public Result<?> disableUser(@PathVariable Long userId) {
        return Result.success(adminUserService.disableUser(userId));
    }

    @PostMapping("/{userId}/enable")
    public Result<?> enableUser(@PathVariable Long userId) {
        return Result.success(adminUserService.enableUser(userId));
    }

    @GetMapping("/{userId}/status-panel")
    public Result<?> userStatusPanel(@PathVariable Long userId) {
        return Result.success(adminUserService.getUserStatusPanel(userId));
    }

    @GetMapping("/{userId}/permissions")
    public Result<?> userPermissions(@PathVariable Long userId) {
        return Result.success(adminUserService.getUserPermissions(userId));
    }

    @PostMapping("/permissions/batch")
    public Result<?> userPermissionsBatch(@RequestBody(required = false) BatchUserPermissionsRequest request) {
        List<Long> ids = request == null || request.getUserIds() == null
                ? new ArrayList<>()
                : request.getUserIds();
        return Result.success(adminUserService.getUserPermissionsBatch(ids));
    }

    @PutMapping("/{userId}/permissions")
    public Result<?> assignUserPermissions(@PathVariable Long userId, @RequestBody(required = false) AssignPermissionsRequest request) {
        List<Long> ids = request == null || request.getPermissionIds() == null
                ? new ArrayList<>()
                : request.getPermissionIds();
        Map<String, Object> data = adminUserService.assignUserPermissions(userId, ids);
        return Result.success(data);
    }
}
