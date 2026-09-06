package com.xuqi.aicodehelper.common;

import lombok.Getter;

/**
 * 业务异常：用于参数校验失败、权限不足等可预期的错误
 * <p>
 * 抛出后由 GlobalExceptionHandler 统一转成 Result{code, message} 返回给前端，
 * 避免在 Controller 里写大量 try-catch。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务状态码，默认 400 */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
