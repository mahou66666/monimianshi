package com.example.springbootfront.service.impl;

import com.example.springbootfront.entity.JdGuide;
import com.example.springbootfront.dao.JdGuideDao;
import com.example.springbootfront.service.JdGuideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;


/**
 * JD指导建议表(JdGuide)表服务实现类
 *
 * @author makejava
 * @since 2026-03-20 01:19:43
 */
@Service("jdGuideService")
public class JdGuideServiceImpl implements JdGuideService {
    @Autowired
    private JdGuideDao jdGuideDao;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public JdGuide queryById(Long id) {
        return this.jdGuideDao.queryById(id);
    }

    /**
     * 分页查询
     *
     * @param jdGuide     筛选条件
     * @param pageRequest 分页对象
     * @return 查询结果
     */
    @Override
    public Page<JdGuide> queryByPage(JdGuide jdGuide, PageRequest pageRequest) {
        long total = this.jdGuideDao.count(jdGuide);
        return new PageImpl<>(this.jdGuideDao.queryAllByLimit(jdGuide, pageRequest), pageRequest, total);
    }

    /**
     * 新增数据
     *
     * @param jdGuide 实例对象
     * @return 实例对象
     */
    @Override
    public JdGuide insert(JdGuide jdGuide) {
        this.jdGuideDao.insert(jdGuide);
        return jdGuide;
    }

    /**
     * 修改数据
     *
     * @param jdGuide 实例对象
     * @return 实例对象
     */
    @Override
    public JdGuide update(JdGuide jdGuide) {
        this.jdGuideDao.update(jdGuide);
        return this.queryById(jdGuide.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        return this.jdGuideDao.deleteById(id) > 0;
    }
}
