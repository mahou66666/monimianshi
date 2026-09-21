package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.ResumeAiOperation;
import com.example.springbootfront.dao.ResumeAiOperationDao;
import com.example.springbootfront.service.ResumeAiOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * AI简历操作记录表(ResumeAiOperation)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:46
 */
@Service("resumeAiOperationService")
public class ResumeAiOperationServiceImpl implements ResumeAiOperationService {
    @Autowired
    private ResumeAiOperationDao resumeAiOperationDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public ResumeAiOperation queryById(Long id) {
        return this.resumeAiOperationDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param resumeAiOperation 筛选条件
     * @param pageRequest       分页对象
     * @return 查询结果
     */
    @Override
    public Page<ResumeAiOperation> queryByPage(ResumeAiOperation resumeAiOperation, PageRequest pageRequest) {
        long total = this.resumeAiOperationDao.count(resumeAiOperation);
        return new PageImpl<>(this.resumeAiOperationDao.queryAllByLimit(resumeAiOperation, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param resumeAiOperation 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeAiOperation insert(ResumeAiOperation resumeAiOperation) {
        this.resumeAiOperationDao.insert(resumeAiOperation);
        return resumeAiOperation;
    }

    /**
     * 修改数据
     *
     * @param resumeAiOperation 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeAiOperation update(ResumeAiOperation resumeAiOperation) {
        this.resumeAiOperationDao.update(resumeAiOperation);
        return this.queryById(resumeAiOperation.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.resumeAiOperationDao.deleteById(id) > 0;
    }
}
