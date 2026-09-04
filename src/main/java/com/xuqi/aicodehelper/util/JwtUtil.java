package com.xuqi.aicodehelper.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：负责签发与校验令牌
 * <p>
 * 密钥来自配置（本地读 application-local.yml，生产从 JWT_SECRET 环境变量注入），
 * HS256 算法要求密钥长度不少于 32 字节，否则启动即报错。
 */
@Slf4j
@Component
public class JwtUtil {

    /** JWT 签名密钥（明文字符串，内部转成 HMAC key） */
    @Value("${jwt.secret}")
    private String secret;

    /** 有效期（小时） */
    @Value("${jwt.expiration-hours:168}")
    private Long expirationHours;

    /** 缓存签名 key，避免每次重复构造 */
    private SecretKey cachedKey;

    /**
     * 获取 HMAC 签名密钥
     *
     * @return SecretKey
     */
    private SecretKey getKey() {
        if (cachedKey == null) {
            cachedKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        return cachedKey;
    }

    /**
     * 签发 token
     *
     * @param userId   用户ID（放在 subject 中）
     * @param username 用户名（放在自定义 claim 中，便于前端展示）
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationHours * 60 * 60 * 1000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getKey())
                .compact();
    }

    /**
     * 校验并解析 token
     *
     * @param token JWT 字符串
     * @return Claims 载荷
     * @throws JwtException token 非法或已过期
     */
    public Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token);
    }

    /**
     * 从 token 中取出用户ID
     *
     * @param token JWT 字符串
     * @return 用户ID；解析失败返回 null（由调用方决定是否拒绝）
     */
    public Long getUserId(String token) {
        try {
            String subject = parseToken(token).getPayload().getSubject();
            return Long.valueOf(subject);
        } catch (JwtException | NumberFormatException e) {
            log.debug("JWT 解析失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 token 中取出用户名
     *
     * @param token JWT 字符串
     * @return 用户名；解析失败返回 null
     */
    public String getUsername(String token) {
        try {
            return parseToken(token).getPayload().get("username", String.class);
        } catch (JwtException e) {
            log.debug("JWT 解析失败：{}", e.getMessage());
            return null;
        }
    }
}
