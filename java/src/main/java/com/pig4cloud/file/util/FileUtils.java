package com.pig4cloud.file.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Component
public class FileUtils {

    /**
     * 生成文件存储目录：/{扩展名}/{UUID}/
     */
    public String generateFilePath(MultipartFile file) {
        String fileExtension = extractFileExtension(file);
        return "/" + fileExtension + "/" + generateUniqueFileName() + "/";
    }

    private String extractFileExtension(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.contains("/") ? contentType.split("/")[1] : "unknown";
    }

    private String generateUniqueFileName() {
        return UUID.randomUUID().toString();
    }
}
