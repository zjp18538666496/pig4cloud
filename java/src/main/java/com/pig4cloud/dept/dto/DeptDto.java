package com.pig4cloud.dept.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 部门创建/编辑请求
 */
@Getter
@Setter
public class DeptDto {

    /**
     * 编辑时必传
     */
    private Integer id;

    /**
     * 父部门id（0为根）
     */
    @JsonProperty("parent_id")
    private Integer parentId = 0;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 64, message = "部门名称最长64个字符")
    @JsonProperty("dept_name")
    private String deptName;

    private Integer sort = 0;

    /**
     * 所属租户（仅平台超管可指定；不传时归当前登录用户租户，平台操作归平台层）
     */
    @JsonProperty("tenant_id")
    private Integer tenantId;
}
