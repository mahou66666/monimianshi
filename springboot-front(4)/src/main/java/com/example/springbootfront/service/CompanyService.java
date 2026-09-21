package com.example.springbootfront.service;

import com.example.springbootfront.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 简历投递公司表(Company)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:37
 */
public interface CompanyService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    Company queryById(Long id);

    /**
     * 分页查询
     *
     * @param company     筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    Page<Company> queryByPage(Company company, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param company 实例对象
     * @return 实例对象
     */
    Company insert(Company company);

    /**
     * 修改数据
     *
     * @param company 实例对象
     * @return 实例对象
     */
    Company update(Company company);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
