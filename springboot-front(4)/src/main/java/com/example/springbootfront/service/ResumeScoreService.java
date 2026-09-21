package com.example.springbootfront.service;

import com.example.springbootfront.entity.ResumeScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 简历评分表(ResumeScore)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:50
 */
public interface ResumeScoreService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    ResumeScore queryById(Long id);

    /**
     * 分页查询
     *
     * @param resumeScore 筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    Page<ResumeScore> queryByPage(ResumeScore resumeScore, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param resumeScore 实例对象
     * @return 实例对象
     */
    ResumeScore insert(ResumeScore resumeScore);

    /**
     * 修改数据
     *
     * @param resumeScore 实例对象
     * @return 实例对象
     */
    ResumeScore update(ResumeScore resumeScore);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
