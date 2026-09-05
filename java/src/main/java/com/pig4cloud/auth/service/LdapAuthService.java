package com.pig4cloud.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

/**
 * LDAP/AD登录认证（app.ldap.enabled=true时启用）：按配置的用户名属性构造过滤器，
 * 在spring.ldap.base下搜索并校验密码。sys_user.auth_source='ldap'的账号登录时走此认证。
 * 联调提示：需配置spring.ldap.urls/base（AD用userPrincipalName属性），
 * 未接真实LDAP服务器前此路径不可用，代码按spring-ldap标准用法实现
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.ldap.enabled", havingValue = "true")
public class LdapAuthService {

    private final LdapTemplate ldapTemplate;

    @Value("${app.ldap.username-attribute:uid}")
    private String usernameAttribute;

    public LdapAuthService(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    /**
     * 认证成功静默返回，失败抛BadCredentialsException（登录侧统一按认证失败处理）
     */
    public void authenticate(String username, String password) {
        try {
            // spring-ldap 3.x：authenticate为void，找不到用户或密码错误均抛DataAccessException
            ldapTemplate.authenticate(
                    LdapQueryBuilder.query().where(usernameAttribute).is(username), password);
            log.info("LDAP认证成功: {}", username);
        } catch (BadCredentialsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("LDAP认证失败[{}]: {}", username, ex.getMessage());
            throw new BadCredentialsException("LDAP认证失败：用户名或密码不正确");
        }
    }
}
