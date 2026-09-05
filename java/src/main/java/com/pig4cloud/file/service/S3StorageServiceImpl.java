package com.pig4cloud.file.service;

import com.pig4cloud.file.config.S3Properties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * S3兼容对象存储（app.storage.type=s3）：MinIO/阿里云OSS S3端点/腾讯COS等均可。
 * 客户端启动时初始化一次，上传/下载走标准S3协议
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageServiceImpl implements StorageService {

    private final S3Properties properties;
    private volatile MinioClient minioClient;

    public S3StorageServiceImpl(S3Properties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        log.info("S3对象存储已启用：endpoint={} bucket={}", properties.getEndpoint(), properties.getBucket());
    }

    @Override
    public String upload(String path, MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(stripLeadingSlash(path))
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .build());
        }
        return "/" + stripLeadingSlash(path);
    }

    @Override
    public String uploadFile(String path, InputStream in, long size) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(stripLeadingSlash(path))
                .stream(in, size, -1)
                .build());
        return "/" + stripLeadingSlash(path);
    }

    @Override
    public String presignedGetUrl(String path, int expireSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.getBucket())
                    .object(stripLeadingSlash(path))
                    .expiry(Math.max(1, expireSeconds))
                    .build());
        } catch (Exception ex) {
            log.warn("预签名下载URL生成失败: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    public String presignedPutUrl(String path, int expireSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(properties.getBucket())
                    .object(stripLeadingSlash(path))
                    .expiry(Math.max(1, expireSeconds))
                    .build());
        } catch (Exception ex) {
            log.warn("预签名上传URL生成失败: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    public InputStream download(String path) throws Exception {
        GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(stripLeadingSlash(path))
                .build());
        return response;
    }

    private String stripLeadingSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
