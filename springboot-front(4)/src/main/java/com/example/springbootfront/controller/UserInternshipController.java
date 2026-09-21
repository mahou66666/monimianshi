package com.example.springbootfront.controller;

import com.example.springbootfront.entity.UserInternship;
import com.example.springbootfront.service.UserInternshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * (UserInternship)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:52
 */
@RestController
@RequestMapping("userInternship")
public class UserInternshipController {
    /**
     * 服务对象
     */
    @Autowired
    private UserInternshipService userInternshipService;

    /**
     * 分页查询
     *
     * @param userInternship 筛选条件
     * @param pageRequest    分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<UserInternship>> queryByPage(UserInternship userInternship, PageRequest pageRequest) {
        return ResponseEntity.ok(this.userInternshipService.queryByPage(userInternship, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<UserInternship> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.userInternshipService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param userInternship 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<UserInternship> add(UserInternship userInternship) {
        return ResponseEntity.ok(this.userInternshipService.insert(userInternship));
    }

    /**
     * 编辑数据
     *
     * @param userInternship 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<UserInternship> edit(UserInternship userInternship) {
        return ResponseEntity.ok(this.userInternshipService.update(userInternship));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.userInternshipService.deleteById(id));
    }

}

