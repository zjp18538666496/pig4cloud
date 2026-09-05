package com.pig4cloud.log.service;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.log.entity.OpenApiLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Open API调用日志服务：异步写入MongoDB（失败不影响调用），按Key/时间/结果分页查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiLogService {

    private final MongoTemplate mongoTemplate;

    @Async
    public void saveAsync(OpenApiLog openApiLog) {
        try {
            mongoTemplate.save(openApiLog);
        } catch (Exception ex) {
            log.error("Open API调用日志写入失败: {}", ex.getMessage());
        }
    }

    /**
     * 分页查询（超管专用，apikey:manage权限点控制）
     */
    public PageResult<OpenApiLog> pageQuery(Integer keyId, Boolean success, Long startTime,
                                            Long endTime, long page, long pageSize) {
        List<Criteria> criteria = new ArrayList<>();
        if (keyId != null) {
            criteria.add(Criteria.where("keyId").is(keyId));
        }
        if (success != null) {
            criteria.add(Criteria.where("success").is(success));
        }
        if (startTime != null || endTime != null) {
            Criteria timeCriteria = Criteria.where("createTime");
            if (startTime != null) {
                timeCriteria.gte(new java.util.Date(startTime));
            }
            if (endTime != null) {
                timeCriteria.lte(new java.util.Date(endTime));
            }
            criteria.add(timeCriteria);
        }
        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }
        long total = mongoTemplate.count(query, OpenApiLog.class);
        query.with(PageRequest.of((int) Math.max(0, page - 1), (int) pageSize,
                Sort.by(Sort.Direction.DESC, "createTime")));
        List<OpenApiLog> rows = mongoTemplate.find(query, OpenApiLog.class);
        return PageResult.of(rows, total, pageSize, page);
    }
}
