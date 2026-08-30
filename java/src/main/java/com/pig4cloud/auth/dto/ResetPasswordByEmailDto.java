package com.pig4cloud.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 通过邮箱验证码重置密码请求
 */
@Getter
@Setter
public class ResetPasswordByEmailDto {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>~`\\\\/\\[\\]\\-_+=;']{4,18}$", message = "请输入4到18位的密码，支持字母、数字和特殊字符")
    private String newPassword;
}
