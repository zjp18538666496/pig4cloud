package com.pig4cloud.file.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储抽象：按app.storage.type选择本地/FTP/S3实现。
 * 头像上传等业务只依赖本接口，切换存储方式不改业务代码
 */
public interface StorageService {

    /**
     * 上传文件到指定路径（路径含目录与文件名），返回实际存储路径
     */
    String upload(String path, MultipartFile file) throws Exception;

    /**
     * 按存储路径下载文件流
     */
    InputStream download(String path) throws Exception;
}
