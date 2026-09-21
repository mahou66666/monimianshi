package com.example.springbootfront.service;

import com.example.springbootfront.entity.ResumeContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 简历内容详情表(ResumeContent)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:47
 */
public interface ResumeContentService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    ResumeContent queryById(Long id);

    /**
     * 分页查询
     *
     * @param resumeContent 筛选条件
     * @param pageRequest   分页对象
     * @return 查询结果
     */
    Page<ResumeContent> queryByPage(ResumeContent resumeContent, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param resumeContent 实例对象
     * @return 实例对象
     */
    ResumeContent insert(ResumeContent resumeContent);

    /**
     * 修改数据
     *
     * @param resumeContent 实例对象
     * @return 实例对象
     */
    ResumeContent update(ResumeContent resumeContent);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
