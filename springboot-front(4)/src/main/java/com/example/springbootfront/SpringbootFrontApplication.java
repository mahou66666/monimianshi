package com.example.springbootfront;

import org.mybatis.spring.annotation.MapperScan; // 记得按 Alt+Enter 导包！
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// 👇 就是加上这一行！注意括号里的包名要指向你的 dao 文件夹
@MapperScan("com.example.springbootfront.dao")
public class SpringbootFrontApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootFrontApplication.class, args);
    }
}