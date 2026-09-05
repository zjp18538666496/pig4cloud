package com.pig4cloud.tenant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class TenantUpdateDto {

    @NotNull(message = "租户id不能为空")
    private Integer id;

    @Size(max = 64, message = "租户名称最长64个字符")
    @JsonProperty("tenant_name")
    private String tenantName;

    /**
     * 状态(0禁用1启用)
     */
    private String status;

    /**
     * 套餐（变更时同步重绑租户管理员角色菜单）
     */
    @JsonProperty("package_id")
    private Integer packageId;

    /**
     * 过期时间（空为永不过期）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("expire_time")
    private Date expireTime;

    /**
     * 用户数上限（空为不限制）
     */
    @JsonProperty("user_limit")
    private Integer userLimit;

    /**
     * 品牌名称（登录页/侧边栏展示，空则用tenant_name）
     */
    @Size(max = 64, message = "品牌名称最长64个字符")
    @JsonProperty("brand_name")
    private String brandName;

    /**
     * 品牌logo地址
     */
    @Size(max = 255, message = "品牌logo最长255个字符")
    @JsonProperty("brand_logo")
    private String brandLogo;

    /**
     * 品牌主题色
     */
    @Size(max = 16, message = "品牌主题色最长16个字符")
    @JsonProperty("brand_color")
    private String brandColor;
}
