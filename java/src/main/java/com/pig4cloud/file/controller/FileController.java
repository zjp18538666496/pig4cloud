package com.pig4cloud.file.controller;

import com.pig4cloud.common.result.R;
import com.pig4cloud.file.service.FtpService;
import com.pig4cloud.file.util.FileUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FtpService ftpService;
    private final FileUtils fileUtils;

    /**
     * 上传文件到本地uploadFile目录
     */
    @PostMapping("/upload")
    public String upload(@RequestParam("imgFile") MultipartFile file, @RequestParam("imgName") String name) throws Exception {
        File dir = new File("uploadFile/imgFile");
        file.transferTo(new File(dir.getAbsolutePath() + File.separator + name + ".png"));
        return "上传完成！文件名：" + name;
    }

    /**
     * 从本地uploadFile目录下载文件
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename) {
        try {
            Path fileStorageLocation = Paths.get("uploadFile").toAbsolutePath().normalize();
            Path filePath = fileStorageLocation.resolve(filename).normalize();
            // 防止路径穿越下载目录之外的文件
            if (!filePath.startsWith(fileStorageLocation)) {
                return ResponseEntity.badRequest().build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(getContentType(filePath)))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 批量上传文件到FTP服务器
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
                } catch (IOException e) {
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
     * 从FTP服务器下载文件
     */
    @GetMapping("/ftp/download")
    public ResponseEntity<InputStreamResource> downloadFileFromFtp(@RequestParam("filename") String filename) {
        try {
            String downloadFileName = filename.substring(filename.lastIndexOf("/") + 1);
            InputStream inputStream = ftpService.downloadFile(filename);
            InputStreamResource resource = new InputStreamResource(inputStream);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFileName + "\"");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getContentType(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream";
    }
}
