package com.example.springbootfront.service;

import com.example.springbootfront.entity.JdGuide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * JD指导建议表(JdGuide)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:43
 */
public interface JdGuideService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    JdGuide queryById(Long id);

    /**
     * 分页查询
     *
     * @param jdGuide     筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    Page<JdGuide> queryByPage(JdGuide jdGuide, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param jdGuide 实例对象
     * @return 实例对象
     */
    JdGuide insert(JdGuide jdGuide);

    /**
     * 修改数据
     *
     * @param jdGuide 实例对象
     * @return 实例对象
     */
    JdGuide update(JdGuide jdGuide);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
