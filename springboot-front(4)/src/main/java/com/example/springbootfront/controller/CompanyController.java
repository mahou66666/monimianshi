package com.example.springbootfront.controller;

import com.example.springbootfront.common.Result;
import com.example.springbootfront.entity.Company;
import com.example.springbootfront.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 简历投递公司表(Company)表控制层
 *
 * @author makejava
 * @since 2026-03-20 01:19:16
 */
@RestController
@RequestMapping("company")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    /**
     * 分页查询
     *
     * @param company     筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @GetMapping
    public Result<Page<Company>> queryByPage(Company company, PageRequest pageRequest) {
        return Result.success(this.companyService.queryByPage(company, pageRequest));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("{id}")
    public Result<Company> queryById(@PathVariable("id") Long id) {
        return Result.success(this.companyService.queryById(id));
    }

    /**
     * 新增数据
     *
     * @param company 实体
     * @return 新增结果
     */
    @PostMapping
    public Result<Company> add(@RequestBody Company company) {
        return Result.success(this.companyService.insert(company));
    }

    /**
     * 编辑数据
     *
     * @param company 实体
     * @return 编辑结果
     */
    @PutMapping
    public Result<Company> edit(@RequestBody Company company) {
        return Result.success(this.companyService.update(company));
    }

    /**
     * 删除数据
     *
     * @param id 主键
     * @return 删除是否成功
     */
    @DeleteMapping
    public Result<Boolean> deleteById(Long id) {
        return Result.success(this.companyService.deleteById(id));
    }
}