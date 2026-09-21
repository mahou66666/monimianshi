package com.example.springbootfront.service;

import com.example.springbootfront.entity.ResumeFragment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 简历片段表(ResumeFragment)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:49
 */
public interface ResumeFragmentService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    ResumeFragment queryById(Long id);

    /**
     * 分页查询
     *
     * @param resumeFragment 筛选条件
     * @param pageRequest    分页对象
     * @return 查询结果
     */
    Page<ResumeFragment> queryByPage(ResumeFragment resumeFragment, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param resumeFragment 实例对象
     * @return 实例对象
     */
    ResumeFragment insert(ResumeFragment resumeFragment);

    /**
     * 修改数据
     *
     * @param resumeFragment 实例对象
     * @return 实例对象
     */
    ResumeFragment update(ResumeFragment resumeFragment);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
