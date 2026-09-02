package com.pig4cloud.health.controller;

import com.pig4cloud.common.result.R;
import com.pig4cloud.file.service.FtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 依赖健康自检：MySQL/Redis/MongoDB/FTP逐项连通性+耗时（登录即可），
 * 供监控中心状态灯展示，出问题第一眼定位是哪个依赖
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectProvider<MongoTemplate> mongoTemplateProvider;
    private final FtpService ftpService;

    @Value("${app.store.type:memory}")
    private String storeType;

    @GetMapping("/detail")
    public R<Map<String, Object>> detail() {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(checkMysql());
        items.add(checkRedis());
        items.add(checkMongo());
        items.add(checkFtp());

        boolean allUp = items.stream().allMatch(item -> "up".equals(item.get("status")));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overall", allUp ? "up" : "down");
        data.put("items", items);
        return R.ok("请求成功", data);
    }

    private Map<String, Object> checkMysql() {
        long start = System.currentTimeMillis();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", "MySQL");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            resultSet.next();
            return up(item, start);
        } catch (Exception ex) {
            return down(item, ex);
        }
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", "Redis");
        // memory模式未启用Redis，标记为skip而非down
        if (!"redis".equals(storeType)) {
            item.put("status", "skip");
            item.put("message", "未启用(app.store.type=memory)");
            return item;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            item.put("status", "skip");
            item.put("message", "RedisTemplate未装配");
            return item;
        }
        long start = System.currentTimeMillis();
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            item.put("message", pong);
            return up(item, start);
        } catch (Exception ex) {
            return down(item, ex);
        }
    }

    private Map<String, Object> checkMongo() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", "MongoDB");
        MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();
        if (mongoTemplate == null) {
            item.put("status", "skip");
            item.put("message", "MongoTemplate未装配");
            return item;
        }
        long start = System.currentTimeMillis();
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
            return up(item, start);
        } catch (Exception ex) {
            return down(item, ex);
        }
    }

    private Map<String, Object> checkFtp() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", "FTP");
        long start = System.currentTimeMillis();
        FTPClient ftpClient = new FTPClient();
        try {
            ftpService.configureFTPClient(ftpClient);
            ftpService.disconnectFTPClient(ftpClient);
            return up(item, start);
        } catch (Exception ex) {
            item.put("status", "down");
            item.put("costMs", System.currentTimeMillis() - start);
            item.put("error", ex.getMessage());
            return item;
        }
    }

    private Map<String, Object> up(Map<String, Object> item, long start) {
        item.put("status", "up");
        item.put("costMs", System.currentTimeMillis() - start);
        return item;
    }

    private Map<String, Object> down(Map<String, Object> item, Exception ex) {
        item.put("status", "down");
        item.put("error", ex.getMessage());
        return item;
    }
}
