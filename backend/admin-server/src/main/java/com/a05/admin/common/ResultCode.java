package com.a05.admin.common;

/**
 * 业务状态码枚举
 *
 * <p>规范：
 * <ul>
 *   <li>200xx  成功类</li>
 *   <li>400xx  客户端错误（参数、权限等）</li>
 *   <li>500xx  服务端错误</li>
 * </ul>
 */
public enum ResultCode {

    // ---------- 成功 ----------
    SUCCESS(200, "操作成功"),

    // ---------- 客户端错误 ----------
    BAD_REQUEST(40000, "请求参数有误"),
    UNAUTHORIZED(40001, "未登录或 Token 已过期"),
    FORBIDDEN(40003, "无权限访问该资源"),
    NOT_FOUND(40004, "请求的资源不存在"),
    VALIDATION_FAILED(40010, "参数校验失败"),

    // ---------- 服务端错误 ----------
    ERROR(50000, "服务器内部错误"),
    SERVICE_UNAVAILABLE(50003, "服务暂不可用，请稍后重试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
