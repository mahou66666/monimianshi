package com.example.springbootfront.controller;

import com.example.springbootfront.entity.ResumeProblem;
import com.example.springbootfront.service.ResumeProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 简历待优化问题表(ResumeProblem)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:49
 */
@RestController
@RequestMapping("resumeProblem")
public class ResumeProblemController {
    /**
     * 服务对象
     */
    @Autowired
    private ResumeProblemService resumeProblemService;

    /**
     * 分页查询
     *
     * @param resumeProblem 筛选条件
     * @param pageRequest   分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<ResumeProblem>> queryByPage(ResumeProblem resumeProblem, PageRequest pageRequest) {
        return ResponseEntity.ok(this.resumeProblemService.queryByPage(resumeProblem, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<ResumeProblem> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.resumeProblemService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param resumeProblem 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<ResumeProblem> add(ResumeProblem resumeProblem) {
        return ResponseEntity.ok(this.resumeProblemService.insert(resumeProblem));
    }

    /**
     * 编辑数据
     *
     * @param resumeProblem 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<ResumeProblem> edit(ResumeProblem resumeProblem) {
        return ResponseEntity.ok(this.resumeProblemService.update(resumeProblem));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.resumeProblemService.deleteById(id));
    }

}

