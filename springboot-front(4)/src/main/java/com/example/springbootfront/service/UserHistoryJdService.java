package com.example.springbootfront.service;

import com.example.springbootfront.entity.UserHistoryJd;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * (UserHistoryJd)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:52
 */
public interface UserHistoryJdService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    UserHistoryJd queryById(Long id);

    /**
     * 分页查询
     *
     * @param userHistoryJd 筛选条件
     * @param pageRequest   分页对象
     * @return 查询结果
     */
    Page<UserHistoryJd> queryByPage(UserHistoryJd userHistoryJd, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param userHistoryJd 实例对象
     * @return 实例对象
     */
    UserHistoryJd insert(UserHistoryJd userHistoryJd);

    /**
     * 修改数据
     *
     * @param userHistoryJd 实例对象
     * @return 实例对象
     */
    UserHistoryJd update(UserHistoryJd userHistoryJd);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
