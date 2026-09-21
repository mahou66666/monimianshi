package com.example.springbootfront.service;

import com.example.springbootfront.entity.ResumeFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 简历文件存储表(ResumeFile)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:48
 */
public interface ResumeFileService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    ResumeFile queryById(Long id);

    /**
     * 分页查询
     *
     * @param resumeFile  筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    Page<ResumeFile> queryByPage(ResumeFile resumeFile, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param resumeFile 实例对象
     * @return 实例对象
     */
    ResumeFile insert(ResumeFile resumeFile);

    /**
     * 修改数据
     *
     * @param resumeFile 实例对象
     * @return 实例对象
     */
    ResumeFile update(ResumeFile resumeFile);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
