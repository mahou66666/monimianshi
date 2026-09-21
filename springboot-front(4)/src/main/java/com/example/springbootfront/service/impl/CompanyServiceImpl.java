package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.Company;
import com.example.springbootfront.dao.CompanyDao;
import com.example.springbootfront.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * 简历投递公司表(Company)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:37
 */
@Service("companyService")
public class CompanyServiceImpl implements CompanyService {
    @Autowired
    private CompanyDao companyDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public Company queryById(Long id) {
        return this.companyDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param company     筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @Override
    public Page<Company> queryByPage(Company company, PageRequest pageRequest) {
        long total = this.companyDao.count(company);
        return new PageImpl<>(this.companyDao.queryAllByLimit(company, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param company 实例对象
     * @return 实例对象
     */
    @Override
    public Company insert(Company company) {
        this.companyDao.insert(company);
        return company;
    }

    /**
     * 修改数据
     *
     * @param company 实例对象
     * @return 实例对象
     */
    @Override
    public Company update(Company company) {
        this.companyDao.update(company);
        return this.queryById(company.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.companyDao.deleteById(id) > 0;
    }
}
