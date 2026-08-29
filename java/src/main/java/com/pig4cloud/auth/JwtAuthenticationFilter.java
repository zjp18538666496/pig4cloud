package com.pig4cloud.auth;

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

/**
 * token校验过滤器：只负责解析token并填充SecurityContext，鉴权交给授权层
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String REFRESH_TOKEN_PATH = "/api/auth/refresh-token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final ResponseWriter responseWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            SecurityContextHolder.clearContext();
            String jwtToken = request.getHeader("authorization");

            // 无token的请求交给授权层判断（公开接口直接放行，受限接口由AuthEntryPointHandler返回401）
            if (!StringUtils.hasText(jwtToken) || !jwtToken.startsWith(BEARER_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }
            // 刷新令牌接口携带的是refresh token，不校验access token
            if (REFRESH_TOKEN_PATH.equals(request.getRequestURI())) {
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = jwtUtils.parseJwt(jwtToken.substring(BEARER_PREFIX.length()));
            String username = claims.get("username", String.class);
            // 登录时多个角色以逗号合并存入authorityString，这里拆回多个权限
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
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            // 认证失败的响应保持HTTP 200+响应体code=401，前端据此走刷新token流程
            responseWriter.write(response, R.fail(R.UNAUTHORIZED, "Token 过期"));
        } catch (JwtException | IllegalArgumentException ex) {
            responseWriter.write(response, R.fail(R.UNAUTHORIZED, "Token 无效"));
        } catch (Exception ex) {
            log.error("token认证处理异常", ex);
            responseWriter.write(response, R.fail(403, "认证处理异常"));
        }
    }
}
