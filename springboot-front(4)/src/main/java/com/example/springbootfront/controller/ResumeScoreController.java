package com.example.springbootfront.controller;

import com.example.springbootfront.entity.ResumeScore;
import com.example.springbootfront.service.ResumeScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 简历评分表(ResumeScore)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:49
 */
@RestController
@RequestMapping("resumeScore")
public class ResumeScoreController {
    /**
     * 服务对象
     */
    @Autowired
    private ResumeScoreService resumeScoreService;

    /**
     * 分页查询
     *
     * @param resumeScore 筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<ResumeScore>> queryByPage(ResumeScore resumeScore, PageRequest pageRequest) {
        return ResponseEntity.ok(this.resumeScoreService.queryByPage(resumeScore, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<ResumeScore> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.resumeScoreService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param resumeScore 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<ResumeScore> add(ResumeScore resumeScore) {
        return ResponseEntity.ok(this.resumeScoreService.insert(resumeScore));
    }

    /**
     * 编辑数据
     *
     * @param resumeScore 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<ResumeScore> edit(ResumeScore resumeScore) {
        return ResponseEntity.ok(this.resumeScoreService.update(resumeScore));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.resumeScoreService.deleteById(id));
    }

}

