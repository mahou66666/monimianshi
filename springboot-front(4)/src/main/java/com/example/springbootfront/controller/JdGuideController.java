package com.example.springbootfront.controller;

import com.example.springbootfront.entity.JdGuide;
import com.example.springbootfront.service.JdGuideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * JD指导建议表(JdGuide)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:43
 */
@RestController
@RequestMapping("jdGuide")
public class JdGuideController {
    /**
     * 服务对象
     */
    @Autowired
    private JdGuideService jdGuideService;

    /**
     * 分页查询
     *
     * @param jdGuide     筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<JdGuide>> queryByPage(JdGuide jdGuide, PageRequest pageRequest) {
        return ResponseEntity.ok(this.jdGuideService.queryByPage(jdGuide, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<JdGuide> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.jdGuideService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param jdGuide 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<JdGuide> add(JdGuide jdGuide) {
        return ResponseEntity.ok(this.jdGuideService.insert(jdGuide));
    }

    /**
     * 编辑数据
     *
     * @param jdGuide 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<JdGuide> edit(JdGuide jdGuide) {
        return ResponseEntity.ok(this.jdGuideService.update(jdGuide));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.jdGuideService.deleteById(id));
    }

}

