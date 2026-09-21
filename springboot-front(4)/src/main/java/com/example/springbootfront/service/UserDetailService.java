package com.example.springbootfront.service;

import com.example.springbootfront.entity.UserDetail;

import java.util.List;

public interface UserDetailService {

    UserDetail queryById(Long id);

    UserDetail queryByUserId(Long userId);

    List<UserDetail> queryAll();

    UserDetail add(UserDetail userDetail);

    UserDetail edit(UserDetail userDetail);

    boolean deleteById(Long id);
}
