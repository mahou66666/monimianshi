package com.example.springbootfront.dao;

import com.example.springbootfront.entity.UserEducation;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * (UserEducation)表数据库访问层
 *
 * @author makejava
 * @since 2026-03-20 01:19:51
 */
public interface UserEducationDao {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    UserEducation queryById(Long id);

    /**
     * 查询指定行数据
     *
     * @param userEducation 查询条件
     * @param pageable      分页对象
     * @return 对象列表
     */
    List<UserEducation> queryAllByLimit(UserEducation userEducation, @Param("pageable") Pageable pageable);

    /**
     * 统计总行数
     *
     * @param userEducation 查询条件
     * @return 总行数
     */
    long count(UserEducation userEducation);

    /**
     * 新增数据
     *
     * @param userEducation 实例对象
     * @return 影响行数
     */
    int insert(UserEducation userEducation);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<UserEducation> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<UserEducation> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<UserEducation> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<UserEducation> entities);

    /**
     * 修改数据
     *
     * @param userEducation 实例对象
     * @return 影响行数
     */
    int update(UserEducation userEducation);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

}

