package com.pig4cloud.file.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FTP连接配置，账号密码通过application-local.yaml或环境变量注入
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ftp")
public class FtpProperties {

    private String server;
    private int port;
    private String user;
    private String password;
}
