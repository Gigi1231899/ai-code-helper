package com.xuqi.aicodehelper.config;

import com.xuqi.aicodehelper.interceptor.AuthInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置：注册登录拦截器 + 跨域
 * <p>
 * 这里配置的是 Servlet 栈（spring-boot-starter-webmvc），
 * 与返回 Flux 的 SSE 接口可以共存。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private AuthInterceptor authInterceptor;

    /**
     * 注册拦截器，放行注册/登录接口
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // 注意：路径是相对 context-path(/api) 的
                .excludePathPatterns(
                        "/auth/register",
                        "/auth/login",
                        "/error"
                );
    }

    /**
     * 前后端分离跨域配置，开发时 Vite(5173) 访问后端(8090)
     *
     * @param registry 跨域注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
