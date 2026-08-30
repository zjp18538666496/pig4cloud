package com.pig4cloud.log.repository;

import com.pig4cloud.log.entity.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginLogRepository extends MongoRepository<LoginLog, String> {

    Page<LoginLog> findByUsernameContaining(String username, Pageable pageable);

    Page<LoginLog> findBySuccess(Boolean success, Pageable pageable);

    Page<LoginLog> findByUsernameContainingAndSuccess(String username, Boolean success, Pageable pageable);
}
