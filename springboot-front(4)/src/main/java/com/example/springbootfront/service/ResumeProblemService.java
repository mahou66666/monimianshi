package com.example.springbootfront.service;

import com.example.springbootfront.entity.ResumeProblem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 简历待优化问题表(ResumeProblem)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:49
 */
public interface ResumeProblemService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    ResumeProblem queryById(Long id);

    /**
     * 分页查询
     *
     * @param resumeProblem 筛选条件
     * @param pageRequest   分页对象
     * @return 查询结果
     */
    Page<ResumeProblem> queryByPage(ResumeProblem resumeProblem, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param resumeProblem 实例对象
     * @return 实例对象
     */
    ResumeProblem insert(ResumeProblem resumeProblem);

    /**
     * 修改数据
     *
     * @param resumeProblem 实例对象
     * @return 实例对象
     */
    ResumeProblem update(ResumeProblem resumeProblem);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
