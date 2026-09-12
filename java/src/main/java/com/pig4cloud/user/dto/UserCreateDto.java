package com.pig4cloud.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateDto {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,12}$", message = "请输入4到12位的用户名，支持字母和数字")
    /**
     * 登录IP白名单（逗号分隔，支持*前缀，空=不限制）
     */
    private String loginIpWhitelist;

    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码最长64位")
    private String password;

    /**
     * 部门id（可选，注册时为空）
     */
    @JsonProperty("dept_id")
    private Integer deptId;

    /**
     * 岗位id列表（可选）
     */
    @JsonProperty("post_ids")
    private java.util.List<Integer> postIds;

    /**
     * 昵称（可选，默认同账号；导入用）
     */
    private String name;

    /**
     * 手机号（可选，导入用）
     */
    private String mobile;

    /**
     * 邮箱（可选，导入用）
     */
    @jakarta.validation.constraints.Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 所属租户（仅平台超管可指定；不传时注册归默认租户1，其他管理员建在本租户）
     */
    @JsonProperty("tenant_id")
    private Integer tenantId;
}
