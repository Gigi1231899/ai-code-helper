package com.xuqi.aicodehelper.service;

// 注意：MyBatis-Plus 3.5.17 起，IService 从 extension 包迁移到了 spring 包
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuqi.aicodehelper.dto.LoginResponse;
import com.xuqi.aicodehelper.entity.User;

/**
 * 用户服务：注册、登录、查询
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册（用户名唯一 + 高强度密码校验）
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 注册成功后的登录信息（含 token，注册完直接免登录）
     */
    LoginResponse register(String username, String password);

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录信息
     */
    LoginResponse login(String username, String password);

    /**
     * 查询用户
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    User getByUserId(Long userId);
}
