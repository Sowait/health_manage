package com.health.manage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.health.manage.mapper")
public class HealthManageApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthManageApplication.class, args);
    }
}
