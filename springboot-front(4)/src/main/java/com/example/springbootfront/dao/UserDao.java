package com.example.springbootfront.dao;

import com.example.springbootfront.entity.User;

public interface UserDao {

    User queryById(Long id);

    int updateUserInfo(User user);

    int countUserDetailByUserId(Long userId);

    int insertUserDetail(User user);

    int updateUserDetail(User user);
}
