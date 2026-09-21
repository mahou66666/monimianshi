package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.UserInternship;
import com.example.springbootfront.dao.UserInternshipDao;
import com.example.springbootfront.service.UserInternshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * (UserInternship)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:54
 */
@Service("userInternshipService")
public class UserInternshipServiceImpl implements UserInternshipService {
    @Autowired
    private UserInternshipDao userInternshipDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public UserInternship queryById(Long id) {
        return this.userInternshipDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param userInternship 筛选条件
     * @param pageRequest    分页对象
     * @return 查询结果
     */
    @Override
    public Page<UserInternship> queryByPage(UserInternship userInternship, PageRequest pageRequest) {
        long total = this.userInternshipDao.count(userInternship);
        return new PageImpl<>(this.userInternshipDao.queryAllByLimit(userInternship, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param userInternship 实例对象
     * @return 实例对象
     */
    @Override
    public UserInternship insert(UserInternship userInternship) {
        this.userInternshipDao.insert(userInternship);
        return userInternship;
    }

    /**
     * 修改数据
     *
     * @param userInternship 实例对象
     * @return 实例对象
     */
    @Override
    public UserInternship update(UserInternship userInternship) {
        this.userInternshipDao.update(userInternship);
        return this.queryById(userInternship.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.userInternshipDao.deleteById(id) > 0;
    }
}
