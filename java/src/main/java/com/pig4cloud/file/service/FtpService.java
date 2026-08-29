package com.pig4cloud.file.service;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FtpService {

    /**
     * 上传文件到FTP服务器指定路径（内部创建并关闭连接）
     */
    void uploadFile(String remotePath, MultipartFile file) throws IOException;

    /**
     * 上传文件到FTP服务器指定路径（复用外部传入的连接）
     */
    void uploadFile(String remotePath, MultipartFile file, FTPClient ftpClient) throws IOException;

    /**
     * 下载文件，返回输入流（后台线程写入，连接在写入完成后关闭）
     */
    InputStream downloadFile(String remoteFilePath) throws IOException;

    /**
     * 下载文件并写入输出流
     */
    void downloadFile(String remoteFilePath, OutputStream outputStream, FTPClient ftpClient) throws IOException;

    /**
     * 创建并配置FTP客户端连接
     */
    void configureFTPClient(FTPClient ftpClient) throws IOException;

    /**
     * 登出并断开FTP连接
     */
    void disconnectFTPClient(FTPClient ftpClient);
}
