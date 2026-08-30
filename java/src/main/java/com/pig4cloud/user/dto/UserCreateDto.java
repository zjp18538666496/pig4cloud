package com.pig4cloud.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    /**
     * 部门id（可选，注册时为空）
     */
    @JsonProperty("dept_id")
    private Integer deptId;

    /**
     * 所属租户（仅平台超管可指定；不传时注册归默认租户1，其他管理员建在本租户）
     */
    @JsonProperty("tenant_id")
    private Integer tenantId;
}
