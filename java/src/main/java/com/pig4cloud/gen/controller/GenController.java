package com.pig4cloud.gen.controller;

import com.pig4cloud.common.result.R;
import com.pig4cloud.gen.service.GenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器（gen:manage权限，仅超管）：表元数据/代码预览/zip下载
 */
@RestController
@RequestMapping("/api/gen")
@RequiredArgsConstructor
public class GenController {

    private final GenService genService;

    /**
     * 可生成代码的表列表
     */
    @GetMapping("/tables")
    @PreAuthorize("hasAuthority('gen:manage')")
    public R<List<Map<String, Object>>> tables() {
        return R.ok("获取数据成功", genService.tables());
    }

    /**
     * 代码预览：返回 文件路径→代码内容 的映射
     */
    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('gen:manage')")
    public R<Map<String, String>> preview(@RequestBody GenService.GenQueryDto dto) {
        return R.ok("请求成功", genService.preview(dto));
    }

    /**
     * 打包下载生成代码（zip）
     */
    @PostMapping("/download")
    @PreAuthorize("hasAuthority('gen:manage')")
    public void download(@RequestBody GenService.GenQueryDto dto, HttpServletResponse response) throws IOException {
        Map<String, String> files = genService.preview(dto);
        response.setContentType("application/zip");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(dto.getTable() + "-code.zip", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=utf-8''" + fileName);
        try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream())) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }
}
