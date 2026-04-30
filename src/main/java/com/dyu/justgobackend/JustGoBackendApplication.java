package com.dyu.justgobackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.dyu.justgobackend.mapper")
public class JustGoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(JustGoBackendApplication.class, args);
    }
}
