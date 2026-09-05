package com.pig4cloud.common.reauth;

import com.pig4cloud.common.annotation.Reauth;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.config.service.ConfigService;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 二次认证切面：拦截标注@Reauth的接口，要求请求头X-Reauth-Password为当前登录账号密码。
 * 防止会话被劫持/离开工位未锁屏时误触或恶意执行高危操作（删租户、重置他人密码等）。
 * reauth.enabled=false时全局关闭；提示信息固定不泄露原因，防暴力尝试
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ReauthAspect {

    public static final String REAUTH_HEADER = "X-Reauth-Password";

    private final ConfigService configService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Before("@annotation(reauth)")
    public void check(JoinPoint joinPoint, Reauth reauth) {
        if (!configService.getBool("reauth.enabled", true)) {
            return;
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BizException(403, "二次认证失败");
        }
        String password = attributes.getRequest().getHeader(REAUTH_HEADER);
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication() == null ? null
                : org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication().getName();
        boolean pass = false;
        if (password != null && !password.isBlank() && username != null) {
            UserEntity user = userMapper.selectUserByUsername(username);
            pass = user != null && passwordEncoder.matches(password, user.getPassword());
        }
        if (!pass) {
            log.warn("二次认证未通过：user={}, uri={}", username, attributes.getRequest().getRequestURI());
            throw new BizException(403, "二次认证失败：请携带当前登录账号密码（请求头X-Reauth-Password）");
        }
    }
}
