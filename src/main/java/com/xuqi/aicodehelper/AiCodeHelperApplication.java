package com.xuqi.aicodehelper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 * <p>
 * @MapperScan 扫描 MyBatis 的 Mapper 接口，生成代理实现注入 Spring 容器。
 */
@MapperScan("com.xuqi.aicodehelper.mapper")
@SpringBootApplication
public class AiCodeHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeHelperApplication.class, args);
    }

}
