package com.pig4cloud.service.impl;

import com.pig4cloud.config.FTPConfig;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@Service
public class FTPServiceImpl {
    @Autowired
    private FTPConfig ftpConfig;

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
            // 调用上传文件的具体实现方法
            uploadFile(remotePath, file, ftpClient);
        } finally {
            // 断开FTP客户端连接
            disconnectFTPClient(ftpClient);
        }
    }

    /**
     * 实际执行文件上传到FTP服务器的方法
     *
     * @param remotePath FTP服务器上的目录路径
     * @param file       要上传的文件
     * @param ftpClient  FTP客户端实例
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
                    System.out.println("File " + file.getOriginalFilename() + " has been uploaded successfully.");
                } else {
                    throw new IOException("Failed to upload file " + file.getOriginalFilename());
                }
            }
        } catch (IOException ex) {
            throw new IOException("Failed to upload file " + file.getOriginalFilename());
        }
    }

    /**
     * 下载文件并返回输入流
     *
     * @param remoteFilePath FTP服务器上的文件路径
     * @return 文件输入流
     * @throws IOException 如果下载失败则抛出异常
     */
    public InputStream downloadFile(String remoteFilePath) throws IOException {
        FTPClient ftpClient = new FTPClient();
        // 使用管道流实现下载和合并文件的流式处理
        PipedInputStream pipedInputStream = new PipedInputStream();
        PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);

        // 在新线程中执行下载操作
        new Thread(() -> {
            try {
                // 配置FTP客户端
                configureFTPClient(ftpClient);
                // 调用下载文件的具体实现方法
                downloadFile(remoteFilePath, pipedOutputStream, ftpClient);
                pipedOutputStream.close(); // 关闭输出流
            } catch (IOException ex) {
                try {
                    pipedOutputStream.close();
                } catch (IOException ignored) {
                }
            } finally {
                // 断开FTP客户端连接
                disconnectFTPClient(ftpClient);
            }
        }).start();

        return pipedInputStream; // 返回管道输入流，实现流式下载
    }

    /**
     * 实际执行文件下载并写入输出流的方法
     *
     * @param remoteFilePath FTP服务器上的文件路径
     * @param outputStream   输出流，用于写入文件内容
     * @param ftpClient      FTP客户端实例
     * @throws IOException 如果下载失败则抛出异常
     */
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
        try {
            // 获取文件大小
            int sizeReply = ftpClient.sendCommand("SIZE " + remoteFilePath);
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                throw new IOException("Failed to get the size of the remote file: " + sizeReply);
            }

            // 分块下载文件
            long startByte = 0;
            ftpClient.sendCommand("REST " + startByte);

            try (InputStream inputStream = ftpClient.retrieveFileStream(remoteFilePath)) {
                if (inputStream == null) {
                    throw new IOException("Failed to retrieve file stream: " + ftpClient.getReplyString());
                }
                // 缓冲区
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead); // 写入输出流
                }
                // 完成文件传输确认
                if (!ftpClient.completePendingCommand()) {
                    throw new IOException("Failed to complete pending command: " + ftpClient.getReplyString());
                }
            }
        } catch (IOException ignored) {

        }
    }

    /**
     * 配置FTP客户端连接参数
     *
     * @param ftpClient FTP客户端实例
     * @throws IOException 如果配置失败则抛出异常
     */
    public void configureFTPClient(FTPClient ftpClient) throws IOException {
        ftpClient.connect(ftpConfig.getServer(), ftpConfig.getPort());
        ftpClient.login(ftpConfig.getUser(), ftpConfig.getPassword());
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.setBufferSize(1024 * 1024 * 10); // 设置1MB的缓冲区大小
        ftpClient.setControlEncoding(StandardCharsets.UTF_8.name());
    }

    /**
     * 断开FTP客户端连接
     *
     * @param ftpClient FTP客户端实例
     */
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
     * 确保FTP服务器上的目录路径存在，不存在则创建
     *
     * @param ftpClient FTP客户端实例
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
                    ftpClient.changeWorkingDirectory(currentPath);
                }
            }
        }
    }
}
