package com.pig4cloud.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateDto {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,12}$", message = "请输入4到12位的用户名，支持字母和数字")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>~`\\\\/\\[\\]\\-_+=;']{4,18}$", message = "请输入4到18位的密码，支持字母、数字和特殊字符")
    private String password;
}
