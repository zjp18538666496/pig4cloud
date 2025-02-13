package com.pig4cloud.controller;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.service.impl.FTPServiceImpl;
import com.pig4cloud.util.file.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
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
import java.io.InputStream;
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

    @Autowired
    private FileUtils fileUtils;

    /**
     * 上传文件
     * @param file
     * @param name
     * @return
     * @throws Exception
     */
    @PostMapping("/upload")
    public String upload(@RequestParam("imgFile") MultipartFile file, @RequestParam("imgName") String name) throws Exception {
        // 设置上传至项目文件夹下的uploadFile文件夹中，没有文件夹则创建
        File dir = new File("uploadFile/imgFile");
        file.transferTo(new File(dir.getAbsolutePath() + File.separator + name + ".png"));
        return "上传完成！文件名：" + name;
    }

    /**
     * 下载文件
     * @param filename
     * @return
     */
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
    @PostMapping("/FTP/uploadFile")
    public Response uploadFile(@RequestParam("files") List<MultipartFile> files) {
        FTPClient ftpClient = new FTPClient();
        Map<String, Object> responseMessage = new HashMap<>();
        List<String> successList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();
        try {
            ftpService.configureFTPClient(ftpClient);
            // 遍历上传的文件列表
            for (MultipartFile file : files) {
                try {
                    String remotePath = "/test/" + fileUtils.generateFilePath(file);
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

    /**
     * 处理文件下载请求
     *
     * @param filename 要下载的文件名（FTP服务器上的路径）
     * @return 包含文件内容的响应实体
     */
    @GetMapping("/FTP/download1")
    public ResponseEntity<InputStreamResource> downloadFile1(@RequestParam("filename") String filename) {
        try {
            String downloadFileName = filename.substring(filename.lastIndexOf("/") + 1);
            // 调用服务层方法下载文件
            InputStream inputStream = ftpService.downloadFile(filename);
            // 封装文件输入流为Spring的InputStreamResource
            InputStreamResource resource = new InputStreamResource(inputStream);

            // 设置响应头信息，指定为附件下载
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFileName + "\"");

            // 返回文件流作为响应
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build(); // 如果出错，返回500错误
        } finally {

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
