package com.pig4cloud.log.service;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.log.entity.LoginLog;
import com.pig4cloud.log.repository.LoginLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录日志服务：异步写入MongoDB，写入失败不影响登录请求；
 * 查询按租户隔离（tenantId=null查全部，仅超管场景）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;
    private final MongoTemplate mongoTemplate;

    @Async
    public void saveLoginLog(LoginLog loginLog) {
        try {
            loginLogRepository.save(loginLog);
        } catch (Exception ex) {
            log.error("登录日志写入MongoDB失败: {}", ex.getMessage());
        }
    }

    /**
     * 最近一次成功登录记录（异地登录提醒用）
     */
    public LoginLog lastSuccessLogin(String username) {
        Query query = new Query(Criteria.where("username").is(username).and("success").is(true))
                .with(Sort.by(Sort.Direction.DESC, "createTime")).limit(1);
        return mongoTemplate.findOne(query, LoginLog.class);
    }

    public PageResult<LoginLog> pageQuery(String username, Boolean success, Integer tenantId, long page, long pageSize) {
        PageRequest pageable = PageRequest.of((int) (page - 1), (int) pageSize,
                Sort.by(Sort.Direction.DESC, "createTime"));
        List<Criteria> criteria = new ArrayList<>();
        if (username != null && !username.isBlank()) {
            criteria.add(Criteria.where("username").regex(java.util.regex.Pattern.quote(username)));
        }
        if (success != null) {
            criteria.add(Criteria.where("success").is(success));
        }
        if (tenantId != null) {
            criteria.add(Criteria.where("tenantId").is(tenantId));
        }
        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }
        long total = mongoTemplate.count(query, LoginLog.class);
        query.with(pageable);
        var list = mongoTemplate.find(query, LoginLog.class);
        return PageResult.of(list, total, pageSize, page);
    }
}
