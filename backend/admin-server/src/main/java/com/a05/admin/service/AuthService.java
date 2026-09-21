package com.a05.admin.service;

import com.a05.admin.controller.dto.LoginRequest;
import com.a05.admin.controller.vo.LoginVO;

public interface AuthService {
    
    /**
     * 后台管理员/用户登录
     * @param request 包含手机号和密码
     * @return 登录成功后的视图对象（包含Token）
     */
    LoginVO login(LoginRequest request);

    /**
     * 退出登录（清理 Redis 中当前用户 Token）
     * @param userId 当前登录用户ID
     */
    void logout(String userId);
}
