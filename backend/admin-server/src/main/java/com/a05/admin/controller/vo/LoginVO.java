package com.a05.admin.controller.vo;

import java.util.List;

/**
 * 专门用于将登录成功后的数据返回给前端的值对象
 */
public class LoginVO {
    private String token;
    private Long userId;
    private String userName;
    private List<String> permissionCodes;

    public LoginVO(String token, Long userId, String userName, List<String> permissionCodes) {
        this.token = token;
        this.userId = userId;
        this.userName = userName;
        this.permissionCodes = permissionCodes;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }
}
