package com.pig4cloud.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RestController
@RequestMapping("/file")
public class FileController {

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
