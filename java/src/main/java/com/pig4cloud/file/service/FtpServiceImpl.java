package com.pig4cloud.file.service;

import com.pig4cloud.file.config.FtpProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class FtpServiceImpl implements FtpService {

    private final FtpProperties ftpProperties;

    @Override
    public void uploadFile(String remotePath, MultipartFile file) throws IOException {
        FTPClient ftpClient = new FTPClient();
        try {
            configureFTPClient(ftpClient);
            uploadFile(remotePath, file, ftpClient);
        } finally {
            disconnectFTPClient(ftpClient);
        }
    }

    @Override
    public void uploadFile(String remotePath, MultipartFile file, FTPClient ftpClient) throws IOException {
        try {
            ensureDirectoryExists(ftpClient, remotePath);
            ftpClient.changeWorkingDirectory(remotePath);

            // 文件名转换为ISO-8859-1编码以支持中文
            String fileName = new String(file.getOriginalFilename().getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            try (InputStream inputStream = file.getInputStream()) {
                if (!ftpClient.storeFile(fileName, inputStream)) {
                    throw new IOException("Failed to upload file " + file.getOriginalFilename());
                }
            }
        } catch (IOException ex) {
            throw new IOException("Failed to upload file " + file.getOriginalFilename(), ex);
        }
    }

    @Override
    public InputStream downloadFile(String remoteFilePath) throws IOException {
        FTPClient ftpClient = new FTPClient();
        PipedInputStream pipedInputStream = new PipedInputStream();
        PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);

        // 在后台线程写管道流，调用方从输入流读取，实现流式下载
        new Thread(() -> {
            try {
                configureFTPClient(ftpClient);
                downloadFile(remoteFilePath, pipedOutputStream, ftpClient);
            } catch (IOException ignored) {
            } finally {
                try {
                    pipedOutputStream.close();
                } catch (IOException ignored) {
                }
                disconnectFTPClient(ftpClient);
            }
        }).start();

        return pipedInputStream;
    }

    @Override
    public void downloadFile(String remoteFilePath, OutputStream outputStream, FTPClient ftpClient) throws IOException {
        if (remoteFilePath == null || remoteFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Remote file path cannot be null or empty.");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream cannot be null.");
        }
        if (ftpClient == null) {
            throw new IllegalArgumentException("FTPClient cannot be null.");
        }
        try (InputStream inputStream = ftpClient.retrieveFileStream(remoteFilePath)) {
            if (inputStream == null) {
                throw new IOException("Failed to retrieve file stream: " + ftpClient.getReplyString());
            }
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            if (!ftpClient.completePendingCommand()) {
                throw new IOException("Failed to complete pending command: " + ftpClient.getReplyString());
            }
        }
    }

    @Override
    public void configureFTPClient(FTPClient ftpClient) throws IOException {
        ftpClient.connect(ftpProperties.getServer(), ftpProperties.getPort());
        ftpClient.login(ftpProperties.getUser(), ftpProperties.getPassword());
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.setBufferSize(1024 * 1024 * 10);
        ftpClient.setControlEncoding(StandardCharsets.UTF_8.name());
    }

    @Override
    public void disconnectFTPClient(FTPClient ftpClient) {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException ex) {
                System.err.println("Error disconnecting from FTP server: " + ex.getMessage());
            }
        }
    }

    /**
     * 确保FTP服务器上的目录路径存在，不存在则逐级创建
     */
    private void ensureDirectoryExists(FTPClient ftpClient, String dirPath) throws IOException {
        String[] directories = dirPath.split("/");
        StringBuilder currentPath = new StringBuilder();
        for (String dir : directories) {
            if (!dir.isEmpty()) {
                currentPath.append('/').append(dir);
                if (!ftpClient.changeWorkingDirectory(currentPath.toString())) {
                    ftpClient.makeDirectory(currentPath.toString());
                    ftpClient.changeWorkingDirectory(currentPath.toString());
                }
            }
        }
    }
}
