package com.xuqi.aicodehelper.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * 密码加密工具（PBKDF2WithHmacSHA256）
 * <p>
 * 为什么不用 MD5/SHA 直存：它们速度快，彩虹表可暴力破解。
 * PBKDF2 通过「随机盐 + 大量迭代」把单次校验成本抬高，是 NIST 推荐的口令存储方案，
 * 且 JDK 原生支持，不需要引入额外依赖。
 * <p>
 * 存储格式：Base64(salt)$Base64(hash)
 */
public final class PasswordHasher {

    /** 密钥派生算法 */
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /** 迭代次数（OWASP 2023 建议 >= 310000） */
    private static final int ITERATIONS = 310_000;

    /** 派生密钥长度（bit） */
    private static final int KEY_LENGTH = 256;

    /** 随机盐长度（byte） */
    private static final int SALT_LENGTH = 16;

    /** 线程安全的随机数发生器 */
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /**
     * 生成密码密文
     *
     * @param rawPassword 明文密码
     * @return 形如 salt$hash 的密文
     */
    public static String hash(String rawPassword) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt);
        return Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 校验明文密码是否匹配密文
     *
     * @param rawPassword 明文密码
     * @param stored     数据库里存的 salt$hash
     * @return true 匹配
     */
    public static boolean matches(String rawPassword, String stored) {
        if (rawPassword == null || stored == null || !stored.contains("$")) {
            return false;
        }
        String[] parts = stored.split("\\$", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
        byte[] actualHash = pbkdf2(rawPassword.toCharArray(), salt);
        // 定长比较，防止计时攻击
        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    /**
     * 执行 PBKDF2 密钥派生
     *
     * @param chars 明文密码字符数组
     * @param salt  随机盐
     * @return 派生的哈希字节
     */
    private static byte[] pbkdf2(char[] chars, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return hash;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("密码加密算法不可用：" + ALGORITHM, e);
        }
    }
}
