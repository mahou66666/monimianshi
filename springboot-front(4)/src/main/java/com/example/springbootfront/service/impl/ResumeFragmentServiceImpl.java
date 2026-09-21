package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.ResumeFragment;
import com.example.springbootfront.dao.ResumeFragmentDao;
import com.example.springbootfront.service.ResumeFragmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * 简历片段表(ResumeFragment)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:49
 */
@Service("resumeFragmentService")
public class ResumeFragmentServiceImpl implements ResumeFragmentService {
    @Autowired
    private ResumeFragmentDao resumeFragmentDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public ResumeFragment queryById(Long id) {
        return this.resumeFragmentDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param resumeFragment 筛选条件
     * @param pageRequest    分页对象
     * @return 查询结果
     */
    @Override
    public Page<ResumeFragment> queryByPage(ResumeFragment resumeFragment, PageRequest pageRequest) {
        long total = this.resumeFragmentDao.count(resumeFragment);
        return new PageImpl<>(this.resumeFragmentDao.queryAllByLimit(resumeFragment, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param resumeFragment 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeFragment insert(ResumeFragment resumeFragment) {
        this.resumeFragmentDao.insert(resumeFragment);
        return resumeFragment;
    }

    /**
     * 修改数据
     *
     * @param resumeFragment 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeFragment update(ResumeFragment resumeFragment) {
        this.resumeFragmentDao.update(resumeFragment);
        return this.queryById(resumeFragment.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.resumeFragmentDao.deleteById(id) > 0;
    }
}
