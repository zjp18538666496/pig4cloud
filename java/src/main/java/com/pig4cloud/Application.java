package com.pig4cloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
@MapperScan({
        "com.pig4cloud.auth.mapper",
        "com.pig4cloud.user.mapper",
        "com.pig4cloud.role.mapper",
        "com.pig4cloud.menu.mapper",
        "com.pig4cloud.tenant.mapper",
        "com.pig4cloud.dept.mapper",
        "com.pig4cloud.notice.mapper",
        "com.pig4cloud.config.mapper",
        "com.pig4cloud.dict.mapper",
        "com.pig4cloud.post.mapper",
        "com.pig4cloud.message.mapper",
        "com.pig4cloud.apikey.mapper",
        "com.pig4cloud.recycle.mapper",
        "com.pig4cloud.job.mapper",
        "com.pig4cloud.notify.mapper",
        "com.pig4cloud.export.mapper"
}) // 扫描各业务模块Mapper接口
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
