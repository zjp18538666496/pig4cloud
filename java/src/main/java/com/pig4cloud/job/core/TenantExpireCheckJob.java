package com.pig4cloud.job.core;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.tenant.entity.TenantEntity;
import com.pig4cloud.tenant.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 租户过期自动禁用：每天检查 expire_time 已过期的启用租户并置为禁用（登录侧同时有过期拦截兜底）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantExpireCheckJob implements JobHandler {

    private final TenantMapper tenantMapper;

    @Override
    public String name() {
        return "tenantExpireCheckJob";
    }

    @Override
    public void execute() {
        int rows = tenantMapper.update(null, new UpdateWrapper<TenantEntity>()
                .eq("status", "1")
                .lt("expire_time", new Date())
                .set("status", "0")
                .set("update_time", new Date()));
        log.info("租户过期检查完成，自动禁用{}个租户", rows);
    }
}
