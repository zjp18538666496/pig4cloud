package com.pig4cloud.log.service;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.log.entity.LoginLog;
import com.pig4cloud.log.repository.LoginLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 登录日志服务：异步写入MongoDB，写入失败不影响登录请求
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;

    @Async
    public void saveLoginLog(LoginLog loginLog) {
        try {
            loginLogRepository.save(loginLog);
        } catch (Exception ex) {
            log.error("登录日志写入MongoDB失败: {}", ex.getMessage());
        }
    }

    public PageResult<LoginLog> pageQuery(String username, Boolean success, long page, long pageSize) {
        PageRequest pageable = PageRequest.of((int) (page - 1), (int) pageSize,
                Sort.by(Sort.Direction.DESC, "createTime"));
        boolean hasUsername = username != null && !username.isBlank();
        Page<LoginLog> result;
        if (hasUsername && success != null) {
            result = loginLogRepository.findByUsernameContainingAndSuccess(username, success, pageable);
        } else if (hasUsername) {
            result = loginLogRepository.findByUsernameContaining(username, pageable);
        } else if (success != null) {
            result = loginLogRepository.findBySuccess(success, pageable);
        } else {
            result = loginLogRepository.findAll(pageable);
        }
        return PageResult.of(result.getContent(), result.getTotalElements(), pageSize, page);
    }
}
