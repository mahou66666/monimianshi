package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.ResumeProblem;
import com.example.springbootfront.dao.ResumeProblemDao;
import com.example.springbootfront.service.ResumeProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * 简历待优化问题表(ResumeProblem)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:49
 */
@Service("resumeProblemService")
public class ResumeProblemServiceImpl implements ResumeProblemService {
    @Autowired
    private ResumeProblemDao resumeProblemDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public ResumeProblem queryById(Long id) {
        return this.resumeProblemDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param resumeProblem 筛选条件
     * @param pageRequest   分页对象
     * @return 查询结果
     */
    @Override
    public Page<ResumeProblem> queryByPage(ResumeProblem resumeProblem, PageRequest pageRequest) {
        long total = this.resumeProblemDao.count(resumeProblem);
        return new PageImpl<>(this.resumeProblemDao.queryAllByLimit(resumeProblem, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param resumeProblem 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeProblem insert(ResumeProblem resumeProblem) {
        this.resumeProblemDao.insert(resumeProblem);
        return resumeProblem;
    }

    /**
     * 修改数据
     *
     * @param resumeProblem 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeProblem update(ResumeProblem resumeProblem) {
        this.resumeProblemDao.update(resumeProblem);
        return this.queryById(resumeProblem.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.resumeProblemDao.deleteById(id) > 0;
    }
}
