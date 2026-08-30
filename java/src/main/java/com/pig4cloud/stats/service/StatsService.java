package com.pig4cloud.stats.service;

import com.pig4cloud.auth.online.OnlineUserStore;
import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.dept.mapper.DeptMapper;
import com.pig4cloud.log.entity.LoginLog;
import com.pig4cloud.notice.entity.NoticeEntity;
import com.pig4cloud.notice.service.NoticeService;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.tenant.mapper.TenantMapper;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
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
