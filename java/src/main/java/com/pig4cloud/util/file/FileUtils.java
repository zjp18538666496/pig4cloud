package com.pig4cloud.util.file;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Component
public class FileUtils {

    /**
     * 生成文件的存储路径
     *
     * @param file 上传的文件
     * @return 文件存储路径
     */
    public String generateFilePath(MultipartFile file) {
        String fileExtension = extractFileExtension(file);
        String uniqueFileName = generateUniqueFileName();
        String filePath = "/" + fileExtension + "/" + uniqueFileName + "/";
        return filePath;
    }

    /**
     * 提取文件扩展名
     *
     * @param file 上传的文件
     * @return 文件扩展名
     */
    private String extractFileExtension(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null ? contentType.split("/")[1] : "unknown";
    }

    /**
     * 生成唯一的文件名
     *
     * @return 唯一的文件名
     */
    private String generateUniqueFileName() {
        return UUID.randomUUID().toString();
    }
}
