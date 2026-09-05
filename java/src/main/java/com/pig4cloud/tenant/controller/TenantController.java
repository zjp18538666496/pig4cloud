package com.pig4cloud.tenant.controller;

import com.pig4cloud.common.annotation.Reauth;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.tenant.dto.TenantCreateDto;
import com.pig4cloud.tenant.dto.TenantDto;
import com.pig4cloud.tenant.dto.TenantUpdateDto;
import com.pig4cloud.tenant.entity.TenantEntity;
import com.pig4cloud.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户管理：仅平台超级管理员可用（tenant:manage权限点只授予super角色）
 */
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/getTenantLists")
    @PreAuthorize("hasAuthority('tenant:manage')")
    public R<PageResult<TenantEntity>> getTenantLists(@RequestBody TenantDto dto) {
        return tenantService.getTenantLists(dto);
    }

    @PostMapping("/createTenant")
    @PreAuthorize("hasAuthority('tenant:manage')")
    public R<Void> createTenant(@Valid @RequestBody TenantCreateDto dto) {
        return tenantService.createTenant(dto);
    }

    @PostMapping("/updateTenant")
    @PreAuthorize("hasAuthority('tenant:manage')")
    public R<Void> updateTenant(@Valid @RequestBody TenantUpdateDto dto) {
        return tenantService.updateTenant(dto);
    }

    /**
     * 公开接口：按租户编码查品牌（登录页展示，无需登录）；租户码为空返回平台默认
     */
    @GetMapping("/brand")
    public R<com.pig4cloud.tenant.dto.TenantBrandVO> brand(@RequestParam(required = false) String tenantCode) {
        return R.ok(tenantService.getTenantBrand(tenantCode));
    }

    @PostMapping("/delTenant")
    @Reauth
    @PreAuthorize("hasAuthority('tenant:manage')")
    public R<Void> delTenant(@RequestBody TenantUpdateDto dto) {
        return tenantService.deleteTenant(dto.getId());
    }
}
