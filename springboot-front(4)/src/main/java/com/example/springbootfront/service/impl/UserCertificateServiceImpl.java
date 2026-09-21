package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.UserCertificate;
import com.example.springbootfront.dao.UserCertificateDao;
import com.example.springbootfront.service.UserCertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * (UserCertificate)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:51
 */
@Service("userCertificateService")
public class UserCertificateServiceImpl implements UserCertificateService {
    @Autowired
    private UserCertificateDao userCertificateDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public UserCertificate queryById(Long id) {
        return this.userCertificateDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param userCertificate 筛选条件
     * @param pageRequest     分页对象
     * @return 查询结果
     */
    @Override
    public Page<UserCertificate> queryByPage(UserCertificate userCertificate, PageRequest pageRequest) {
        long total = this.userCertificateDao.count(userCertificate);
        return new PageImpl<>(this.userCertificateDao.queryAllByLimit(userCertificate, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param userCertificate 实例对象
     * @return 实例对象
     */
    @Override
    public UserCertificate insert(UserCertificate userCertificate) {
        this.userCertificateDao.insert(userCertificate);
        return userCertificate;
    }

    /**
     * 修改数据
     *
     * @param userCertificate 实例对象
     * @return 实例对象
     */
    @Override
    public UserCertificate update(UserCertificate userCertificate) {
        this.userCertificateDao.update(userCertificate);
        return this.queryById(userCertificate.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.userCertificateDao.deleteById(id) > 0;
    }
}
