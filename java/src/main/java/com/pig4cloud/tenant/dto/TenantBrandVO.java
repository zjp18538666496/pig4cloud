package com.pig4cloud.tenant.dto;

/**
 * 租户品牌信息（公开接口返回，登录页与登录后侧边栏展示）
 */
public record TenantBrandVO(String tenantCode, String name, String logo, String color) {

    /**
     * 平台默认品牌
     */
    public static TenantBrandVO defaultBrand() {
        return new TenantBrandVO(null, "PIGX ADMIN", null, null);
    }
}
