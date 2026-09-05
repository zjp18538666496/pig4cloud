package com.pig4cloud.backup.controller;

import com.pig4cloud.backup.service.BackupService;
import com.pig4cloud.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 备份管理（仅平台超管）：纯JDBC全量备份（建表语句+数据INSERT），不依赖mysqldump；
 * 列表/创建/下载/删除
 */
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('super')")
    public R<List<Map<String, Object>>> list() {
        return R.ok("获取数据成功", backupService.list());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('super')")
    public R<String> create() {
        return R.ok("备份完成", backupService.create());
    }

    @GetMapping("/download/{name}")
    @PreAuthorize("hasAuthority('super')")
    public ResponseEntity<InputStreamResource> download(@PathVariable String name) throws Exception {
        InputStream in = backupService.download(name);
        String fileName = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .body(new InputStreamResource(in));
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('super')")
    public R<Void> delete(@RequestBody Map<String, String> body) {
        backupService.delete(body.get("name"));
        return R.ok("删除成功", null);
    }
}
