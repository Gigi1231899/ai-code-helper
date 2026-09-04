package com.xuqi.aicodehelper.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuqi.aicodehelper.common.Result;
import com.xuqi.aicodehelper.common.UserContext;
import com.xuqi.aicodehelper.util.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 登录鉴权拦截器
 * <p>
 * 除注册/登录外的所有接口都必须携带有效 JWT：
 * 1. 从 Authorization: Bearer xxx 请求头取 token（也兼容 ?token=xxx，方便某些无法自定义头的场景）
 * 2. 解析出 userId 放入 UserContext，供后续 Controller/Service 使用
 * 3. 请求结束后必须清理 ThreadLocal，否则线程复用会导致用户串号
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** token 请求头前缀 */
    private static final String BEARER = "Bearer ";

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        // 跨域预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null) {
            writeUnauthorized(response, "请先登录哦~");
            return false;
        }

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        // 把用户身份绑定到当前请求线程
        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 必须清理，防止线程池复用导致用户信息泄漏到下一个请求
        UserContext.clear();
    }

    /**
     * 从请求中提取 token：优先请求头，其次 query 参数
     *
     * @param request 当前请求
     * @return token；不存在返回 null
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            return header.substring(BEARER.length()).trim();
        }
        String param = request.getParameter("token");
        return (param == null || param.isBlank()) ? null : param.trim();
    }

    /**
     * 写入 401 响应
     *
     * @param response 响应对象
     * @param message  提示文案
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(new ObjectMapper().writeValueAsString(Result.fail(401, message)));
    }
}
