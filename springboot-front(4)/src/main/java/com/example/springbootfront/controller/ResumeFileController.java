package com.example.springbootfront.controller;

import com.example.springbootfront.entity.ResumeFile;
import com.example.springbootfront.service.ResumeFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 简历文件存储表(ResumeFile)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:47
 */
@RestController
@RequestMapping("resumeFile")
public class ResumeFileController {
    /**
     * 服务对象
     */
    @Autowired
    private ResumeFileService resumeFileService;

    /**
     * 分页查询
     *
     * @param resumeFile  筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<ResumeFile>> queryByPage(ResumeFile resumeFile, PageRequest pageRequest) {
        return ResponseEntity.ok(this.resumeFileService.queryByPage(resumeFile, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<ResumeFile> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.resumeFileService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param resumeFile 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<ResumeFile> add(ResumeFile resumeFile) {
        return ResponseEntity.ok(this.resumeFileService.insert(resumeFile));
    }

    /**
     * 编辑数据
     *
     * @param resumeFile 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<ResumeFile> edit(ResumeFile resumeFile) {
        return ResponseEntity.ok(this.resumeFileService.update(resumeFile));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.resumeFileService.deleteById(id));
    }

}

