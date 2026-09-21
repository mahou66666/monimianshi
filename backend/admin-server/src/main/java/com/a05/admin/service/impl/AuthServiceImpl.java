package com.a05.admin.service.impl;

import com.a05.admin.config.JwtProperties;
import com.a05.admin.controller.dto.LoginRequest;
import com.a05.admin.controller.vo.LoginVO;
import com.a05.admin.entity.Permission;
import com.a05.admin.entity.UserInfo;
import com.a05.admin.mapper.PermissionMapper;
import com.a05.admin.mapper.UserInfoMapper;
import com.a05.admin.mapper.UserPermissionRelMapper;
import com.a05.admin.service.AuthService;
import com.a05.admin.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserPermissionRelMapper userPermissionRelMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public LoginVO login(LoginRequest request) {
        UserInfo user = userInfoMapper.selectByPhone(request.getPhone());
        if (user == null) {
            throw new RuntimeException("该手机号尚未注册");
        }

        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误，请重试");
        }

        List<String> permissionCodes = loadPermissionCodes(user.getId());
        if (permissionCodes.isEmpty()) {
            throw new RuntimeException("当前账号没有后台访问权限，请联系管理员");
        }

        Map<String, Object> claims = new HashMap<>();
        String token = jwtUtil.generateToken(String.valueOf(user.getId()), claims);

        String redisKey = tokenKey(String.valueOf(user.getId()));
        long ttlSeconds = jwtProperties.getExpireSeconds() > 0 ? jwtProperties.getExpireSeconds() : 7200L;
        stringRedisTemplate.opsForValue().set(redisKey, token, ttlSeconds, TimeUnit.SECONDS);

        return new LoginVO(token, user.getId(), user.getUserName(), permissionCodes);
    }

    @Override
    public void logout(String userId) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        stringRedisTemplate.delete(tokenKey(userId));
    }

    private String tokenKey(String userId) {
        return "admin:auth:token:" + userId;
    }

    private List<String> loadPermissionCodes(Long userId) {
        List<Long> permIds = userPermissionRelMapper.listPermIdsByUserId(userId);
        if (permIds == null || permIds.isEmpty()) {
            return List.of();
        }

        List<Permission> permissions = permissionMapper.selectBatchIds(permIds);
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }

        return permissions.stream()
                .filter(permission -> permission.getStatus() == null || permission.getStatus() == 1)
                .map(Permission::getPermCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
