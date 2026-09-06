package com.xuqi.aicodehelper.dto;

import lombok.Data;

/**
 * 登录/注册成功后返回给前端的数据
 */
@Data
public class LoginResponse {

    /** JWT 令牌，前端需保存并在后续请求中带上 */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;
}
