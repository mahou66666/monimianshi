package com.example.springbootfront.controller;

import com.example.springbootfront.entity.UserPermissionRel;
import com.example.springbootfront.service.UserPermissionRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * (UserPermissionRel)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:54
 */
@RestController
@RequestMapping("userPermissionRel")
public class
UserPermissionRelController {
    /**
     * 服务对象
     */
    @Autowired
    private UserPermissionRelService userPermissionRelService;

    /**
     * 分页查询
     *
     * @param userPermissionRel 筛选条件
     * @param pageRequest       分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<UserPermissionRel>> queryByPage(UserPermissionRel userPermissionRel, PageRequest pageRequest) {
        return ResponseEntity.ok(this.userPermissionRelService.queryByPage(userPermissionRel, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<UserPermissionRel> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.userPermissionRelService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param userPermissionRel 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<UserPermissionRel> add(UserPermissionRel userPermissionRel) {
        return ResponseEntity.ok(this.userPermissionRelService.insert(userPermissionRel));
    }

    /**
     * 编辑数据
     *
     * @param userPermissionRel 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<UserPermissionRel> edit(UserPermissionRel userPermissionRel) {
        return ResponseEntity.ok(this.userPermissionRelService.update(userPermissionRel));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.userPermissionRelService.deleteById(id));
    }

}

