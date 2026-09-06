package com.xuqi.aicodehelper.controller;

import com.xuqi.aicodehelper.common.Result;
import com.xuqi.aicodehelper.common.UserContext;
import com.xuqi.aicodehelper.dto.AuthRequest;
import com.xuqi.aicodehelper.dto.LoginResponse;
import com.xuqi.aicodehelper.entity.User;
import com.xuqi.aicodehelper.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册、登录、获取当前用户信息
 * <p>
 * 这些接口在 WebConfig 中被排除鉴权，可匿名访问。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param request 用户名 + 密码
     * @return 注册成功返回 token（免二次登录）
     */
//    @PostMapping("/register")
//    public Result<LoginResponse> register(@RequestBody AuthRequest request) {
//        return Result.ok(userService.register(request.getUsername(), request.getPassword()));
//    }

    /**
     * 用户登录
     *
     * @param request 用户名 + 密码
     * @return token 与用户信息
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody AuthRequest request) {
        return Result.ok(userService.login(request.getUsername(), request.getPassword()));
    }

    /**
     * 获取当前登录用户信息（前端启动时调用，用于判断 token 是否有效）
     *
     * @return 用户信息
     */
    @GetMapping("/me")
    public Result<LoginResponse> me() {
        Long userId = UserContext.getUserId();
        User user = userService.getByUserId(userId);

        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        // 这里不再签发新 token，前端沿用本地保存的即可
        return Result.ok(response);
    }
}
