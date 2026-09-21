package com.a05.admin;

import com.a05.admin.config.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * A05 管理后台 - 启动类
 */
@SpringBootApplication
@MapperScan("com.a05.admin.mapper")
@EnableConfigurationProperties(JwtProperties.class)
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}

