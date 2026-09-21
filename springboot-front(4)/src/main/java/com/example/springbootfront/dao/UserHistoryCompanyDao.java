package com.example.springbootfront.dao;

import com.example.springbootfront.entity.UserHistoryCompany;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * (UserHistoryCompany)表数据库访问层
 *
 * @author makejava
 * @since 2026-03-20 01:19:51
 */
public interface UserHistoryCompanyDao {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    UserHistoryCompany queryById(Long id);

    /**
     * 查询指定行数据
     *
     * @param userHistoryCompany 查询条件
     * @param pageable           分页对象
     * @return 对象列表
     */
    List<UserHistoryCompany> queryAllByLimit(UserHistoryCompany userHistoryCompany, @Param("pageable") Pageable pageable);

    /**
     * 统计总行数
     *
     * @param userHistoryCompany 查询条件
     * @return 总行数
     */
    long count(UserHistoryCompany userHistoryCompany);

    /**
     * 新增数据
     *
     * @param userHistoryCompany 实例对象
     * @return 影响行数
     */
    int insert(UserHistoryCompany userHistoryCompany);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<UserHistoryCompany> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<UserHistoryCompany> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<UserHistoryCompany> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<UserHistoryCompany> entities);

    /**
     * 修改数据
     *
     * @param userHistoryCompany 实例对象
     * @return 影响行数
     */
    int update(UserHistoryCompany userHistoryCompany);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

}

