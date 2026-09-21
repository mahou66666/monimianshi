package com.example.springbootfront.dao;

import com.example.springbootfront.entity.ResumeDeliveryRel;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 简历投递关系表（含投递次数统计）(ResumeDeliveryRel)表数据库访问层
 *
 * @author makejava
 * @since 2026-03-20 01:19:47
 */
public interface ResumeDeliveryRelDao {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    ResumeDeliveryRel queryById(Long id);

    /**
     * 查询指定行数据
     *
     * @param resumeDeliveryRel 查询条件
     * @param pageable          分页对象
     * @return 对象列表
     */
    List<ResumeDeliveryRel> queryAllByLimit(ResumeDeliveryRel resumeDeliveryRel, @Param("pageable") Pageable pageable);

    /**
     * 统计总行数
     *
     * @param resumeDeliveryRel 查询条件
     * @return 总行数
     */
    long count(ResumeDeliveryRel resumeDeliveryRel);

    /**
     * 新增数据
     *
     * @param resumeDeliveryRel 实例对象
     * @return 影响行数
     */
    int insert(ResumeDeliveryRel resumeDeliveryRel);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<ResumeDeliveryRel> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<ResumeDeliveryRel> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<ResumeDeliveryRel> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<ResumeDeliveryRel> entities);

    /**
     * 修改数据
     *
     * @param resumeDeliveryRel 实例对象
     * @return 影响行数
     */
    int update(ResumeDeliveryRel resumeDeliveryRel);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

}

