package com.pig4cloud.tenant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

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
}
