package com.pig4cloud.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pig4cloud.common.dto.BasePageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto extends BasePageQuery {

    /**
     * 关键字：用户名/姓名模糊匹配
     */
    private String username = "";

    /**
     * 部门筛选（含子部门）
     */
    @JsonProperty("dept_id")
    private Integer deptId;

    /**
     * 租户筛选（仅平台超管生效，普通用户被租户拦截器限制在本租户）
     */
    @JsonProperty("tenant_id")
    private Integer tenantId;
}
