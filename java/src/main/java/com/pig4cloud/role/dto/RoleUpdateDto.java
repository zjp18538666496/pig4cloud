package com.pig4cloud.role.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoleUpdateDto {

    @NotNull(message = "角色id不能为空")
    private Integer id;

    @JsonProperty("role_name")
    private String roleName;

    @JsonProperty("role_code")
    private String roleCode;

    private String description;

    /**
     * 数据权限(1本租户全部2本部门及以下3仅本人)
     */
    @JsonProperty("data_scope")
    private String dataScope;

    /**
     * 上级角色id(0为顶级)
     */
    @JsonProperty("parent_id")
    private Integer parentId;

    @JsonProperty("menu_codes")
    private List<String> menuCodes;
}
