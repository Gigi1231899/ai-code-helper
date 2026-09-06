package com.xuqi.aicodehelper.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 把各类异常统一转换成 Result 结构，保证前端总能拿到可读的错误提示。
 * 注意：SSE 流式接口在响应开始后抛出的异常无法被这里捕获（响应头已发出），
 * 因此聊天接口内部的异常需要在 Flux 的 onErrorResume 中单独处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：直接把 message 返回给用户 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数绑定/类型错误：Spring 抛出的 IllegalArgumentException 等 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误：{}", e.getMessage());
        return Result.fail(400, "参数错误：" + e.getMessage());
    }

    /** 兜底：未知异常记日志，返回统一文案，避免把堆栈暴露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("服务器异常", e);
        return Result.fail(500, "服务器开小差了，请稍后再试~");
    }
}
