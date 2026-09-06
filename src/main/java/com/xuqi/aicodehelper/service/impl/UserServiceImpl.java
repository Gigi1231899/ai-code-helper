package com.xuqi.aicodehelper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// 注意：MyBatis-Plus 3.5.17 起，ServiceImpl 从 extension 包迁移到了 spring 包
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xuqi.aicodehelper.common.BusinessException;
import com.xuqi.aicodehelper.dto.LoginResponse;
import com.xuqi.aicodehelper.entity.User;
import com.xuqi.aicodehelper.mapper.UserMapper;
import com.xuqi.aicodehelper.service.UserService;
import com.xuqi.aicodehelper.util.JwtUtil;
import com.xuqi.aicodehelper.util.PasswordHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;

/**
 * 用户服务实现
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /** JWT 工具，用于登录成功后签发令牌 */
    @Resource
    private JwtUtil jwtUtil;

    @Override
    public LoginResponse register(String username, String password) {
        // ① 校验用户名与密码强度
        validateUsername(username);
        validatePasswordStrength(password);

        // ② 用户名唯一性校验（数据库也有唯一索引兜底，这里先查一次给出友好提示）
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (count != null && count > 0) {
            throw new BusinessException("用户名已被占用，换一个试试~");
        }

        // ③ 落库：只存密文，绝不存明文
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordHasher.hash(password));
        user.setCreateTime(LocalDateTime.now());
        baseMapper.insert(user);

        log.info("新用户注册成功：id={}, username={}", user.getId(), username);

        // ④ 注册成功直接签发 token，前端无需再登录一次
        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException("用户名和密码不能为空");
        }

        User user = baseMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        // 用户不存在与密码错误返回同一提示，避免被爆破出哪些用户名存在
        if (user == null || !PasswordHasher.matches(password, user.getPassword())) {
            throw new BusinessException(401, "用户名或密码不正确");
        }

        log.info("用户登录成功：id={}, username={}", user.getId(), username);
        return buildLoginResponse(user);
    }

    @Override
    public User getByUserId(Long userId) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或已被删除");
        }
        return user;
    }

    /**
     * 构造返回给前端的登录信息
     *
     * @param user 用户实体
     * @return 登录响应（含 JWT）
     */
    private LoginResponse buildLoginResponse(User user) {
        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setToken(jwtUtil.generateToken(user.getId(), user.getUsername()));
        return response;
    }

    /**
     * 用户名规则：4~20 位，字母/数字/下划线，且必须以字母开头
     *
     * @param username 用户名
     */
    private void validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("用户名不能为空");
        }
        if (!username.matches("^[a-zA-Z][a-zA-Z0-9_]{3,19}$")) {
            throw new BusinessException("用户名需为 4~20 位字母/数字/下划线，且以字母开头");
        }
    }

    /**
     * 密码强度规则（需求中的"高强度密码"）：
     * 长度 8~64，必须同时包含大写字母、小写字母和数字
     *
     * @param password 明文密码
     */
    private void validatePasswordStrength(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException("密码不能为空");
        }
        if (password.length() < 8 || password.length() > 64) {
            throw new BusinessException("密码长度需为 8~64 位");
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasUpper || !hasLower || !hasDigit) {
            throw new BusinessException("密码需同时包含大写字母、小写字母和数字");
        }
    }
}
