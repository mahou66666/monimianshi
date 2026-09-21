package com.example.springbootfront.controller;

import com.example.springbootfront.common.Result;
import com.example.springbootfront.entity.User;
import com.example.springbootfront.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public Result<User> queryById(@PathVariable("id") Long id) {
        User user = userService.queryById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    @PutMapping
    public Result<User> save(@RequestBody User user) {
        User savedUser = userService.save(user);
        if (savedUser == null) {
            return Result.error("保存失败");
        }
        return Result.success(savedUser);
    }
}
