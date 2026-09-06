package com.xuqi.aicodehelper.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 全局统一响应体
 * <p>
 * 所有 REST 接口都返回 { code, message, data } 结构，前端 axios 拦截器统一按 code 判断成败。
 * 约定：code = 200 表示成功，其余为失败（401 未登录、400 参数/业务错误、500 服务器错误）。
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    /** 状态码，200 成功 */
    private Integer code;

    /** 提示信息，失败时展示给用户 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 成功：带数据 */
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    /** 成功：不带数据 */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /** 失败：默认 400 */
    public static <T> Result<T> fail(String message) {
        return fail(400, message);
    }

    /** 失败：自定义状态码（如 401 未登录） */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
