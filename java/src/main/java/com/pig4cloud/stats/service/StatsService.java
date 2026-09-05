package com.pig4cloud.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.auth.online.OnlineUserStore;
import com.pig4cloud.auth.online.SessionRecord;
import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.dept.mapper.DeptMapper;
import com.pig4cloud.log.entity.LoginLog;
import com.pig4cloud.notice.entity.NoticeEntity;
import com.pig4cloud.notice.service.NoticeService;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.tenant.entity.TenantEntity;
import com.pig4cloud.tenant.mapper.TenantMapper;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页仪表盘统计：各实体数量走MySQL（租户拦截器自动隔离），
 * 登录趋势走MongoDB login_log，在线人数取内存会话数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final DeptMapper deptMapper;
    private final TenantMapper tenantMapper;
    private final OnlineUserStore onlineUserStore;
    private final NoticeService noticeService;
    private final MongoTemplate mongoTemplate;
    private final com.pig4cloud.approval.mapper.SysApprovalMapper approvalMapper;

    public Map<String, Object> dashboard() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userCount", userMapper.selectCount(null));
        stats.put("roleCount", roleMapper.selectCount(null));
        stats.put("deptCount", deptMapper.selectCount(null));
        if (UserContext.isSuperTenant()) {
            stats.put("tenantCount", tenantMapper.selectCount(null));
        }
        stats.put("onlineCount", onlineUserStore.count());

        // 近7日登录趋势（含今日），无数据的日期补0
        List<Map<String, Object>> trend = loginTrend(7);
        stats.put("loginTrend", trend);
        stats.put("todayLoginCount", trend.isEmpty() ? 0
                : ((Number) trend.get(trend.size() - 1).get("count")).intValue());

        List<NoticeEntity> notices = noticeService.latestPublished(5);
        stats.put("latestNotices", notices);
        return stats;
    }

    /**
     * 大屏总览（screen:read）：登录趋势/今日登录/在线/用户/租户 + 审批量统计 + 通知送达率
     */
    public Map<String, Object> screenSummary() {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> trend = loginTrend(7);
        data.put("loginTrend", trend);
        data.put("todayLoginCount", trend.isEmpty() ? 0
                : ((Number) trend.get(trend.size() - 1).get("count")).intValue());
        data.put("onlineCount", onlineUserStore.count());
        data.put("userCount", userMapper.selectCount(null));
        data.put("tenantCount", tenantMapper.selectCount(null));
        // 审批量（轻量审批）
        Map<String, Object> approval = new LinkedHashMap<>();
        approval.put("pending", approvalMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.pig4cloud.approval.entity.SysApprovalEntity>()
                        .eq("status", "0")));
        approval.put("approved", approvalMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.pig4cloud.approval.entity.SysApprovalEntity>()
                        .eq("status", "1")));
        approval.put("rejected", approvalMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.pig4cloud.approval.entity.SysApprovalEntity>()
                        .eq("status", "2")));
        data.put("approval", approval);
        // 通知送达率（notify_log）
        Map<String, Object> notify = new LinkedHashMap<>();
        long total = mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(),
                com.pig4cloud.notify.entity.NotifyLog.class);
        long success = mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("success").is(true)),
                com.pig4cloud.notify.entity.NotifyLog.class);
        notify.put("total", total);
        notify.put("success", success);
        notify.put("successRate", total == 0 ? "-" : Math.round(success * 1000 / total) / 10.0 + "%");
        data.put("notify", notify);
        return data;
    }

    /**
     * 租户维度使用报表（数据大屏，仅超管）：
     * 每租户 用户数/角色数/部门数/在线数/配额使用率 + 近30天登录次数（Mongo按tenantId聚合）
     */
    public List<Map<String, Object>> tenantReport() {
        List<TenantEntity> tenants = tenantMapper.selectList(
                new QueryWrapper<TenantEntity>().orderByAsc("id"));
        // 近30天登录按租户聚合
        Date since = Date.from(LocalDate.now().minusDays(29)
                .atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
        Map<Integer, Long> loginByTenant = new HashMap<>();
        try {
            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("success").is(true).and("createTime").gte(since)),
                    Aggregation.group("tenantId").count().as("count"));
            AggregationResults<Map> results =
                    mongoTemplate.aggregate(aggregation, "login_log", Map.class);
            for (Map row : results.getMappedResults()) {
                Object tenantId = row.get("_id");
                if (tenantId instanceof Number number) {
                    loginByTenant.put(number.intValue(), ((Number) row.get("count")).longValue());
                }
            }
        } catch (Exception ex) {
            log.error("租户登录统计失败: {}", ex.getMessage());
        }
        // 在线数按租户汇总
        Map<Integer, Long> onlineByTenant = new HashMap<>();
        for (SessionRecord session : onlineUserStore.list()) {
            onlineByTenant.merge(session.getTenantId(), 1L, Long::sum);
        }
        List<Map<String, Object>> report = new ArrayList<>();
        for (TenantEntity tenant : tenants) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenant.getId());
            row.put("tenantName", tenant.getTenant_name());
            row.put("status", tenant.getStatus());
            row.put("expireTime", tenant.getExpire_time());
            row.put("userCount", userMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.pig4cloud.user.entity.UserEntity>()
                            .eq("tenant_id", tenant.getId())));
            row.put("roleCount", roleMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.pig4cloud.role.entity.RoleEntity>()
                            .eq("tenant_id", tenant.getId())));
            row.put("deptCount", deptMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.pig4cloud.dept.entity.DeptEntity>()
                            .eq("tenant_id", tenant.getId())));
            row.put("onlineCount", onlineByTenant.getOrDefault(tenant.getId(), 0L));
            row.put("login30d", loginByTenant.getOrDefault(tenant.getId(), 0L));
            row.put("userLimit", tenant.getUser_limit());
            row.put("quotaUsedPercent", tenant.getUser_limit() == null ? null
                    : Math.round(userMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.pig4cloud.user.entity.UserEntity>()
                            .eq("tenant_id", tenant.getId())) * 1000.0 / tenant.getUser_limit()) / 10.0);
            report.add(row);
        }
        return report;
    }

    /**
     * 近N天登录成功趋势：从MongoDB拉取时间窗内的成功登录记录，按天（东八区）分组计数
     */
    private List<Map<String, Object>> loginTrend(int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        Date start = Date.from(startDate.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
        Map<String, Long> countByDay = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            countByDay.put(startDate.plusDays(i).toString(), 0L);
        }
        try {
            Query query = new Query(Criteria.where("success").is(true)
                    .and("createTime").gte(start));
            query.fields().include("createTime");
            List<LoginLog> logs = mongoTemplate.find(query, LoginLog.class);
            for (LoginLog loginLog : logs) {
                if (loginLog.getCreateTime() == null) {
                    continue;
                }
                String day = loginLog.getCreateTime().toInstant()
                        .atZone(ZoneId.of("Asia/Shanghai")).toLocalDate().toString();
                countByDay.computeIfPresent(day, (key, count) -> count + 1);
            }
        } catch (Exception ex) {
            log.error("统计登录趋势失败: {}", ex.getMessage());
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        countByDay.forEach((day, count) -> trend.add(Map.of("date", day, "count", count)));
        return trend;
    }
}
