package com.example.springbootfront.controller;

import com.example.springbootfront.entity.UserHistoryCompany;
import com.example.springbootfront.service.UserHistoryCompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * (UserHistoryCompany)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:51
 */
@RestController
@RequestMapping("userHistoryCompany")
public class UserHistoryCompanyController {
    /**
     * 服务对象
     */
    @Autowired
    private UserHistoryCompanyService userHistoryCompanyService;

    /**
     * 分页查询
     *
     * @param userHistoryCompany 筛选条件
     * @param pageRequest        分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<UserHistoryCompany>> queryByPage(UserHistoryCompany userHistoryCompany, PageRequest pageRequest) {
        return ResponseEntity.ok(this.userHistoryCompanyService.queryByPage(userHistoryCompany, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<UserHistoryCompany> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.userHistoryCompanyService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param userHistoryCompany 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<UserHistoryCompany> add(UserHistoryCompany userHistoryCompany) {
        return ResponseEntity.ok(this.userHistoryCompanyService.insert(userHistoryCompany));
    }

    /**
     * 编辑数据
     *
     * @param userHistoryCompany 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<UserHistoryCompany> edit(UserHistoryCompany userHistoryCompany) {
        return ResponseEntity.ok(this.userHistoryCompanyService.update(userHistoryCompany));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.userHistoryCompanyService.deleteById(id));
    }

}

