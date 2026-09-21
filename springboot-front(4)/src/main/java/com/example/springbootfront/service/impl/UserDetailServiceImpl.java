package com.example.springbootfront.service.impl;

import com.example.springbootfront.dao.UserDetailMapper;
import com.example.springbootfront.entity.UserDetail;
import com.example.springbootfront.service.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("userDetailService")
public class UserDetailServiceImpl implements UserDetailService {

    @Autowired
    private UserDetailMapper userDetailMapper;

    @Override
    public UserDetail queryById(Long id) {
        return userDetailMapper.selectById(id);
    }

    @Override
    public UserDetail queryByUserId(Long userId) {
        return userDetailMapper.selectByUserId(userId);
    }

    @Override
    public List<UserDetail> queryAll() {
        return userDetailMapper.selectAll();
    }

    @Override
    public UserDetail add(UserDetail userDetail) {
        userDetailMapper.insert(userDetail);
        return userDetailMapper.selectById(userDetail.getId());
    }

    @Override
    public UserDetail edit(UserDetail userDetail) {
        userDetailMapper.updateById(userDetail);
        return userDetailMapper.selectById(userDetail.getId());
    }

    @Override
    public boolean deleteById(Long id) {
        return userDetailMapper.deleteById(id) > 0;
    }
}
