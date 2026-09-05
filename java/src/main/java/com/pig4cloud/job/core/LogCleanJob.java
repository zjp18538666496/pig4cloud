package com.pig4cloud.job.core;

import com.pig4cloud.config.service.ConfigService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.export.entity.SysExportTaskEntity;
import com.pig4cloud.export.mapper.SysExportTaskMapper;
import com.pig4cloud.file.service.StorageService;
import com.pig4cloud.log.entity.LoginLog;
import com.pig4cloud.log.entity.OpenApiLog;
import com.pig4cloud.log.entity.OperateLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 日志保留清理：按 sys_config 的 log.retention-days 删除超期MongoDB操作/登录日志（0=永久保留）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanJob implements JobHandler {

    private final MongoTemplate mongoTemplate;
    private final ConfigService configService;
    private final SysExportTaskMapper exportTaskMapper;
    private final StorageService storageService;

    @Override
    public String name() {
        return "logCleanJob";
    }

    @Override
    public void execute() {
        int retentionDays = configService.getInt("log.retention-days", 90);
        if (retentionDays <= 0) {
            log.info("日志保留天数为0，跳过清理");
            return;
        }
        Date cutoff = new Date(System.currentTimeMillis() - retentionDays * 24L * 3600 * 1000);
        long operate = mongoTemplate.remove(
                new Query(Criteria.where("createTime").lt(cutoff)), OperateLog.class).getDeletedCount();
        long login = mongoTemplate.remove(
                new Query(Criteria.where("createTime").lt(cutoff)), LoginLog.class).getDeletedCount();
        long openApi = mongoTemplate.remove(
                new Query(Criteria.where("createTime").lt(cutoff)), OpenApiLog.class).getDeletedCount();
        int exportFiles = cleanExportFiles(cutoff);
        log.info("日志清理完成：操作日志{}条，登录日志{}条，OpenAPI调用日志{}条，导出文件{}个（保留{}天）",
                operate, login, openApi, exportFiles, retentionDays);
    }

    /**
     * 导出中心文件按 export.retention-days（默认7天，0=不清理）清理：
     * 删除存储文件并将任务的file_path置空（任务行保留供查历史）
     */
    private int cleanExportFiles(Date cutoff) {
        int retentionDays = configService.getInt("export.retention-days", 7);
        if (retentionDays <= 0) {
            return 0;
        }
        Date fileCutoff = new Date(System.currentTimeMillis() - retentionDays * 24L * 3600 * 1000);
        int cleaned = 0;
        List<SysExportTaskEntity> tasks = exportTaskMapper.selectList(new QueryWrapper<SysExportTaskEntity>()
                .eq("status", "1")
                .isNotNull("file_path")
                .lt("finish_time", fileCutoff));
        for (SysExportTaskEntity task : tasks) {
            try {
                storageService.deleteFile(task.getFile_path());
            } catch (Exception ex) {
                log.warn("导出文件删除失败[{}]: {}", task.getFile_path(), ex.getMessage());
            }
            // updateById默认忽略null字段，file_path置空必须用UpdateWrapper显式set
            exportTaskMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<SysExportTaskEntity>()
                    .eq("id", task.getId())
                    .set("file_path", null)
                    .set("message", "文件已过保留期清理"));
            cleaned++;
        }
        return cleaned;
    }
}