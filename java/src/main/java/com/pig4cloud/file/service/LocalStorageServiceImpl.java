package com.pig4cloud.file.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘存储（默认，app.storage.type=local）：文件落在工作目录uploadFile/下，
 * 单机部署简单可靠，多实例/容器部署请切ftp或s3共享存储
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageServiceImpl implements StorageService {

    private static final String BASE_DIR = "uploadFile";

    @Override
    public String upload(String path, MultipartFile file) throws Exception {
        Path target = Paths.get(BASE_DIR).toAbsolutePath().normalize().resolve(path).normalize();
        if (!target.startsWith(Paths.get(BASE_DIR).toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("非法存储路径：" + path);
        }
        File dir = target.getParent().toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("创建目录失败：" + dir.getAbsolutePath());
        }
        file.transferTo(target.toFile());
        return "/" + path;
    }

    @Override
    public String uploadFile(String path, java.io.InputStream in, long size) throws Exception {
        Path target = Paths.get(BASE_DIR).toAbsolutePath().normalize().resolve(path).normalize();
        if (!target.startsWith(Paths.get(BASE_DIR).toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("非法存储路径：" + path);
        }
        File dir = target.getParent().toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("创建目录失败：" + dir.getAbsolutePath());
        }
        java.nio.file.Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return "/" + path;
    }

    @Override
    public InputStream download(String path) throws Exception {
        Path base = Paths.get(BASE_DIR).toAbsolutePath().normalize();
        Path target = base.resolve(path).normalize();
        // 防止路径穿越下载目录之外的文件
        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("非法下载路径：" + path);
        }
        return new FileInputStream(target.toFile());
    }
}
