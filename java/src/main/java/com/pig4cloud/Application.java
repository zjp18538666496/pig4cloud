package com.pig4cloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan({
        "com.pig4cloud.auth.mapper",
        "com.pig4cloud.user.mapper",
        "com.pig4cloud.role.mapper",
        "com.pig4cloud.menu.mapper"
}) // 扫描各业务模块Mapper接口
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
