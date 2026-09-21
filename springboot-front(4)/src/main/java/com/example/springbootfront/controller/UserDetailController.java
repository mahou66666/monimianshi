package com.example.springbootfront.controller;

import com.example.springbootfront.common.Result;
import com.example.springbootfront.entity.UserDetail;
import com.example.springbootfront.service.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/userDetail")
public class UserDetailController {

    @Autowired
    private UserDetailService userDetailService;

    @GetMapping("/{id}")
    public Result<UserDetail> queryById(@PathVariable("id") Long id) {
        UserDetail userDetail = userDetailService.queryById(id);
        if (userDetail == null) {
            return Result.error("用户详情不存在");
        }
        return Result.success(userDetail);
    }

    @GetMapping("/byUserId")
    public Result<UserDetail> queryByUserId(@RequestParam("userId") Long userId) {
        UserDetail userDetail = userDetailService.queryByUserId(userId);
        if (userDetail == null) {
            return Result.error("用户详情不存在");
        }
        return Result.success(userDetail);
    }

    @GetMapping("/list")
    public Result<List<UserDetail>> queryAll() {
        return Result.success(userDetailService.queryAll());
    }

    @PostMapping
    public Result<UserDetail> add(@RequestBody UserDetail userDetail) {
        return Result.success(userDetailService.add(userDetail));
    }

    @PutMapping
    public Result<UserDetail> edit(@RequestBody UserDetail userDetail) {
        return Result.success(userDetailService.edit(userDetail));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteById(@PathVariable("id") Long id) {
        return Result.success(userDetailService.deleteById(id));
    }
}
