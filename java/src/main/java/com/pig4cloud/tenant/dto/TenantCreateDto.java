package com.pig4cloud.tenant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class TenantCreateDto {

    @NotBlank(message = "租户编码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{2,32}$", message = "租户编码为2到32位字母、数字、下划线或中划线")
    @JsonProperty("tenant_code")
    private String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 64, message = "租户名称最长64个字符")
    @JsonProperty("tenant_name")
    private String tenantName;

    /**
     * 绑定套餐（决定租户管理员可用菜单）
     */
    @NotNull(message = "请选择租户套餐")
    @JsonProperty("package_id")
    private Integer packageId;

    /**
     * 过期时间（空为永不过期）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("expire_time")
    private Date expireTime;

    /**
     * 用户数上限（空为不限制）
     */
    @JsonProperty("user_limit")
    private Integer userLimit;
}
