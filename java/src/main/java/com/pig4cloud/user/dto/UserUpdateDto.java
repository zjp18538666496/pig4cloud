package com.pig4cloud.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserUpdateDto {

    @NotNull(message = "用户id不能为空")
    private Long id;

    private String username;

    private String name;

    private String mobile;

    private String email;

    /**
     * 角色编码列表，为空表示清空用户角色
     */
    @JsonProperty("role_codes")
    private List<String> roleCodes;
}
