package com.pig4cloud.common.result;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一响应体：{code, message, data}，业务成功code=200，业务失败code=-200，
 * 认证失败code=401（前端据此走刷新token流程）
 */
@Getter
@Setter
public class R<T> {

    public static final int SUCCESS = 200;
    public static final int FAIL = -200;
    public static final int UNAUTHORIZED = 401;

    private int code;
    private String message;
    private T data;

    public R() {
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok(T data) {
        return ok("请求成功", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(SUCCESS, message, data);
    }

    public static R<Void> ok() {
        return ok("请求成功", null);
    }

    public static <T> R<T> fail(String message) {
        return fail(FAIL, message);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }
}
