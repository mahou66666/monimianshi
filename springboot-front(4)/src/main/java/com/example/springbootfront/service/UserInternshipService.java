package com.example.springbootfront.service;

import com.example.springbootfront.entity.UserInternship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * (UserInternship)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:52
 */
public interface UserInternshipService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    UserInternship queryById(Long id);

    /**
     * 分页查询
     *
     * @param userInternship 筛选条件
     * @param pageRequest    分页对象
     * @return 查询结果
     */
    Page<UserInternship> queryByPage(UserInternship userInternship, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param userInternship 实例对象
     * @return 实例对象
     */
    UserInternship insert(UserInternship userInternship);

    /**
     * 修改数据
     *
     * @param userInternship 实例对象
     * @return 实例对象
     */
    UserInternship update(UserInternship userInternship);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
