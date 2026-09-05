package com.pig4cloud.backup.service;

import com.pig4cloud.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 数据库备份（界面化）：纯JDBC导出（SHOW CREATE TABLE建表语句 + 全量INSERT），
 * 不依赖服务器安装mysqldump，Windows/Linux均可运行。文件落app.backup.dir（默认backup/）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.backup.dir:backup}")
    private String backupDir;

    /**
     * 备份目录下的备份文件列表（name/sizeBytes/lastModifiedMs），新→旧
     */
    public List<Map<String, Object>> list() {
        File dir = new File(backupDir);
        List<Map<String, Object>> result = new ArrayList<>();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));
        if (files == null) {
            return result;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (File file : files) {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("name", file.getName());
            row.put("sizeBytes", file.length());
            row.put("lastModifiedMs", file.lastModified());
            result.add(row);
        }
        return result;
    }

    /**
     * 创建备份（同步，小库秒级）：返回文件名
     */
    public String create() {
        String fileName = "backup-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".sql";
        File dir = new File(backupDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException("创建备份目录失败：" + backupDir);
        }
        File file = new File(dir, fileName);
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() "
                        + "AND table_type = 'BASE TABLE' ORDER BY table_name", String.class);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            writer.write("-- pig4cloud admin 数据库备份 " + new Date());
            writer.newLine();
            writer.write("SET NAMES utf8mb4;");
            writer.newLine();
            writer.write("SET FOREIGN_KEY_CHECKS = 0;");
            writer.newLine();
            for (String table : tables) {
                Map<String, Object> create = jdbcTemplate.queryForMap("SHOW CREATE TABLE `" + table + "`");
                writer.newLine();
                writer.write("-- ----------------------------");
                writer.newLine();
                writer.write("-- 表结构 " + table);
                writer.newLine();
                writer.write("-- ----------------------------");
                writer.newLine();
                writer.write(String.valueOf(create.values().iterator().next()) + ";");
                writer.newLine();
                List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM `" + table + "`");
                if (!rows.isEmpty()) {
                    writer.write("-- 数据 " + table + "（" + rows.size() + "行）");
                    writer.newLine();
                    for (Map<String, Object> row : rows) {
                        writer.write(buildInsert(table, row));
                        writer.newLine();
                    }
                }
            }
            writer.newLine();
            writer.write("SET FOREIGN_KEY_CHECKS = 1;");
            writer.newLine();
        } catch (Exception ex) {
            file.delete();
            log.error("数据库备份失败", ex);
            throw new BizException("备份失败：" + ex.getMessage());
        }
        log.info("数据库备份完成：{}（{}张表，{}KB）", fileName, tables.size(), file.length() / 1024);
        return fileName;
    }

    /**
     * 备份文件下载流
     */
    public InputStream download(String name) throws Exception {
        return new FileInputStream(resolve(name));
    }

    /**
     * 删除备份文件
     */
    public void delete(String name) {
        resolve(name).delete();
    }

    private File resolve(String name) {
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\") || !name.endsWith(".sql")) {
            throw new BizException("非法的备份文件名");
        }
        File file = new File(backupDir, name);
        if (!file.exists()) {
            throw new BizException("备份文件不存在");
        }
        return file;
    }

    private String buildInsert(String table, Map<String, Object> row) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (!first) {
                columns.append(", ");
                values.append(", ");
            }
            first = false;
            columns.append("`").append(entry.getKey()).append("`");
            Object value = entry.getValue();
            values.append(value == null ? "NULL" : "'" + String.valueOf(value)
                    .replace("\\", "\\\\").replace("'", "\\'") + "'");
        }
        return "INSERT INTO `" + table + "` (" + columns + ") VALUES (" + values + ");";
    }
}
