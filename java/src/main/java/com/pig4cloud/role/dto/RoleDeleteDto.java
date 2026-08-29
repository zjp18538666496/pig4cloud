package com.pig4cloud.role.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleDeleteDto {

    @NotBlank(message = "角色编码不能为空")
    @JsonProperty("role_code")
    private String roleCode;
}
