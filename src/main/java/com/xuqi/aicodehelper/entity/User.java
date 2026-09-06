package com.xuqi.aicodehelper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（对应表 t_user）
 * <p>
 * 用户名全局唯一，密码只存 PBKDF2 密文，永不回传给前端。
 */
@Data
@TableName("t_user")
public class User {

    /** 用户ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（唯一） */
    private String username;

    /** 密码密文：Base64(salt)$Base64(hash) */
    private String password;

    /** 注册时间 */
    private LocalDateTime createTime;
}
