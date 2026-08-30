package com.pig4cloud.log.service;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.log.entity.OperateLog;
import com.pig4cloud.log.repository.OperateLogRepository;
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

/**
 * 操作日志服务：异步写入MongoDB，写入失败不影响业务请求；
 * 查询按租户隔离（tenantId=null查全部，仅超管场景）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperateLogService {

    private final OperateLogRepository operateLogRepository;
    private final MongoTemplate mongoTemplate;

    @Async
    public void saveOperateLog(OperateLog operateLog) {
        try {
            operateLogRepository.save(operateLog);
        } catch (Exception ex) {
            log.error("操作日志写入MongoDB失败: {}", ex.getMessage());
        }
    }

    public PageResult<OperateLog> pageQuery(String username, Integer tenantId, long page, long pageSize) {
        PageRequest pageable = PageRequest.of((int) (page - 1), (int) pageSize,
                Sort.by(Sort.Direction.DESC, "createTime"));
        Query query = new Query();
        if (username != null && !username.isBlank()) {
            query.addCriteria(Criteria.where("username").regex(java.util.regex.Pattern.quote(username)));
        }
        if (tenantId != null) {
            query.addCriteria(Criteria.where("tenantId").is(tenantId));
        }
        long total = mongoTemplate.count(query, OperateLog.class);
        query.with(pageable);
        var list = mongoTemplate.find(query, OperateLog.class);
        return PageResult.of(list, total, pageSize, page);
    }
}
