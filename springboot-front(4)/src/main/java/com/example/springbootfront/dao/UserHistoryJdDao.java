package com.example.springbootfront.dao;

import com.example.springbootfront.entity.UserHistoryJd;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * (UserHistoryJd)表数据库访问层
 *
 * @author makejava
 * @since 2026-03-20 01:19:52
 */
public interface UserHistoryJdDao {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    UserHistoryJd queryById(Long id);

    /**
     * 查询指定行数据
     *
     * @param userHistoryJd 查询条件
     * @param pageable      分页对象
     * @return 对象列表
     */
    List<UserHistoryJd> queryAllByLimit(UserHistoryJd userHistoryJd, @Param("pageable") Pageable pageable);

    /**
     * 统计总行数
     *
     * @param userHistoryJd 查询条件
     * @return 总行数
     */
    long count(UserHistoryJd userHistoryJd);

    /**
     * 新增数据
     *
     * @param userHistoryJd 实例对象
     * @return 影响行数
     */
    int insert(UserHistoryJd userHistoryJd);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<UserHistoryJd> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<UserHistoryJd> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<UserHistoryJd> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<UserHistoryJd> entities);

    /**
     * 修改数据
     *
     * @param userHistoryJd 实例对象
     * @return 影响行数
     */
    int update(UserHistoryJd userHistoryJd);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

}

