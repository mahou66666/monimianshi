package com.example.springbootfront.controller;

import com.example.springbootfront.entity.UserProject;
import com.example.springbootfront.service.UserProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * (UserProject)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:54
 */
@RestController
@RequestMapping("userProject")
public class UserProjectController {
    /**
     * 服务对象
     */
    @Autowired
    private UserProjectService userProjectService;

    /**
     * 分页查询
     *
     * @param userProject 筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<UserProject>> queryByPage(UserProject userProject, PageRequest pageRequest) {
        return ResponseEntity.ok(this.userProjectService.queryByPage(userProject, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<UserProject> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.userProjectService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param userProject 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<UserProject> add(UserProject userProject) {
        return ResponseEntity.ok(this.userProjectService.insert(userProject));
    }

    /**
     * 编辑数据
     *
     * @param userProject 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<UserProject> edit(UserProject userProject) {
        return ResponseEntity.ok(this.userProjectService.update(userProject));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.userProjectService.deleteById(id));
    }

}

