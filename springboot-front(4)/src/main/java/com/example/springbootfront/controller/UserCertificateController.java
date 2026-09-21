package com.example.springbootfront.controller;

import com.example.springbootfront.entity.UserCertificate;
import com.example.springbootfront.service.UserCertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * (UserCertificate)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:50
 */
@RestController
@RequestMapping("userCertificate")
public class UserCertificateController {
    /**
     * 服务对象
     */
    @Autowired
    private UserCertificateService userCertificateService;

    /**
     * 分页查询
     *
     * @param userCertificate 筛选条件
     * @param pageRequest     分页对象
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Page<UserCertificate>> queryByPage(UserCertificate userCertificate, PageRequest pageRequest) {
        return ResponseEntity.ok(this.userCertificateService.queryByPage(userCertificate, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public ResponseEntity<UserCertificate> queryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.userCertificateService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param userCertificate 实体
     * @return 新增结果
     */
    @PostMapping
    public ResponseEntity<UserCertificate> add(UserCertificate userCertificate) {
        return ResponseEntity.ok(this.userCertificateService.insert(userCertificate));
    }

    /**
     * 编辑数据
     *
     * @param userCertificate 实体
     * @return 编辑结果
     */
    @PutMapping
    public ResponseEntity<UserCertificate> edit(UserCertificate userCertificate) {
        return ResponseEntity.ok(this.userCertificateService.update(userCertificate));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public ResponseEntity<Boolean> deleteById(Long id) {
        return ResponseEntity.ok(this.userCertificateService.deleteById(id));
    }

}

