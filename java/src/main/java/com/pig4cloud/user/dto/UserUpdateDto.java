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
    /**
     * 登录IP白名单（逗号分隔，支持*前缀，空=不限制）
     */
    private String loginIpWhitelist;

    private Long id;

    private String username;

    private String name;

    private String mobile;

    private String email;

    /**
     * 部门id（仅管理员可改，null表示清空部门归属）
     */
    @JsonProperty("dept_id")
    private Integer deptId;

    /**
     * 岗位id列表（仅管理员可改；null表示不修改）
     */
    @JsonProperty("post_ids")
    private List<Integer> postIds;

    /**
     * 角色编码列表，为空表示清空用户角色
     */
    @JsonProperty("role_codes")
    private List<String> roleCodes;
}
