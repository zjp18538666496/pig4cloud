package com.pig4cloud.job.core;

import com.pig4cloud.config.service.ConfigService;
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

/**
 * 日志保留清理：按 sys_config 的 log.retention-days 删除超期MongoDB操作/登录日志（0=永久保留）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanJob implements JobHandler {

    private final MongoTemplate mongoTemplate;
    private final ConfigService configService;

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
        log.info("日志清理完成：操作日志{}条，登录日志{}条，OpenAPI调用日志{}条（保留{}天）", operate, login, openApi, retentionDays);
    }
}
