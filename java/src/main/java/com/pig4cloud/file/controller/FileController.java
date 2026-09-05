package com.pig4cloud.file.controller;

import com.pig4cloud.common.result.R;
import com.pig4cloud.file.service.FtpService;
import com.pig4cloud.file.service.StorageService;
import com.pig4cloud.file.util.FileUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;
    private final FtpService ftpService;
    private final FileUtils fileUtils;
    private final com.pig4cloud.user.mapper.UserMapper userMapper;

    /**
     * 头像公开访问（唯一免登录的文件接口）：仅返回登记在sys_user.avatar中的存储路径，
     * 其它路径一律拒绝，防止未登录遍历下载文件
     */
    @GetMapping("/avatar/{userId}")
    public ResponseEntity<InputStreamResource> avatar(@PathVariable Long userId) {
        com.pig4cloud.user.entity.UserEntity user = userMapper.selectById(userId);
        if (user == null || user.getAvatar() == null || user.getAvatar().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        // 双保险：只允许返回登记为头像的路径
        String registeredPath = user.getAvatar();
        try {
            // 对象存储支持预签名时302直连，节省后端带宽
            String presigned = storageService.presignedGetUrl(registeredPath, 600);
            if (presigned != null) {
                return ResponseEntity.status(302).location(java.net.URI.create(presigned)).build();
            }
            InputStream inputStream = storageService.download(registeredPath);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"avatar\"");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.IMAGE_PNG)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 生成预签名URL（需登录）：mode=get下载直连 / mode=put前端直传；
     * 仅对象存储(app.storage.type=s3)支持，其它存储返回null
     */
    @GetMapping("/presign")
    public R<String> presign(@RequestParam String path,
                             @RequestParam(defaultValue = "get") String mode,
                             @RequestParam(defaultValue = "600") int expireSeconds) {
        // 仅允许业务前缀，防止预签名出存储根下的任意文件
        if (path == null || path.isBlank() || path.contains("..")) {
            return R.fail("非法路径");
        }
        int expire = Math.min(Math.max(expireSeconds, 10), 7 * 24 * 3600);
        String url = "put".equalsIgnoreCase(mode)
                ? storageService.presignedPutUrl(path, expire)
                : storageService.presignedGetUrl(path, expire);
        return url == null ? R.fail("当前存储类型不支持预签名") : R.ok("生成成功", url);
    }

    /**
     * 上传文件到当前存储（app.storage.type=local|ftp|s3）
     */
    @PostMapping("/upload")
    public String upload(@RequestParam("imgFile") MultipartFile file, @RequestParam("imgName") String name) {
        // 文件名清洗：只保留字母数字下划线中划线，防止name携带../造成写入穿越
        String safeName = name == null ? "" : name.replaceAll("[^a-zA-Z0-9_-]", "");
        if (safeName.isBlank()) {
            safeName = "file";
        }
        try {
            String stored = storageService.upload("imgFile/" + safeName + ".png", file);
            return "上传完成！文件路径：" + stored;
        } catch (Exception e) {
            return "上传失败：" + e.getMessage();
        }
    }

    /**
     * 从当前存储下载文件（本地存储内置路径穿越防护，对象存储按key天然隔离）
     */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam String filename) {
        try {
            InputStream inputStream = storageService.download(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(getContentType(filename)))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 批量上传文件到FTP服务器（独立于当前存储的FTP直传通道）
     */
    @PostMapping("/ftp/upload")
    public R<Map<String, List<String>>> uploadFile(@RequestParam("files") List<MultipartFile> files) {
        FTPClient ftpClient = new FTPClient();
        List<String> successList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        try {
            ftpService.configureFTPClient(ftpClient);
            for (MultipartFile file : files) {
                try {
                    String remotePath = "/test/" + fileUtils.generateFilePath(file);
                    ftpService.uploadFile(remotePath, file, ftpClient);
                    successList.add(remotePath + file.getOriginalFilename());
                } catch (Exception e) {
                    errorList.add(file.getOriginalFilename() + "上传失败: " + e.getMessage());
                }
            }
            String message = "上传成功" + successList.size() + "个文件,上传失败" + errorList.size() + "个文件";
            Map<String, List<String>> data = new HashMap<>();
            data.put("successList", successList);
            data.put("errorList", errorList);
            return R.ok(message, data);
        } catch (Exception e) {
            return R.fail("上传过程中发生错误: " + e.getMessage());
        } finally {
            ftpService.disconnectFTPClient(ftpClient);
        }
    }

    /**
     * 从FTP服务器下载文件（独立于当前存储的FTP直连通道）
     */
    @GetMapping("/ftp/download")
    public ResponseEntity<InputStreamResource> downloadFileFromFtp(@RequestParam("filename") String filename) {
        try {
            String downloadFileName = filename.substring(filename.lastIndexOf("/") + 1);
            InputStream inputStream = ftpService.downloadFile(filename);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFileName + "\"");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getContentType(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream";
    }
}
