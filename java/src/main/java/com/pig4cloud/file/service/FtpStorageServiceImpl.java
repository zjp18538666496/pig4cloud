package com.pig4cloud.file.service;

import org.apache.commons.net.ftp.FTPClient;
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
    public String uploadFile(String path, InputStream in, long size) throws Exception {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpService.configureFTPClient(ftpClient);
            String dir = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
            if (!dir.isBlank() && !ftpClient.changeWorkingDirectory(dir) && !ftpClient.makeDirectory(dir)) {
                throw new IllegalStateException("FTP创建目录失败：" + dir);
            }
            String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            if (!ftpClient.storeFile(fileName, in)) {
                throw new IllegalStateException("FTP上传失败：" + ftpClient.getReplyCode());
            }
            return path;
        } finally {
            try { ftpService.disconnectFTPClient(ftpClient); } catch (Exception ignored) { }
            in.close();
        }
    }

    @Override
    public void deleteFile(String path) throws Exception {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpService.configureFTPClient(ftpClient);
            if (!ftpClient.deleteFile(path)) {
                throw new IllegalStateException("FTP删除失败：" + ftpClient.getReplyCode());
            }
        } finally {
            try { ftpService.disconnectFTPClient(ftpClient); } catch (Exception ignored) { }
        }
    }

    @Override
    public InputStream download(String path) throws Exception {
        return ftpService.downloadFile(path);
    }
}
