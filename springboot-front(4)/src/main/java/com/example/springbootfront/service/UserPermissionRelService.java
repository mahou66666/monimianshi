package com.example.springbootfront.service;

import com.example.springbootfront.entity.UserPermissionRel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * (UserPermissionRel)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:54
 */
public interface UserPermissionRelService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    UserPermissionRel queryById(Long id);

    /**
     * 分页查询
     *
     * @param userPermissionRel 筛选条件
     * @param pageRequest       分页对象
     * @return 查询结果
     */
    Page<UserPermissionRel> queryByPage(UserPermissionRel userPermissionRel, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param userPermissionRel 实例对象
     * @return 实例对象
     */
    UserPermissionRel insert(UserPermissionRel userPermissionRel);

    /**
     * 修改数据
     *
     * @param userPermissionRel 实例对象
     * @return 实例对象
     */
    UserPermissionRel update(UserPermissionRel userPermissionRel);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
