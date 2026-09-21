package com.example.springbootfront.controller;

import com.example.springbootfront.common.Result;
import com.example.springbootfront.dto.resume.ResumeAnalysisRequest;
import com.example.springbootfront.dto.resume.ResumeImportResult;
import com.example.springbootfront.entity.Resume;
import com.example.springbootfront.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 简历基础信息表(Resume)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:45
 */
@RestController
@RequestMapping("resume")
public class ResumeController {
    /**
     * 服务对象
     */
    @Autowired
    private ResumeService resumeService;

    /**
     * 分页查询
     *
     * @param resume      筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<Resume>> queryByPage(Resume resume, PageRequest pageRequest) {
        return ResponseEntity.ok(this.resumeService.queryByPage(resume, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<Resume> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.resumeService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param resume 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<Resume> add(Resume resume) {
        return ResponseEntity.ok(this.resumeService.insert(resume));
    }

    /**
     * 编辑数据
     *
     * @param resume 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<Resume> edit(Resume resume) {
        return ResponseEntity.ok(this.resumeService.update(resume));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.resumeService.deleteById(id));
    }

    @PostMapping("/import")
    public Result<ResumeImportResult> importByParser(@RequestParam("userId") Long userId,
                                                     @RequestParam("file") MultipartFile file,
                                                     @RequestParam(value = "title", required = false) String title) {
        try {
            return Result.success(this.resumeService.importByParser(userId, file, title));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/analysis")
    public Result<Map<String, Object>> analyze(@RequestBody ResumeAnalysisRequest request) {
        try {
            if (request == null || request.getResumeId() == null) {
                return Result.error("resumeId is required");
            }
            return Result.success(this.resumeService.analyzeResume(request.getResumeId(), request.getTargetJdText()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

}

