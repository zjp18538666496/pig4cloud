package com.pig4cloud.log.service;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.log.entity.OperateLog;
import com.pig4cloud.log.repository.OperateLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务：异步写入MongoDB，写入失败不影响业务请求
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperateLogService {

    private final OperateLogRepository operateLogRepository;

    @Async
    public void saveOperateLog(OperateLog operateLog) {
        try {
            operateLogRepository.save(operateLog);
        } catch (Exception ex) {
            log.error("操作日志写入MongoDB失败: {}", ex.getMessage());
        }
    }

    public PageResult<OperateLog> pageQuery(String username, long page, long pageSize) {
        PageRequest pageable = PageRequest.of((int) (page - 1), (int) pageSize,
                Sort.by(Sort.Direction.DESC, "createTime"));
        Page<OperateLog> result = username == null || username.isBlank()
                ? operateLogRepository.findAll(pageable)
                : operateLogRepository.findByUsernameContaining(username, pageable);
        return PageResult.of(result.getContent(), result.getTotalElements(), pageSize, page);
    }
}
