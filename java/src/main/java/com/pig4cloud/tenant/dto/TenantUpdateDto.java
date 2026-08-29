package com.pig4cloud.tenant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantUpdateDto {

    @NotNull(message = "租户id不能为空")
    private Integer id;

    @Size(max = 64, message = "租户名称最长64个字符")
    @JsonProperty("tenant_name")
    private String tenantName;

    /**
     * 状态(0禁用1启用)
     */
    private String status;
}
