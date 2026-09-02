package com.pig4cloud.apikey.filter;

import com.pig4cloud.apikey.entity.SysApiKeyEntity;
import com.pig4cloud.apikey.service.ApiKeyService;
import com.pig4cloud.auth.service.IpRateLimiter;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.R;
import com.pig4cloud.common.util.ResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Open API密钥过滤器：拦截/api/open/**，校验X-Api-Key（有效/启用/未过期）、
 * 按Key限流，将scopes放入请求属性供开放接口校验。其余路径直接放行（由JWT链负责）
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class OpenApiKeyFilter extends OncePerRequestFilter {

    private static final String OPEN_PREFIX = "/api/open/";

    private final ApiKeyService apiKeyService;
    private final IpRateLimiter ipRateLimiter;
    private final ResponseWriter responseWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(OPEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String apiKey = request.getHeader("X-Api-Key");
            SysApiKeyEntity entity = apiKeyService.validate(apiKey);
            ipRateLimiter.checkLimit("openapi:key", entity.getApi_key(),
                    apiKeyService.rateLimit(), 60 * 1000L, "Open API调用过于频繁（每分钟"
                            + apiKeyService.rateLimit() + "次上限）");
            Set<String> scopes = Arrays.stream(
                            (entity.getScopes() == null ? "" : entity.getScopes()).split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            request.setAttribute("openApiScopes", scopes);
            request.setAttribute("openApiKeyId", entity.getId());
            apiKeyService.touchLastUsed(entity.getId());
            filterChain.doFilter(request, response);
        } catch (BizException ex) {
            responseWriter.write(response, R.fail(ex.getCode(), ex.getMessage()));
        } catch (Exception ex) {
            responseWriter.write(response, R.fail("Open API调用失败：" + ex.getMessage()));
        }
    }
}
