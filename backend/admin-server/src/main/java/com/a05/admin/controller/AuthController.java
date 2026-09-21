package com.a05.admin.controller;

import com.a05.admin.common.Result;
import com.a05.admin.controller.dto.LoginRequest;
import com.a05.admin.controller.vo.LoginVO;
import com.a05.admin.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest request) {
        // 基础参数判空校验
        if (request.getPhone() == null || request.getPhone().isEmpty() || 
            request.getPassword() == null || request.getPassword().isEmpty()) {
            return Result.error(400, "手机号和密码不能为空");
        }
        
        try {
            // 调用业务层执行核心登录逻辑
            LoginVO loginVO = authService.login(request);
            // 登录成功，返回数据给前端
            return Result.success(loginVO);
        } catch (RuntimeException e) {
            // 捕获我们在 Service 层主动抛出的业务异常（比如“密码错误”、“账号禁用”）
            return Result.error(401, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "系统抛错：" + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        authService.logout(userId == null ? null : String.valueOf(userId));
        return Result.success(null);
    }
}
