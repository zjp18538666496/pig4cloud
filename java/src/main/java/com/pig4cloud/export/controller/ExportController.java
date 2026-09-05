package com.pig4cloud.export.controller;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.export.entity.SysExportTaskEntity;
import com.pig4cloud.export.service.ExportService;
import com.pig4cloud.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

/**
 * 导出中心：异步导出任务提交/查询/下载
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    private String currentUser() {
        return SecurityContextHolder.getContext().getAuthentication() == null ? null
                : SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * 提交用户导出任务（数据行同步构建保留数据权限，Excel异步生成）
     */
    @PostMapping("/submitUser")
    public R<Long> submitUser(@RequestBody UserDto dto) {
        return exportService.submitUserExport(dto, currentUser());
    }

    @PostMapping("/getLists")
    public R<PageResult<SysExportTaskEntity>> getLists(@RequestBody com.pig4cloud.common.dto.BasePageQuery dto) {
        return exportService.getLists(dto, currentUser());
    }

    /**
     * 下载任务文件
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Integer id) {
        InputStream in = exportService.download(id, currentUser());
        String fileName = "导出文件-" + id + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8) + "\"")
                .body(new InputStreamResource(in));
    }
}
