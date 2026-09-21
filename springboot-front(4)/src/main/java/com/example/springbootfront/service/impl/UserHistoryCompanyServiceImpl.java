package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.UserHistoryCompany;
import com.example.springbootfront.dao.UserHistoryCompanyDao;
import com.example.springbootfront.service.UserHistoryCompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * (UserHistoryCompany)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:51
 */
@Service("userHistoryCompanyService")
public class UserHistoryCompanyServiceImpl implements UserHistoryCompanyService {
    @Autowired
    private UserHistoryCompanyDao userHistoryCompanyDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public UserHistoryCompany queryById(Long id) {
        return this.userHistoryCompanyDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param userHistoryCompany 筛选条件
     * @param pageRequest        分页对象
     * @return 查询结果
     */
    @Override
    public Page<UserHistoryCompany> queryByPage(UserHistoryCompany userHistoryCompany, PageRequest pageRequest) {
        long total = this.userHistoryCompanyDao.count(userHistoryCompany);
        return new PageImpl<>(this.userHistoryCompanyDao.queryAllByLimit(userHistoryCompany, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param userHistoryCompany 实例对象
     * @return 实例对象
     */
    @Override
    public UserHistoryCompany insert(UserHistoryCompany userHistoryCompany) {
        this.userHistoryCompanyDao.insert(userHistoryCompany);
        return userHistoryCompany;
    }

    /**
     * 修改数据
     *
     * @param userHistoryCompany 实例对象
     * @return 实例对象
     */
    @Override
    public UserHistoryCompany update(UserHistoryCompany userHistoryCompany) {
        this.userHistoryCompanyDao.update(userHistoryCompany);
        return this.queryById(userHistoryCompany.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.userHistoryCompanyDao.deleteById(id) > 0;
    }
}
