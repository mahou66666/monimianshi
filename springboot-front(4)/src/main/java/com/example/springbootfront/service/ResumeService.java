package com.example.springbootfront.service;

import com.example.springbootfront.dto.resume.ResumeImportResult;
import com.example.springbootfront.entity.Resume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 简历基础信息表(Resume)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:45
 */
public interface ResumeService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    Resume queryById(Long id);

    /**
     * 分页查询
     *
     * @param resume      筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    Page<Resume> queryByPage(Resume resume, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param resume 实例对象
     * @return 实例对象
     */
    Resume insert(Resume resume);

    /**
     * 修改数据
     *
     * @param resume 实例对象
     * @return 实例对象
     */
    Resume update(Resume resume);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

    ResumeImportResult importByParser(Long userId, MultipartFile file, String title);

    Map<String, Object> analyzeResume(Long resumeId, String targetJdText);

}
