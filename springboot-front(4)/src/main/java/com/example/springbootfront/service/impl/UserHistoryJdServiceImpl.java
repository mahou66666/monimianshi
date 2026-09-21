package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.UserHistoryJd;
import com.example.springbootfront.dao.UserHistoryJdDao;
import com.example.springbootfront.service.UserHistoryJdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * (UserHistoryJd)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:52
 */
@Service("userHistoryJdService")
public class UserHistoryJdServiceImpl implements UserHistoryJdService {
    @Autowired
    private UserHistoryJdDao userHistoryJdDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public UserHistoryJd queryById(Long id) {
        return this.userHistoryJdDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param userHistoryJd 筛选条件
     * @param pageRequest   分页对象
     * @return 查询结果
     */
    @Override
    public Page<UserHistoryJd> queryByPage(UserHistoryJd userHistoryJd, PageRequest pageRequest) {
        long total = this.userHistoryJdDao.count(userHistoryJd);
        return new PageImpl<>(this.userHistoryJdDao.queryAllByLimit(userHistoryJd, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param userHistoryJd 实例对象
     * @return 实例对象
     */
    @Override
    public UserHistoryJd insert(UserHistoryJd userHistoryJd) {
        this.userHistoryJdDao.insert(userHistoryJd);
        return userHistoryJd;
    }

    /**
     * 修改数据
     *
     * @param userHistoryJd 实例对象
     * @return 实例对象
     */
    @Override
    public UserHistoryJd update(UserHistoryJd userHistoryJd) {
        this.userHistoryJdDao.update(userHistoryJd);
        return this.queryById(userHistoryJd.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.userHistoryJdDao.deleteById(id) > 0;
    }
}
