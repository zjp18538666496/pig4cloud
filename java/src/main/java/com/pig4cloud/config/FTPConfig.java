package com.pig4cloud.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "ftp")
public class FTPConfig {
    private String server;
    private int port;
    private String user;
    private String password;
}
