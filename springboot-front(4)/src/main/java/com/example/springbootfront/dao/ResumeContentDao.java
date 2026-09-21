package com.example.springbootfront.dao;

import com.example.springbootfront.entity.ResumeContent;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 简历内容详情表(ResumeContent)表数据库访问层
 *
 * @author makejava
 * @since 2026-03-20 01:19:46
 */
public interface ResumeContentDao {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    ResumeContent queryById(Long id);

    /**
     * 查询指定行数据
     *
     * @param resumeContent 查询条件
     * @param pageable      分页对象
     * @return 对象列表
     */
    List<ResumeContent> queryAllByLimit(ResumeContent resumeContent, @Param("pageable") Pageable pageable);

    /**
     * 统计总行数
     *
     * @param resumeContent 查询条件
     * @return 总行数
     */
    long count(ResumeContent resumeContent);

    /**
     * 新增数据
     *
     * @param resumeContent 实例对象
     * @return 影响行数
     */
    int insert(ResumeContent resumeContent);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<ResumeContent> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<ResumeContent> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<ResumeContent> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<ResumeContent> entities);

    /**
     * 修改数据
     *
     * @param resumeContent 实例对象
     * @return 影响行数
     */
    int update(ResumeContent resumeContent);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

}

