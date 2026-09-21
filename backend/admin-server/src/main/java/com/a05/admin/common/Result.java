package com.a05.admin.common;

/**
 * 全局统一返回结果类
 *
 * <p>使用示例：
 * <pre>
 *   // 成功，无数据
 *   return Result.success();
 *   // 成功，携带数据
 *   return Result.success(data);
 *   // 失败，自定义消息
 *   return Result.error("参数校验失败");
 *   // 失败，自定义码和消息
 *   return Result.error(40001, "用户不存在");
 * </pre>
 */
public class Result<T> {

    /** 业务状态码：200 表示成功 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    // ----------------------------------------------------------------
    // 构造（私有，强制使用静态工厂方法）
    // ----------------------------------------------------------------

    private Result() {}

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ----------------------------------------------------------------
    // 成功静态方法
    // ----------------------------------------------------------------

    /** 操作成功，无返回数据 */
    public static Result<Void> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 操作成功，携带业务数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 操作成功，自定义提示信息，携带业务数据 */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    // ----------------------------------------------------------------
    // 失败静态方法
    // ----------------------------------------------------------------

    /** 操作失败，使用默认状态码，自定义消息 */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    /** 操作失败，使用预定义 ResultCode 枚举 */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 操作失败，完全自定义状态码和消息 */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    // ----------------------------------------------------------------
    // Getter / Setter（保留给 Jackson 序列化）
    // ----------------------------------------------------------------

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Result{code=" + code + ", message='" + message + "', data=" + data + "}";
    }
}
