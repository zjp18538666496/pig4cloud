package com.pig4cloud.file.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * FTP存储（app.storage.type=ftp）：委托既有FtpService，保持与历史头像数据兼容
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "ftp")
public class FtpStorageServiceImpl implements StorageService {

    private final FtpService ftpService;

    public FtpStorageServiceImpl(FtpService ftpService) {
        this.ftpService = ftpService;
    }

    @Override
    public String upload(String path, MultipartFile file) throws Exception {
        ftpService.uploadFile(path, file);
        return path;
    }

    @Override
    public InputStream download(String path) throws Exception {
        return ftpService.downloadFile(path);
    }
}
