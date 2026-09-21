package com.example.springbootfront.service.impl;

import com.example.springbootfront.dao.UserDao;
import com.example.springbootfront.entity.User;
import com.example.springbootfront.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("userService")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public User queryById(Long id) {
        return userDao.queryById(id);
    }

    @Override
    public User save(User user) {
        if (user == null) {
            return null;
        }

        if (user.getId() == null) {
            return null;
        }

        User existingUser = userDao.queryById(user.getId());
        if (existingUser == null) {
            return null;
        }

        mergeUserData(existingUser, user);

        userDao.updateUserInfo(user);

        if (userDao.countUserDetailByUserId(user.getId()) > 0) {
            userDao.updateUserDetail(user);
        } else {
            userDao.insertUserDetail(user);
        }

        return userDao.queryById(user.getId());
    }

    private void mergeUserData(User existingUser, User newUser) {
        if (isBlank(newUser.getPhone())) {
            newUser.setPhone(existingUser.getPhone());
        }
        if (isBlank(newUser.getName())) {
            newUser.setName(existingUser.getName());
        }
        if (isBlank(newUser.getAvatar())) {
            newUser.setAvatar(existingUser.getAvatar());
        }
        if (isBlank(newUser.getGender())) {
            newUser.setGender(existingUser.getGender());
        }
        if (isBlank(newUser.getIdentity())) {
            newUser.setIdentity(existingUser.getIdentity());
        }
        if (isBlank(newUser.getGradYear())) {
            newUser.setGradYear(existingUser.getGradYear());
        }
        if (isBlank(newUser.getWechat())) {
            newUser.setWechat(existingUser.getWechat());
        }
        if (isBlank(newUser.getBirthday())) {
            newUser.setBirthday(existingUser.getBirthday());
        }
        if (isBlank(newUser.getDesc())) {
            newUser.setDesc(existingUser.getDesc());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
