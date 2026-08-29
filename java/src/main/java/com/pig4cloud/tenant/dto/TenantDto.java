package com.pig4cloud.tenant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pig4cloud.common.dto.BasePageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantDto extends BasePageQuery {

    /**
     * 租户名称，模糊匹配
     */
    @JsonProperty("tenant_name")
    private String tenantName = "";
}
