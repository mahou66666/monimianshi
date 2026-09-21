package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.ResumeDeliveryRel;
import com.example.springbootfront.dao.ResumeDeliveryRelDao;
import com.example.springbootfront.service.ResumeDeliveryRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * 简历投递关系表（含投递次数统计）(ResumeDeliveryRel)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:47
 */
@Service("resumeDeliveryRelService")
public class ResumeDeliveryRelServiceImpl implements ResumeDeliveryRelService {
    @Autowired
    private ResumeDeliveryRelDao resumeDeliveryRelDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public ResumeDeliveryRel queryById(Long id) {
        return this.resumeDeliveryRelDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param resumeDeliveryRel 筛选条件
     * @param pageRequest       分页对象
     * @return 查询结果
     */
    @Override
    public Page<ResumeDeliveryRel> queryByPage(ResumeDeliveryRel resumeDeliveryRel, PageRequest pageRequest) {
        long total = this.resumeDeliveryRelDao.count(resumeDeliveryRel);
        return new PageImpl<>(this.resumeDeliveryRelDao.queryAllByLimit(resumeDeliveryRel, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param resumeDeliveryRel 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeDeliveryRel insert(ResumeDeliveryRel resumeDeliveryRel) {
        this.resumeDeliveryRelDao.insert(resumeDeliveryRel);
        return resumeDeliveryRel;
    }

    /**
     * 修改数据
     *
     * @param resumeDeliveryRel 实例对象
     * @return 实例对象
     */
    @Override
    public ResumeDeliveryRel update(ResumeDeliveryRel resumeDeliveryRel) {
        this.resumeDeliveryRelDao.update(resumeDeliveryRel);
        return this.queryById(resumeDeliveryRel.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.resumeDeliveryRelDao.deleteById(id) > 0;
    }
}
