package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.UserProject;
import com.example.springbootfront.dao.UserProjectDao;
import com.example.springbootfront.service.UserProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * (UserProject)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:55
 */
@Service("userProjectService")
public class UserProjectServiceImpl implements UserProjectService {
    @Autowired
    private UserProjectDao userProjectDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public UserProject queryById(Long id) {
        return this.userProjectDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param userProject 筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @Override
    public Page<UserProject> queryByPage(UserProject userProject, PageRequest pageRequest) {
        long total = this.userProjectDao.count(userProject);
        return new PageImpl<>(this.userProjectDao.queryAllByLimit(userProject, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param userProject 实例对象
     * @return 实例对象
     */
    @Override
    public UserProject insert(UserProject userProject) {
        this.userProjectDao.insert(userProject);
        return userProject;
    }

    /**
     * 修改数据
     *
     * @param userProject 实例对象
     * @return 实例对象
     */
    @Override
    public UserProject update(UserProject userProject) {
        this.userProjectDao.update(userProject);
        return this.queryById(userProject.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.userProjectDao.deleteById(id) > 0;
    }
}
