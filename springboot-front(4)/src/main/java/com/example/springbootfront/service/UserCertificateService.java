package com.example.springbootfront.service;

import com.example.springbootfront.entity.UserCertificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * (UserCertificate)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:50
 */
public interface UserCertificateService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    UserCertificate queryById(Long id);

    /**
     * 分页查询
     *
     * @param userCertificate 筛选条件
     * @param pageRequest     分页对象
     * @return 查询结果
     */
    Page<UserCertificate> queryByPage(UserCertificate userCertificate, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param userCertificate 实例对象
     * @return 实例对象
     */
    UserCertificate insert(UserCertificate userCertificate);

    /**
     * 修改数据
     *
     * @param userCertificate 实例对象
     * @return 实例对象
     */
    UserCertificate update(UserCertificate userCertificate);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
