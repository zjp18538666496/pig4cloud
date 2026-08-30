package com.pig4cloud.tenant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 租户套餐创建/编辑请求
 */
@Getter
@Setter
public class TenantPackageDto {

    /**
     * 编辑时必传
     */
    private Integer id;

    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 64, message = "套餐名称最长64个字符")
    @JsonProperty("package_name")
    private String packageName;

    /**
     * 关联菜单id列表（树形勾选）
     */
    @JsonProperty("menu_codes")
    private List<String> menuCodes;

    /**
     * 状态(0停用1启用)
     */
    private String status = "1";

    @Size(max = 255, message = "备注最长255个字符")
    private String remark;
}
