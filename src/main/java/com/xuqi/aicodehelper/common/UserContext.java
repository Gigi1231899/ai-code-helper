package com.xuqi.aicodehelper.common;

/**
 * 当前登录用户的上下文（基于 ThreadLocal）
 * <p>
 * 流程：AuthInterceptor 解析 JWT 成功后把 userId 放进这里，
 * Controller / Service 在本次请求线程内随时可取，无需层层传参。
 * 请求结束后必须在 finally 中 clear()，避免线程复用导致用户信息串号。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    /** 存入当前登录用户ID */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    /**
     * 取出当前登录用户ID
     *
     * @throws BusinessException 未登录时抛出，由全局异常处理器转成 401
     */
    public static Long getUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return userId;
    }

    /** 清理线程变量，防止内存泄漏与用户信息串号 */
    public static void clear() {
        USER_ID.remove();
    }
}
