package com.a05.admin.controller;

import com.a05.admin.auth.RequirePermission;
import com.a05.admin.common.Result;
import com.a05.admin.controller.dto.GenerateGeneralQuestionsRequest;
import com.a05.admin.controller.dto.GeneralQuestionQuery;
import com.a05.admin.controller.dto.UpdateGeneralQuestionRequest;
import com.a05.admin.service.AdminQuestionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interview/admin/questions")
@RequirePermission(anyOf = {"company:manage"})
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;

    public AdminQuestionController(AdminQuestionService adminQuestionService) {
        this.adminQuestionService = adminQuestionService;
    }

    @GetMapping("/{category}/list")
    public Result<?> list(@PathVariable String category, GeneralQuestionQuery query) {
        return Result.success(adminQuestionService.listQuestions(category, query));
    }

    @PostMapping("/{category}/generate")
    public Result<?> generate(@PathVariable String category,
                              @RequestBody(required = false) GenerateGeneralQuestionsRequest request) {
        return Result.success(adminQuestionService.generateQuestions(category, request));
    }

    @PutMapping("/{category}/{id}")
    public Result<?> update(@PathVariable String category,
                            @PathVariable Long id,
                            @RequestBody(required = false) UpdateGeneralQuestionRequest request) {
        return Result.success(adminQuestionService.updateQuestion(category, id, request));
    }

    @DeleteMapping("/{category}/{id}")
    public Result<?> delete(@PathVariable String category, @PathVariable Long id) {
        return Result.success(adminQuestionService.deleteQuestion(category, id));
    }
}
