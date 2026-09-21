package com.example.springbootfront.controller;

import com.example.springbootfront.entity.ResumeDeliveryRel;
import com.example.springbootfront.service.ResumeDeliveryRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 简历投递关系表（含投递次数统计）(ResumeDeliveryRel)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:47
 */
@RestController
@RequestMapping("resumeDeliveryRel")
public class ResumeDeliveryRelController {
    /**
     * 服务对象
     */
    @Autowired
    private ResumeDeliveryRelService resumeDeliveryRelService;

    /**
     * 分页查询
     *
     * @param resumeDeliveryRel 筛选条件
     * @param pageRequest       分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<ResumeDeliveryRel>> queryByPage(ResumeDeliveryRel resumeDeliveryRel, PageRequest pageRequest) {
        return ResponseEntity.ok(this.resumeDeliveryRelService.queryByPage(resumeDeliveryRel, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<ResumeDeliveryRel> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.resumeDeliveryRelService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param resumeDeliveryRel 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<ResumeDeliveryRel> add(ResumeDeliveryRel resumeDeliveryRel) {
        return ResponseEntity.ok(this.resumeDeliveryRelService.insert(resumeDeliveryRel));
    }

    /**
     * 编辑数据
     *
     * @param resumeDeliveryRel 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<ResumeDeliveryRel> edit(ResumeDeliveryRel resumeDeliveryRel) {
        return ResponseEntity.ok(this.resumeDeliveryRelService.update(resumeDeliveryRel));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.resumeDeliveryRelService.deleteById(id));
    }

}

