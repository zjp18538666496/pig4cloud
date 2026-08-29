package com.pig4cloud.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 配置mybatis-plus插件：租户拦截器必须在分页拦截器之前
 */
@EnableTransactionManagement
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MyTenantHandler tenantHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户隔离：自动为租户表的SQL追加tenant_id条件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantHandler));
        // 分页
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
