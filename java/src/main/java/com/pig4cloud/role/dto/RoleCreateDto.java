package com.pig4cloud.role.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoleCreateDto {

    @NotBlank(message = "角色名称不能为空")
    @JsonProperty("role_name")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @JsonProperty("role_code")
    private String roleCode;

    private String description;

    /**
     * 关联菜单id列表，为空表示不关联菜单
     */
    @JsonProperty("menu_codes")
    private List<String> menuCodes;
}
