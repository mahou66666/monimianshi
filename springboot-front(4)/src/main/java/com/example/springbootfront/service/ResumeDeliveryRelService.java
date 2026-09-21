package com.example.springbootfront.service;

import com.example.springbootfront.entity.ResumeDeliveryRel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 简历投递关系表（含投递次数统计）(ResumeDeliveryRel)表服务接口
 *
 * @author makejava
 * @since 2026-03-20 01:19:47
 */
public interface ResumeDeliveryRelService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    ResumeDeliveryRel queryById(Long id);

    /**
     * 分页查询
     *
     * @param resumeDeliveryRel 筛选条件
     * @param pageRequest       分页对象
     * @return 查询结果
     */
    Page<ResumeDeliveryRel> queryByPage(ResumeDeliveryRel resumeDeliveryRel, PageRequest pageRequest);

    /**
     * 新增数据
     *
     * @param resumeDeliveryRel 实例对象
     * @return 实例对象
     */
    ResumeDeliveryRel insert(ResumeDeliveryRel resumeDeliveryRel);

    /**
     * 修改数据
     *
     * @param resumeDeliveryRel 实例对象
     * @return 实例对象
     */
    ResumeDeliveryRel update(ResumeDeliveryRel resumeDeliveryRel);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

}
