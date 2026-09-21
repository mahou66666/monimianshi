package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.ResumeFile;
import com.example.springbootfront.dao.ResumeFileDao;
import com.example.springbootfront.service.ResumeFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * 简历文件存储表(ResumeFile)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:48
 */
@Service("resumeFileService")
public class ResumeFileServiceImpl implements ResumeFileService {
    @Autowired
    private ResumeFileDao resumeFileDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public ResumeFile queryById(Long id) {
        return this.resumeFileDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param resumeFile  筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @Override
    public Page<ResumeFile> queryByPage(ResumeFile resumeFile, PageRequest pageRequest) {
        long total = this.resumeFileDao.count(resumeFile);
        return new PageImpl<>(this.resumeFileDao.queryAllByLimit(resumeFile, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param resumeFile 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeFile insert(ResumeFile resumeFile) {
        this.resumeFileDao.insert(resumeFile);
        return resumeFile;
    }

    /**
     * 修改数据
     *
     * @param resumeFile 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeFile update(ResumeFile resumeFile) {
        this.resumeFileDao.update(resumeFile);
        return this.queryById(resumeFile.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.resumeFileDao.deleteById(id) > 0;
    }
}
