package com.pig4cloud.apikey.filter;

import com.pig4cloud.apikey.entity.SysApiKeyEntity;
import com.pig4cloud.apikey.open.OpenApiSigner;
import com.pig4cloud.apikey.service.ApiKeyService;
import com.pig4cloud.auth.service.IpRateLimiter;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.R;
import com.pig4cloud.common.store.StateStore;
import com.pig4cloud.common.util.IpRegionService;
import com.pig4cloud.common.util.ResponseWriter;
import com.pig4cloud.common.util.ServletUtils;
import com.pig4cloud.config.service.ConfigService;
import com.pig4cloud.log.entity.OpenApiLog;
import com.pig4cloud.log.service.OpenApiLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Open API密钥过滤器：拦截/api/open/**。
 * 鉴权模式auth-mode：simple(仅X-Api-Key) | hmac(强制签名) | both(默认，带签名头走签名，否则simple)。
 * hmac模式校验：X-Timestamp时间窗（默认±300秒）→ StateStore nonce防重放（窗口内同Key同Nonce仅一次）
 * → HMAC-SHA256签名常量时间比对。校验通过后按scopes授权、按分钟限流，调用明细异步落MongoDB。
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class OpenApiKeyFilter extends OncePerRequestFilter {

    private static final String OPEN_PREFIX = "/api/open/";
    private static final int MAX_NONCE_LENGTH = 64;

    private final ApiKeyService apiKeyService;
    private final IpRateLimiter ipRateLimiter;
    private final ResponseWriter responseWriter;
    private final StateStore stateStore;
    private final ConfigService configService;
    private final OpenApiLogService openApiLogService;
    private final IpRegionService ipRegionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(OPEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        long start = System.currentTimeMillis();
        String ip = ServletUtils.getClientIp(request);
        SysApiKeyEntity entity = null;
        Integer rejectCode = null;
        String rejectMessage = null;
        boolean reachedBusiness = false;
        try {
            entity = apiKeyService.validate(request.getHeader("X-Api-Key"));
            verifySignature(request, entity);
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
            reachedBusiness = true;
        } catch (BizException ex) {
            rejectCode = ex.getCode();
            rejectMessage = ex.getMessage();
            responseWriter.write(response, R.fail(ex.getCode(), ex.getMessage()));
        } catch (Exception ex) {
            rejectMessage = ex.getMessage();
            responseWriter.write(response, R.fail("Open API调用失败：" + ex.getMessage()));
        } finally {
            saveCallLog(request, entity, ip, start, reachedBusiness, rejectCode, rejectMessage);
        }
    }

    /**
     * 签名校验：simple模式直接放行；hmac模式强制验签；both模式带签名头才验签。
     * 校验顺序：时间窗（缩小回放区间）→ nonce防重放 → 签名比对
     */
    private void verifySignature(HttpServletRequest request, SysApiKeyEntity entity) {
        String mode = apiKeyService.authMode();
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Signature");
        boolean signedCall = timestamp != null || nonce != null || signature != null;
        if ("simple".equals(mode) || (!"hmac".equals(mode) && !signedCall)) {
            return;
        }
        if (timestamp == null || nonce == null || signature == null) {
            throw new BizException(401, "缺少签名请求头（X-Timestamp/X-Nonce/X-Signature）");
        }
        long windowSeconds = configService.getInt("openapi.sign-window-seconds", 300);
        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException ex) {
            throw new BizException(401, "X-Timestamp格式非法");
        }
        long skew = Math.abs(System.currentTimeMillis() - ts);
        if (skew > windowSeconds * 1000L) {
            throw new BizException(401, "请求时间戳超出允许窗口（±" + windowSeconds + "秒）");
        }
        // nonce防重放：窗口内同Key同Nonce只放行一次（memory单机有效，redis全集群有效）
        if (nonce.isBlank() || nonce.length() > MAX_NONCE_LENGTH) {
            throw new BizException(401, "X-Nonce非法（1~64字符）");
        }
        if (!stateStore.putIfAbsent("openapi:nonce:" + entity.getId() + ":" + nonce, "1",
                windowSeconds * 2 * 1000L)) {
            throw new BizException(401, "重复请求（nonce已被使用）");
        }
        if (entity.getApi_secret() == null || entity.getApi_secret().isBlank()) {
            throw new BizException(401, "该密钥未配置签名Secret，无法验签（请在管理端重新生成）");
        }
        String expected = OpenApiSigner.sign(entity.getApi_secret(), stringToSign(request, timestamp.trim(), nonce.trim()));
        if (!OpenApiSigner.safeEquals(expected, signature.trim())) {
            throw new BizException(401, "签名不匹配");
        }
    }

    /**
     * 签名原文：METHOD\npath[?按参数名排序的query]\napiKey\ntimestamp\nnonce
     */
    private String stringToSign(HttpServletRequest request, String timestamp, String nonce) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String canonicalQuery = query == null || query.isBlank() ? ""
                : Arrays.stream(query.split("&")).sorted().collect(Collectors.joining("&"));
        return request.getMethod().toUpperCase() + "\n"
                + path + (canonicalQuery.isEmpty() ? "" : "?" + canonicalQuery) + "\n"
                + request.getHeader("X-Api-Key") + "\n"
                + timestamp + "\n" + nonce;
    }

    /**
     * 调用明细异步落MongoDB（失败不影响调用）
     */
    private void saveCallLog(HttpServletRequest request, SysApiKeyEntity entity, String ip, long start,
                             boolean reachedBusiness, Integer rejectCode, String rejectMessage) {
        try {
            OpenApiLog logEntity = new OpenApiLog();
            if (entity != null) {
                logEntity.setKeyId(entity.getId());
                logEntity.setAppName(entity.getApp_name());
            }
            logEntity.setMethod(request.getMethod().toUpperCase());
            logEntity.setPath(request.getRequestURI());
            logEntity.setQuery(request.getQueryString());
            logEntity.setSuccess(reachedBusiness && rejectCode == null);
            logEntity.setCode(rejectCode);
            logEntity.setMessage(rejectMessage);
            logEntity.setIp(ip);
            logEntity.setRegion(ipRegionService.resolve(ip));
            logEntity.setCostMs(System.currentTimeMillis() - start);
            logEntity.setCreateTime(new Date());
            openApiLogService.saveAsync(logEntity);
        } catch (Exception ex) {
            log.warn("Open API调用日志组装失败: {}", ex.getMessage());
        }
    }
}
