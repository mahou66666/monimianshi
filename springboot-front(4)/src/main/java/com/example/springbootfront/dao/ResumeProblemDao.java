package com.example.springbootfront.dao;

import com.example.springbootfront.entity.ResumeProblem;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 简历待优化问题表(ResumeProblem)表数据库访问层
 *
 * @author makejava
 * @since 2026-03-20 01:19:49
 */
public interface ResumeProblemDao {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    ResumeProblem queryById(Long id);

    /**
     * 查询指定行数据
     *
     * @param resumeProblem 查询条件
     * @param pageable      分页对象
     * @return 对象列表
     */
    List<ResumeProblem> queryAllByLimit(@Param("resumeProblem") ResumeProblem resumeProblem,
                                        @Param("pageable") Pageable pageable);

    /**
     * 统计总行数
     *
     * @param resumeProblem 查询条件
     * @return 总行数
     */
    long count(@Param("resumeProblem") ResumeProblem resumeProblem);

    /**
     * 新增数据
     *
     * @param resumeProblem 实例对象
     * @return 影响行数
     */
    int insert(ResumeProblem resumeProblem);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<ResumeProblem> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<ResumeProblem> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<ResumeProblem> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<ResumeProblem> entities);

    /**
     * 修改数据
     *
     * @param resumeProblem 实例对象
     * @return 影响行数
     */
    int update(ResumeProblem resumeProblem);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

    int deleteByResumeId(@Param("resumeId") Long resumeId);

}

