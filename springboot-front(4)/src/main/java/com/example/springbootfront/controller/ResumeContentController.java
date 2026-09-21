package com.example.springbootfront.controller;

import com.example.springbootfront.entity.ResumeContent;
import com.example.springbootfront.service.ResumeContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * 简历内容详情表(ResumeContent)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:46
 */
@RestController
@RequestMapping("resumeContent")
public class ResumeContentController {
    /**
     * 服务对象
     */
    @Autowired
    private ResumeContentService resumeContentService;

    /**
     * 分页查询
     *
     * @param resumeContent 筛选条件
     * @param pageRequest   分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<ResumeContent>> queryByPage(ResumeContent resumeContent, PageRequest pageRequest) {
        return ResponseEntity.ok(this.resumeContentService.queryByPage(resumeContent, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<ResumeContent> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.resumeContentService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param resumeContent 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<ResumeContent> add(ResumeContent resumeContent) {
        return ResponseEntity.ok(this.resumeContentService.insert(resumeContent));
    }

    /**
     * 编辑数据
     *
     * @param resumeContent 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<ResumeContent> edit(ResumeContent resumeContent) {
        return ResponseEntity.ok(this.resumeContentService.update(resumeContent));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.resumeContentService.deleteById(id));
    }

}

