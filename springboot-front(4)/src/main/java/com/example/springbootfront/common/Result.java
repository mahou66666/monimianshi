package com.example.springbootfront.common;

/**
 * 全局统一返回结果类
 */
public class Result<T> {

    private Integer code; // 状态码：200表示成功，500表示失败等
    private String msg;   // 提示信息：用于前端弹窗展示
    private T data;       // 实际的数据：泛型T代表可以装入任何类型的数据

    // 空构造函数
    public Result() {
    }

    // 全参构造函数
    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // --- 快捷工厂方法 ---

    // 1. 成功，但不返回具体数据
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    // 2. 成功，并且附带数据（最常用）
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    // 3. 失败，返回通用错误提示
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    // 4. 失败，自定义错误码和提示
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    // --- Getter 和 Setter 方法（必须要有，否则前端拿不到数据） ---
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}