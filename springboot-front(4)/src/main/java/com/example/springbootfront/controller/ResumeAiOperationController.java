package com.example.springbootfront.controller;

import com.example.springbootfront.entity.ResumeAiOperation;
import com.example.springbootfront.service.ResumeAiOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * AI简历操作记录表(ResumeAiOperation)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:46
 */
@RestController
@RequestMapping("resumeAiOperation")
public class ResumeAiOperationController {
    /**
     * 服务对象
     */
    @Autowired
    private ResumeAiOperationService resumeAiOperationService;

    /**
     * 分页查询
     *
     * @param resumeAiOperation 筛选条件
     * @param pageRequest       分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<ResumeAiOperation>> queryByPage(ResumeAiOperation resumeAiOperation, PageRequest pageRequest) {
        return ResponseEntity.ok(this.resumeAiOperationService.queryByPage(resumeAiOperation, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<ResumeAiOperation> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.resumeAiOperationService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param resumeAiOperation 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<ResumeAiOperation> add(ResumeAiOperation resumeAiOperation) {
        return ResponseEntity.ok(this.resumeAiOperationService.insert(resumeAiOperation));
    }

    /**
     * 编辑数据
     *
     * @param resumeAiOperation 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<ResumeAiOperation> edit(ResumeAiOperation resumeAiOperation) {
        return ResponseEntity.ok(this.resumeAiOperationService.update(resumeAiOperation));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.resumeAiOperationService.deleteById(id));
    }

}

