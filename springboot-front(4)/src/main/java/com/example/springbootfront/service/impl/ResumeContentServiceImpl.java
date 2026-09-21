package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.ResumeContent;
import com.example.springbootfront.dao.ResumeContentDao;
import com.example.springbootfront.service.ResumeContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * 简历内容详情表(ResumeContent)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:47
 */
@Service("resumeContentService")
public class ResumeContentServiceImpl implements ResumeContentService {
    @Autowired
    private ResumeContentDao resumeContentDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public ResumeContent queryById(Long id) {
        return this.resumeContentDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param resumeContent 筛选条件
     * @param pageRequest   分页对象
     * @return 查询结果
     */
    @Override
    public Page<ResumeContent> queryByPage(ResumeContent resumeContent, PageRequest pageRequest) {
        long total = this.resumeContentDao.count(resumeContent);
        return new PageImpl<>(this.resumeContentDao.queryAllByLimit(resumeContent, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param resumeContent 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeContent insert(ResumeContent resumeContent) {
        this.resumeContentDao.insert(resumeContent);
        return resumeContent;
    }

    /**
     * 修改数据
     *
     * @param resumeContent 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeContent update(ResumeContent resumeContent) {
        this.resumeContentDao.update(resumeContent);
        return this.queryById(resumeContent.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.resumeContentDao.deleteById(id) > 0;
    }
}
