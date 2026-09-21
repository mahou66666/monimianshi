package com.example.springbootfront.service;

import com.example.springbootfront.entity.JdJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 简历JD工作岗位表(JdJob)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:44
 */
public interface JdJobService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    JdJob queryById(Long id);

    /**
     * 分页查询
     *
     * @param jdJob       筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    Page<JdJob> queryByPage(JdJob jdJob, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param jdJob 实例对象
     * @return 实例对象
     */
    JdJob insert(JdJob jdJob);

    /**
     * 修改数据
     *
     * @param jdJob 实例对象
     * @return 实例对象
     */
    JdJob update(JdJob jdJob);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
