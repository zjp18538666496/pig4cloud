package com.pig4cloud.util.auth;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dao.impl.ResponseImpl;
import com.pig4cloud.response.WriteResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;

import java.util.Collections;

/**
 * 自定义token验证过滤器，验证成功后将用户信息放入SecurityContext上下文
 */
public class JwtAuthenticationFilter extends BasicAuthenticationFilter {
    public JwtAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws java.io.IOException {
        try {
            //获取请求头中的token
            String jwtToken = request.getHeader("token");
            if (!StringUtils.hasLength(jwtToken)) {
                //token不存在，交给其他过滤器处理
                filterChain.doFilter(request, response);
                return; //结束方法
            }

            //过滤器中无法初始化Bean组件，使用上下文获取
            JwtUtils jwtUtils = SpringContextUtils.getBean("jwtUtils");
            if (jwtUtils == null) {
                throw new RuntimeException("未找到 JwtUtils bean");
            }

            //解析jwt令牌
            Claims claims;
            try {
                claims = jwtUtils.parseJwt(jwtToken);
            }catch (ExpiredJwtException ex) {
                // Token过期，返回错误信息
                Response res = new ResponseImpl(401, "Token 过期", ex.getMessage());
                new WriteResponse(response, res);
                return;
            }  catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            //获取用户信息
            String username = (String) claims.get("username"); //用户名
            String authorityString = (String) claims.get("authorityString"); //权限信息

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    Collections.singleton(new SimpleGrantedAuthority(authorityString))
            );
            //将用户信息放入SecurityContext上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            //过滤器中抛出的异常无法被全局异常处理器捕获，直接返回错误结果
            Response res = new ResponseImpl(403, ex.getMessage(), null);
            new WriteResponse(response, res);
        }
    }

}
