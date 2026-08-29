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

    @JsonProperty("menu_codes")
    private List<String> menuCodes;
}
