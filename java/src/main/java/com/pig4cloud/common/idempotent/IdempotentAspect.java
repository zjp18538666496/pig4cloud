package com.pig4cloud.common.idempotent;

import com.pig4cloud.common.annotation.Idempotent;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.store.StateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 防重复提交切面：用户+URI+参数摘要作为幂等键（StateStore putIfAbsent），
 * 窗口内重复请求直接拒绝。网络重试/双击都不会产生重复数据
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final StateStore stateStore;

    @Before("@annotation(idempotent)")
    public void check(JoinPoint joinPoint, Idempotent idempotent) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        var request = attributes.getRequest();
        String user = SecurityContextHolder.getContext().getAuthentication() == null ? "anonymous"
                : SecurityContextHolder.getContext().getAuthentication().getName();
        // 无缓存包装器时退化为query串（GET幂等场景少，POST JSON主体常见，但保持健壮）
        String params = request instanceof ContentCachingRequestWrapper wrapper
                ? new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8) + request.getQueryString()
                : (request.getQueryString() == null ? "" : request.getQueryString());
        String key = "idempotent:" + user + ":" + request.getRequestURI() + ":" + md5(params);
        if (!stateStore.putIfAbsent(key, "1", idempotent.intervalSeconds() * 1000L)) {
            log.warn("重复提交拦截：user={} uri={}", user, request.getRequestURI());
            throw new BizException("操作处理中，请勿重复提交");
        }
    }

    private String md5(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return "raw";
        }
    }
}
