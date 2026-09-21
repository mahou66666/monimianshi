package com.a05.admin.controller;

import com.a05.admin.auth.RequirePermission;
import com.a05.admin.common.Result;
import com.a05.admin.controller.dto.AdminJdQuery;
import com.a05.admin.controller.dto.ExtractJdBatchRequest;
import com.a05.admin.controller.dto.ImportJdTextRequest;
import com.a05.admin.service.AdminJdService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/interview/admin/jd")
public class AdminJdController {

    private final AdminJdService adminJdService;

    public AdminJdController(AdminJdService adminJdService) {
        this.adminJdService = adminJdService;
    }

    @GetMapping("/list")
    @RequirePermission(anyOf = {"job:view", "job:add", "job:edit"})
    public Result<?> listJobs(AdminJdQuery query) {
        return Result.success(adminJdService.listJobs(query));
    }

    @GetMapping("/guide/list")
    @RequirePermission(anyOf = {"job:view", "job:add", "job:edit"})
    public Result<?> listGuideJobs(AdminJdQuery query) {
        return Result.success(adminJdService.listGuideJobs(query));
    }

    @GetMapping("/guide/{jdId}/latest")
    @RequirePermission(anyOf = {"job:view", "job:add", "job:edit"})
    public Result<?> latestGuide(@PathVariable Long jdId) {
        return Result.success(adminJdService.getLatestGuide(jdId));
    }

    @PostMapping("/guide/{jdId}/clear")
    @RequirePermission(anyOf = {"job:edit"})
    public Result<?> clearGuide(@PathVariable Long jdId) {
        return Result.success(adminJdService.clearGuide(jdId));
    }

    @PostMapping("/import/text-batch")
    @RequirePermission(anyOf = {"job:add", "job:edit"})
    public Result<?> importTextBatch(@RequestBody(required = false) ImportJdTextRequest request) {
        String text = request == null ? null : request.getText();
        Long companyId = request == null ? null : request.getCompanyId();
        return Result.success(adminJdService.importTextBatch(text, companyId));
    }

    @PostMapping("/extract/batch")
    @RequirePermission(anyOf = {"job:edit"})
    public Result<?> extractBatch(@RequestBody(required = false) ExtractJdBatchRequest request) {
        List<Long> ids = request == null || request.getJdIds() == null
                ? new ArrayList<>()
                : request.getJdIds();
        return Result.success(adminJdService.extractBatch(ids));
    }
}
