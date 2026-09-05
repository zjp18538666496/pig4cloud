package com.pig4cloud.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码（sys_config开启captcha时必填，服务端校验）
     */
    private String captchaId;

    private String captchaCode;

    /**
     * 两步认证动态码（已绑定TOTP的用户必填；也接受备用恢复码）
     */
    private String totpCode;

    /**
     * 短信验证码（短信登录模式使用）
     */
    private String smsCode;
}
