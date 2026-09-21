package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.JdJob;
import com.example.springbootfront.dao.JdJobDao;
import com.example.springbootfront.service.JdJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * 简历JD工作岗位表(JdJob)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:44
 */
@Service("jdJobService")
public class JdJobServiceImpl implements JdJobService {
    @Autowired
    private JdJobDao jdJobDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public JdJob queryById(Long id) {
        return this.jdJobDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param jdJob       筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @Override
    public Page<JdJob> queryByPage(JdJob jdJob, PageRequest pageRequest) {
        long total = this.jdJobDao.count(jdJob);
        return new PageImpl<>(this.jdJobDao.queryAllByLimit(jdJob, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param jdJob 实例对象
     * @return 实例对象
     */
    @Override
    public JdJob insert(JdJob jdJob) {
        this.jdJobDao.insert(jdJob);
        return jdJob;
    }

    /**
     * 修改数据
     *
     * @param jdJob 实例对象
     * @return 实例对象
     */
    @Override
    public JdJob update(JdJob jdJob) {
        this.jdJobDao.update(jdJob);
        return this.queryById(jdJob.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.jdJobDao.deleteById(id) > 0;
    }
}
