package com.example.springbootfront.service;

import com.example.springbootfront.entity.UserHistoryCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * (UserHistoryCompany)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:51
 */
public interface UserHistoryCompanyService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    UserHistoryCompany queryById(Long id);

    /**
     * 分页查询
     *
     * @param userHistoryCompany 筛选条件
     * @param pageRequest        分页对象
     * @return 查询结果
     */
    Page<UserHistoryCompany> queryByPage(UserHistoryCompany userHistoryCompany, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param userHistoryCompany 实例对象
     * @return 实例对象
     */
    UserHistoryCompany insert(UserHistoryCompany userHistoryCompany);

    /**
     * 修改数据
     *
     * @param userHistoryCompany 实例对象
     * @return 实例对象
     */
    UserHistoryCompany update(UserHistoryCompany userHistoryCompany);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
