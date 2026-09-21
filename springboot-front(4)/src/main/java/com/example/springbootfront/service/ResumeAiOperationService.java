package com.example.springbootfront.service;

import com.example.springbootfront.entity.ResumeAiOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * AI简历操作记录表(ResumeAiOperation)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:46
 */
public interface ResumeAiOperationService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    ResumeAiOperation queryById(Long id);

    /**
     * 分页查询
     *
     * @param resumeAiOperation 筛选条件
     * @param pageRequest       分页对象
     * @return 查询结果
     */
    Page<ResumeAiOperation> queryByPage(ResumeAiOperation resumeAiOperation, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param resumeAiOperation 实例对象
     * @return 实例对象
     */
    ResumeAiOperation insert(ResumeAiOperation resumeAiOperation);

    /**
     * 修改数据
     *
     * @param resumeAiOperation 实例对象
     * @return 实例对象
     */
    ResumeAiOperation update(ResumeAiOperation resumeAiOperation);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
