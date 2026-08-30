package com.pig4cloud.auth;

import com.pig4cloud.auth.online.OnlineUserStore;
import com.pig4cloud.auth.online.TokenBlacklist;
import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.common.result.R;
import com.pig4cloud.common.util.ResponseWriter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * token校验过滤器：解析token、校验黑名单、填充SecurityContext与租户上下文；鉴权交给授权层
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String REFRESH_TOKEN_PATH = "/api/auth/refresh-token";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 公开业务接口：不解析请求携带的旧token、不设置租户上下文。
     * 否则旧token的租户上下文会被租户拦截器拼进登录/注册的用户查询（如用A租户的有效token登录B租户账号会查不到用户）
     */
    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/auth/login", "/api/auth/captcha", "/api/auth/sendResetCode",
            "/api/auth/resetPasswordByEmail", "/api/user/register");

    private final JwtUtils jwtUtils;
    private final ResponseWriter responseWriter;
    private final TokenBlacklist tokenBlacklist;
    private final OnlineUserStore onlineUserStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            SecurityContextHolder.clearContext();
            // 刷新令牌接口携带的是refresh token，公开业务接口不消费token，均不校验access token
            if (REFRESH_TOKEN_PATH.equals(request.getRequestURI())
                    || PUBLIC_AUTH_PATHS.contains(request.getRequestURI())) {
                filterChain.doFilter(request, response);
                return;
            }
            String jwtToken = request.getHeader("authorization");

            // 无token的请求交给授权层判断（公开接口直接放行，受限接口由AuthEntryPointHandler返回401）
            if (!StringUtils.hasText(jwtToken) || !jwtToken.startsWith(BEARER_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = jwtUtils.parseJwt(jwtToken.substring(BEARER_PREFIX.length()));
            // 登出/强退/改密的token已进黑名单，拒绝访问
            String jti = claims.getId();
            if (tokenBlacklist.isRevoked(jti)) {
                responseWriter.write(response, R.fail(R.UNAUTHORIZED, "登录状态已失效，请重新登录"));
                return;
            }
            String username = claims.get("username", String.class);
            // 登录时多个角色/权限点以逗号合并存入authorityString，这里拆回多个权限
            String authorityString = claims.get("authorityString", String.class);
            var authorities = authorityString == null || authorityString.isBlank()
                    ? Collections.<SimpleGrantedAuthority>emptyList()
                    : Arrays.stream(authorityString.split(","))
                            .filter(StringUtils::hasText)
                            .map(SimpleGrantedAuthority::new)
                            .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 租户上下文：供MyBatis-Plus租户拦截器使用；super角色跨租户
            Integer tenantId = claims.get("tenantId", Integer.class);
            boolean isSuper = authorities.stream().anyMatch(a -> "super".equals(a.getAuthority()));
            UserContext.set(tenantId, isSuper);

            // 刷新在线会话活跃时间（重启后会话已清空，跳过不影响认证）
            onlineUserStore.touchAccess(jti);

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            // 认证失败的响应保持HTTP 200+响应体code=401，前端据此走刷新token流程
            responseWriter.write(response, R.fail(R.UNAUTHORIZED, "Token 过期"));
        } catch (JwtException | IllegalArgumentException ex) {
            responseWriter.write(response, R.fail(R.UNAUTHORIZED, "Token 无效"));
        } catch (Exception ex) {
            log.error("token认证处理异常", ex);
            responseWriter.write(response, R.fail(403, "认证处理异常"));
        } finally {
            UserContext.clear();
        }
    }
}
