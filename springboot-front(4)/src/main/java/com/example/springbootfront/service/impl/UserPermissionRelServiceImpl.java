package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.UserPermissionRel;
import com.example.springbootfront.dao.UserPermissionRelDao;
import com.example.springbootfront.service.UserPermissionRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * (UserPermissionRel)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:54
 */
@Service("userPermissionRelService")
public class UserPermissionRelServiceImpl implements UserPermissionRelService {
    @Autowired
    private UserPermissionRelDao userPermissionRelDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public UserPermissionRel queryById(Long id) {
        return this.userPermissionRelDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param userPermissionRel 筛选条件
     * @param pageRequest       分页对象
     * @return 查询结果
     */
    @Override
    public Page<UserPermissionRel> queryByPage(UserPermissionRel userPermissionRel, PageRequest pageRequest) {
        long total = this.userPermissionRelDao.count(userPermissionRel);
        return new PageImpl<>(this.userPermissionRelDao.queryAllByLimit(userPermissionRel, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param userPermissionRel 实例对象
     * @return 实例对象
     */
    @Override
    public UserPermissionRel insert(UserPermissionRel userPermissionRel) {
        this.userPermissionRelDao.insert(userPermissionRel);
        return userPermissionRel;
    }

    /**
     * 修改数据
     *
     * @param userPermissionRel 实例对象
     * @return 实例对象
     */
    @Override
    public UserPermissionRel update(UserPermissionRel userPermissionRel) {
        this.userPermissionRelDao.update(userPermissionRel);
        return this.queryById(userPermissionRel.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.userPermissionRelDao.deleteById(id) > 0;
    }
}
