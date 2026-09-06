package com.xuqi.aicodehelper.dto;

import lombok.Data;

/**
 * 注册/登录请求体（两个接口字段相同，复用一个 DTO）
 */
@Data
public class AuthRequest {

    /** 用户名 */
    private String username;

    /** 明文密码（走 HTTPS 传输，服务端用 PBKDF2 加盐存储） */
    private String password;
}
