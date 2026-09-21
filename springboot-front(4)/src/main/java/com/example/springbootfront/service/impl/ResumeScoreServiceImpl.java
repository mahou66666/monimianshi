package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.ResumeScore;
import com.example.springbootfront.dao.ResumeScoreDao;
import com.example.springbootfront.service.ResumeScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * 简历评分表(ResumeScore)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:50
 */
@Service("resumeScoreService")
public class ResumeScoreServiceImpl implements ResumeScoreService {
    @Autowired
    private ResumeScoreDao resumeScoreDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public ResumeScore queryById(Long id) {
        return this.resumeScoreDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param resumeScore 筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @Override
    public Page<ResumeScore> queryByPage(ResumeScore resumeScore, PageRequest pageRequest) {
        long total = this.resumeScoreDao.count(resumeScore);
        return new PageImpl<>(this.resumeScoreDao.queryAllByLimit(resumeScore, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param resumeScore 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeScore insert(ResumeScore resumeScore) {
        this.resumeScoreDao.insert(resumeScore);
        return resumeScore;
    }

    /**
     * 修改数据
     *
     * @param resumeScore 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeScore update(ResumeScore resumeScore) {
        this.resumeScoreDao.update(resumeScore);
        return this.queryById(resumeScore.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.resumeScoreDao.deleteById(id) > 0;
    }
}
