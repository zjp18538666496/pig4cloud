package com.pig4cloud.service.impl;

import com.pig4cloud.config.FTPConfig;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
// import java.util.logging.Level;
// import java.util.logging.Logger;

@Service
public class FTPServiceImpl {
    @Autowired
    private FTPConfig ftpConfig;

    // private static final Logger LOGGER = Logger.getLogger(FTPServiceImpl.class.getName());

    /**
     * 上传文件到FTP服务器指定路径
     *
     * @param remotePath FTP服务器上的目录路径
     * @param file       要上传的文件
     * @throws IOException 如果上传失败则抛出异常
     */
    public void uploadFile(String remotePath, MultipartFile file) throws IOException {
        FTPClient ftpClient = new FTPClient();
        try {
            // 配置FTP客户端
            configureFTPClient(ftpClient);
            uploadFile(remotePath, file, ftpClient);
        } finally {
            // 断开FTP客户端连接
            disconnectFTPClient(ftpClient);
        }
    }

    /**
     * 上传文件到FTP服务器指定路径
     *
     * @param remotePath FTP服务器上的目录路径
     * @param file       要上传的文件
     * @throws IOException 如果上传失败则抛出异常
     */
    public void uploadFile(String remotePath, MultipartFile file, FTPClient ftpClient) throws IOException {
        try {
            // 确保目录存在
            ensureDirectoryExists(ftpClient, remotePath);
            // 切换到目标目录
            ftpClient.changeWorkingDirectory(remotePath);

            // 将文件名转换为ISO-8859-1编码以支持中文
            String fileName = new String(file.getOriginalFilename().getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            try (InputStream inputStream = file.getInputStream()) {
                // 上传文件
                if (ftpClient.storeFile(fileName, inputStream)) {
                    // LOGGER.info("File " + file.getOriginalFilename() + " has been uploaded successfully.");
                    System.out.println("File " + file.getOriginalFilename() + " has been uploaded successfully.");
                } else {
                    throw new IOException("Failed to upload file " + file.getOriginalFilename());
                }
            }
        } catch (IOException ex) {
            throw new IOException("Failed to upload file " + file.getOriginalFilename());
        }
    }

    // 在 FTPServiceImpl 类中添加以下方法

    public InputStream downloadFile(String remoteFilePath) throws IOException {
        FTPClient ftpClient = new FTPClient();
        try {
            configureFTPClient(ftpClient);
           return downloadFile(remoteFilePath, ftpClient);
        } catch (IOException ex) {
            throw new IOException("Failed to download file: " + remoteFilePath, ex);
        }
        finally {
            disconnectFTPClient(ftpClient);
        }
    }

    /**
     * 从FTP服务器下载文件
     *
     * @param remoteFilePath FTP服务器上的文件路径
     * @return 文件输入流
     * @throws IOException 如果下载失败则抛出异常
     */
    public InputStream downloadFile(String remoteFilePath, FTPClient ftpClient) throws IOException {
        try {
            return ftpClient.retrieveFileStream(remoteFilePath);
        } catch (IOException ex) {
            throw new IOException("Failed to download file: " + remoteFilePath, ex);
        }
    }


    /**
     * 配置FTP客户端
     *
     * @param ftpClient 要配置的FTP客户端
     * @throws IOException 如果配置失败则抛出异常
     */
    public void configureFTPClient(FTPClient ftpClient) throws IOException {
        ftpClient.connect(ftpConfig.getServer(), ftpConfig.getPort());
        ftpClient.login(ftpConfig.getUser(), ftpConfig.getPassword());
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.setControlEncoding(StandardCharsets.UTF_8.name());
    }

    /**
     * 断开FTP客户端连接
     *
     * @param ftpClient 要断开的FTP客户端
     */
    public void disconnectFTPClient(FTPClient ftpClient) {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException ex) {
                // LOGGER.log(Level.WARNING, "Error disconnecting from FTP server", ex);
                System.err.println("Error disconnecting from FTP server: " + ex.getMessage());
            }
        }
    }

    /**
     * 确保FTP服务器上的目录路径存在，不存在则创建
     *
     * @param ftpClient FTP客户端
     * @param dirPath   目录路径
     * @throws IOException 如果操作失败则抛出异常
     */
    private void ensureDirectoryExists(FTPClient ftpClient, String dirPath) throws IOException {
        String[] directories = dirPath.split("/");
        String currentPath = "";
        for (String dir : directories) {
            if (!dir.isEmpty()) {
                currentPath += "/" + dir;
                if (!ftpClient.changeWorkingDirectory(currentPath)) {
                    ftpClient.makeDirectory(currentPath);
                    ftpClient.changeWorkingDirectory(currentPath); // 确保目录创建后切换到该目录
                }
            }
        }
    }
}
