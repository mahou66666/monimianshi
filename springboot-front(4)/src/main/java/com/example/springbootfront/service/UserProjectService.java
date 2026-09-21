package com.example.springbootfront.service;

import com.example.springbootfront.entity.UserProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * (UserProject)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:55
 */
public interface UserProjectService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    UserProject queryById(Long id);

    /**
     * 分页查询
     *
     * @param userProject 筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    Page<UserProject> queryByPage(UserProject userProject, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param userProject 实例对象
     * @return 实例对象
     */
    UserProject insert(UserProject userProject);

    /**
     * 修改数据
     *
     * @param userProject 实例对象
     * @return 实例对象
     */
    UserProject update(UserProject userProject);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
