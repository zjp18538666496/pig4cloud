package com.pig4cloud.log.repository;

import com.pig4cloud.log.entity.OperateLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OperateLogRepository extends MongoRepository<OperateLog, String> {

    Page<OperateLog> findByUsernameContaining(String username, Pageable pageable);
}
