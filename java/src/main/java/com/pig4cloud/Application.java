package com.pig4cloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.pig4cloud.dao") // 扫描Mapper接口所在的包
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
