package com.pig4cloud.file.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3兼容对象存储配置（MinIO/阿里云OSS S3兼容端点/腾讯云COS等），
 * 账号密码通过application-local.yaml或环境变量注入
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "s3")
public class S3Properties {

    /**
     * S3兼容端点，如http://127.0.0.1:9000
     */
    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucket;

    /**
     * MinIO等自建存储用路径风格（true）；公有云S3端点一般为false
     */
    private boolean pathStyleAccess = true;
}
