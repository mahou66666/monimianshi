package com.example.springbootfront.service;

import com.example.springbootfront.entity.User;

public interface UserService {

    User queryById(Long id);

    User save(User user);
}
