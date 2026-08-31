package com.pig4cloud.monitor.config;

import com.pig4cloud.monitor.interceptor.ApiMetricsInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC配置：注册接口指标拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiMetricsInterceptor apiMetricsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiMetricsInterceptor).addPathPatterns("/api/**");
    }
}
