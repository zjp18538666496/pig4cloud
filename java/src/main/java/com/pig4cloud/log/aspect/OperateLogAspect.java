package com.pig4cloud.log.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.log.entity.OperateLog;
import com.pig4cloud.log.service.OperateLogService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 操作日志切面：拦截@LogRecord标注的接口，记录操作人/参数(脱敏)/结果/耗时并异步写入MongoDB
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperateLogAspect {

    private static final int MAX_LENGTH = 2000;
    private static final String MASK = "******";
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "newPassword", "confirmPassword", "oldPassword", "refreshToken");

    private final ObjectMapper objectMapper;
    private final OperateLogService operateLogService;

    @Around("@annotation(logRecord)")
    public Object around(ProceedingJoinPoint joinPoint, LogRecord logRecord) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            record(joinPoint, logRecord, result, null, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            record(joinPoint, logRecord, null, ex, System.currentTimeMillis() - start);
            throw ex;
        }
    }

    private void record(ProceedingJoinPoint joinPoint, LogRecord logRecord, Object result, Throwable ex, long costMs) {
        try {
            JsonNode paramsNode = toParamsNode(joinPoint.getArgs());
            OperateLog operateLog = new OperateLog();
            operateLog.setModule(logRecord.module());
            operateLog.setOperation(logRecord.operation());
            operateLog.setUsername(resolveUsername(paramsNode));
            // 租户上下文：公开接口(登录/注册)为null，仅超管可见
            if (com.pig4cloud.common.context.UserContext.get() != null) {
                operateLog.setTenantId(com.pig4cloud.common.context.UserContext.getTenantId());
            }
            fillRequestInfo(operateLog);
            operateLog.setParams(truncate(writeJson(paramsNode)));
            if (ex != null) {
                operateLog.setCode(R.FAIL);
                operateLog.setErrorMsg(truncate(ex.getMessage()));
            } else if (result instanceof R<?> r) {
                operateLog.setCode(r.getCode());
                operateLog.setErrorMsg(r.getCode() == R.SUCCESS ? null : r.getMessage());
            }
            operateLog.setSuccess(operateLog.getCode() != null && operateLog.getCode() == R.SUCCESS);
            operateLog.setCostMs(costMs);
            operateLog.setCreateTime(new Date());
            operateLogService.saveOperateLog(operateLog);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    private void fillRequestInfo(OperateLog operateLog) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        operateLog.setMethod(request.getMethod());
        operateLog.setUrl(request.getRequestURI());
        operateLog.setIp(resolveIp(request));
    }

    /**
     * 序列化请求参数为JsonNode：过滤文件流/Servlet对象等不可序列化参数
     */
    private JsonNode toParamsNode(Object[] args) {
        List<Object> filtered = Arrays.stream(args)
                .filter(arg -> !(arg instanceof MultipartFile)
                        && !(arg instanceof ServletRequest)
                        && !(arg instanceof ServletResponse)
                        && !(arg instanceof BindingResult))
                .toList();
        JsonNode node = objectMapper.valueToTree(filtered.size() == 1 ? filtered.get(0) : filtered);
        maskSensitive(node);
        return node;
    }

    /**
     * 递归掩码敏感字段
     */
    private void maskSensitive(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                if (SENSITIVE_KEYS.contains(fieldName)) {
                    objectNode.put(fieldName, MASK);
                } else {
                    maskSensitive(objectNode.get(fieldName));
                }
            }
        } else if (node.isArray()) {
            node.forEach(this::maskSensitive);
        }
    }

    /**
     * 操作人：优先取认证上下文，未认证接口(登录/注册)取请求参数中的username
     */
    private String resolveUsername(JsonNode paramsNode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof String name
                && !"anonymousUser".equals(name)) {
            return name;
        }
        return paramsNode.path("username").isTextual() ? paramsNode.path("username").asText() : null;
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return "(参数序列化失败)";
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) + "...(截断)" : text;
    }
}
