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

    /**
     * 删除存储文件（导出文件过保留期清理用）；实现按需覆盖
     */
    default void deleteFile(String path) throws Exception {
        throw new UnsupportedOperationException("当前存储不支持删除文件");
    }

    /**
     * 预签名下载URL（限时直连下载，绕过后端带宽）；不支持预签名的存储返回null
     */
    default String presignedGetUrl(String path, int expireSeconds) {
        return null;
    }

    /**
     * 预签名上传URL（前端直传）；不支持预签名的存储返回null
     */
    default String presignedPutUrl(String path, int expireSeconds) {
        return null;
    }

    /**
     * 流式上传（后端生成的文件，如导出Excel）；默认不支持，各实现按需覆盖
     */
    default String uploadFile(String path, java.io.InputStream in, long size) throws Exception {
        throw new UnsupportedOperationException("当前存储不支持流式上传");
    }
}
