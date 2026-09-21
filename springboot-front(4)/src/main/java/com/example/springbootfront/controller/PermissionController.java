package com.example.springbootfront.controller;

import com.example.springbootfront.entity.Permission;
import com.example.springbootfront.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * (Permission)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:44
 */
@RestController
@RequestMapping("permission")
public class PermissionController {
    /**
     * 服务对象
     */
    @Autowired
    private PermissionService permissionService;

    /**
     * 分页查询
     *
     * @param permission  筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<Permission>> queryByPage(Permission permission, PageRequest pageRequest) {
        return ResponseEntity.ok(this.permissionService.queryByPage(permission, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<Permission> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.permissionService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param permission 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<Permission> add(Permission permission) {
        return ResponseEntity.ok(this.permissionService.insert(permission));
    }

    /**
     * 编辑数据
     *
     * @param permission 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<Permission> edit(Permission permission) {
        return ResponseEntity.ok(this.permissionService.update(permission));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.permissionService.deleteById(id));
    }

}

