package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.UserEducation;
import com.example.springbootfront.dao.UserEducationDao;
import com.example.springbootfront.service.UserEducationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * (UserEducation)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:51
 */
@Service("userEducationService")
public class UserEducationServiceImpl implements UserEducationService {
    @Autowired
    private UserEducationDao userEducationDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public UserEducation queryById(Long id) {
        return this.userEducationDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param userEducation 筛选条件
     * @param pageRequest   分页对象
     * @return 查询结果
     */
    @Override
    public Page<UserEducation> queryByPage(UserEducation userEducation, PageRequest pageRequest) {
        long total = this.userEducationDao.count(userEducation);
        return new PageImpl<>(this.userEducationDao.queryAllByLimit(userEducation, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param userEducation 实例对象
     * @return 实例对象
     */
    @Override
    public UserEducation insert(UserEducation userEducation) {
        this.userEducationDao.insert(userEducation);
        return userEducation;
    }

    /**
     * 修改数据
     *
     * @param userEducation 实例对象
     * @return 实例对象
     */
    @Override
    public UserEducation update(UserEducation userEducation) {
        this.userEducationDao.update(userEducation);
        return this.queryById(userEducation.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.userEducationDao.deleteById(id) > 0;
    }
}
