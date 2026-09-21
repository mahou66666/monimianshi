package com.example.springbootfront.controller;

import com.example.springbootfront.entity.JdJob;
import com.example.springbootfront.service.JdJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 简历JD工作岗位表(JdJob)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:43
 */
@RestController
@RequestMapping("jdJob")
public class JdJobController {
    /**
     * 服务对象
     */
    @Autowired
    private JdJobService jdJobService;

    /**
     * 分页查询
     *
     * @param jdJob       筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<JdJob>> queryByPage(JdJob jdJob, PageRequest pageRequest) {
        return ResponseEntity.ok(this.jdJobService.queryByPage(jdJob, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<JdJob> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.jdJobService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param jdJob 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<JdJob> add(JdJob jdJob) {
        return ResponseEntity.ok(this.jdJobService.insert(jdJob));
    }

    /**
     * 编辑数据
     *
     * @param jdJob 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<JdJob> edit(JdJob jdJob) {
        return ResponseEntity.ok(this.jdJobService.update(jdJob));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.jdJobService.deleteById(id));
    }

}

