package com.a05.admin.controller;

import com.a05.admin.auth.RequirePermission;
import com.a05.admin.common.Result;
import com.a05.admin.service.AdminAlgorithmTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/interview/admin/users/{userId}/algorithms")
@RequirePermission(anyOf = {"job:view", "job:add", "job:edit", "resume:view", "resume:manage"})
public class AdminAlgorithmTaskController {

    private final AdminAlgorithmTaskService adminAlgorithmTaskService;

    public AdminAlgorithmTaskController(AdminAlgorithmTaskService adminAlgorithmTaskService) {
        this.adminAlgorithmTaskService = adminAlgorithmTaskService;
    }

    @PostMapping("/{algoKey}/tasks")
    public Result<?> createTask(@PathVariable Long userId,
                                @PathVariable String algoKey,
                                @RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> safeRequest = request == null ? new HashMap<>() : request;
        return Result.success(adminAlgorithmTaskService.createTask(userId, algoKey, safeRequest));
    }

    @GetMapping("/tasks/{taskId}")
    public Result<?> getTask(@PathVariable Long userId, @PathVariable String taskId) {
        return Result.success(adminAlgorithmTaskService.getTask(userId, taskId));
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public Result<?> cancelTask(@PathVariable Long userId, @PathVariable String taskId) {
        return Result.success(adminAlgorithmTaskService.cancelTask(userId, taskId));
    }

    @GetMapping("/tasks/{taskId}/pending-resume-ids")
    public Result<?> getPendingResumeIds(@PathVariable Long userId, @PathVariable String taskId) {
        return Result.success(adminAlgorithmTaskService.getPendingResumeIds(userId, taskId));
    }

    @GetMapping("/tasks/{taskId}/pending-resume-file-ids")
    public Result<?> getPendingResumeFileIds(@PathVariable Long userId, @PathVariable String taskId) {
        return Result.success(adminAlgorithmTaskService.getPendingResumeFileIds(userId, taskId));
    }
}
