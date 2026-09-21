package com.example.springbootfront.service;

import com.example.springbootfront.entity.UserEducation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * (UserEducation)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:51
 */
public interface UserEducationService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    UserEducation queryById(Long id);

    /**
     * 分页查询
     *
     * @param userEducation 筛选条件
     * @param pageRequest   分页对象
     * @return 查询结果
     */
    Page<UserEducation> queryByPage(UserEducation userEducation, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param userEducation 实例对象
     * @return 实例对象
     */
    UserEducation insert(UserEducation userEducation);

    /**
     * 修改数据
     *
     * @param userEducation 实例对象
     * @return 实例对象
     */
    UserEducation update(UserEducation userEducation);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
