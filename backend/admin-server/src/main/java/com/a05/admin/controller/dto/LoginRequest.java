package com.a05.admin.controller.dto;

/**
 * 专门用于接收前端登录请求的参数对象
 */
public class LoginRequest {
    private String phone;
    private String password;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
