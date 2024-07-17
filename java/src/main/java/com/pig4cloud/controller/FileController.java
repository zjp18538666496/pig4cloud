package com.pig4cloud.controller;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.service.impl.FTPServiceImpl;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RestController
@RequestMapping("/file")
public class FileController {
    @Autowired
    private FTPServiceImpl ftpService;

    @PostMapping("/upload")
    public String upload(@RequestParam("imgFile") MultipartFile file, @RequestParam("imgName") String name) throws Exception {
        // 设置上传至项目文件夹下的uploadFile文件夹中，没有文件夹则创建
        File dir = new File("uploadFile/imgFile");
        file.transferTo(new File(dir.getAbsolutePath() + File.separator + name + ".png"));
        return "上传完成！文件名：" + name;
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename) {
        try {
            Path fileStorageLocation = Paths.get("uploadFile").toAbsolutePath().normalize();
            Path filePath = fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                // 根据文件类型设置正确的MIME类型
                String contentType = getContentType(filePath);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            // Handle file not found exception
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 上传多个文件
     *
     * @param files 上传的文件列表
     * @return 响应结果
     */
    @PostMapping("/uploadFile")
    public Response uploadFile(@RequestParam("files") List<MultipartFile> files) {
        FTPClient ftpClient = new FTPClient();
        Map<String, Object> responseMessage = new HashMap<>();
        List<String> successList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        try {
            ftpService.configureFTPClient(ftpClient);
            String remotePath = "/test/";
            // 遍历上传的文件列表
            for (MultipartFile file : files) {
                try {
                    // 上传每个文件
                    ftpService.uploadFile(remotePath, file, ftpClient);
                    successList.add(remotePath + file.getOriginalFilename());
                } catch (IOException e) {
                    errorList.add(file.getOriginalFilename() + "上传失败: " + e.getMessage());
                }
            }
            responseMessage.put("successList", successList);
            responseMessage.put("errorList", errorList);
            return new ResponseImpl(200, "上传成功" + successList.size() + "个文件,上传失败" + errorList.size() + "个文件", responseMessage);
        } catch (Exception e) {
            return new ResponseImpl(-200, "上传过程中发生错误: " + e.getMessage(), null);
        } finally {
            // 断开FTP客户端连接
            ftpService.disconnectFTPClient(ftpClient);
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
        } else {
            // Fallback to a default content type
            return "application/octet-stream";
        }
    }
}
