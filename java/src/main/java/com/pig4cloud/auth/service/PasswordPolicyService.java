package com.pig4cloud.auth.service;

import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.config.service.ConfigService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 密码策略：最小长度/复杂度要求走sys_config（pwd.min-length/pwd.require-complex），
 * 创建用户/注册/改密/重置统一校验，改配置即时生效
 */
@Service
public class PasswordPolicyService {

    private final ConfigService configService;

    public PasswordPolicyService(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * 校验明文密码是否符合当前策略，不符合抛业务异常
     */
    public void validate(String rawPassword) {
        int minLength = configService.getInt("pwd.min-length", 8);
        if (rawPassword == null || rawPassword.length() < minLength) {
            throw new BizException("密码长度不能少于" + minLength + "位");
        }
        if (rawPassword.length() > 64) {
            throw new BizException("密码长度不能超过64位");
        }
        if (configService.getBool("pwd.require-complex", false)
                && !(rawPassword.matches(".*[A-Za-z].*") && rawPassword.matches(".*\\d.*"))) {
            throw new BizException("密码必须同时包含字母和数字");
        }
    }

    /**
     * 当前策略（公开接口供前端表单提示）
     */
    public Map<String, Object> policy() {
        return Map.of(
                "minLength", configService.getInt("pwd.min-length", 8),
                "requireComplex", configService.getBool("pwd.require-complex", false));
    }

    /**
     * 管理员重置/初始密码是否要求首次登录强制修改
     */
    public boolean forceChangeOnReset() {
        return configService.getBool("pwd.force-change-initial", true);
    }
}
