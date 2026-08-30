package com.pig4cloud.tenant.controller;

import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.tenant.dto.TenantPackageDto;
import com.pig4cloud.tenant.entity.TenantPackageEntity;
import com.pig4cloud.tenant.service.TenantPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租户套餐管理：仅平台超级管理员可用
 */
@RestController
@RequestMapping("/api/tenant/package")
@RequiredArgsConstructor
public class TenantPackageController {

    private final TenantPackageService packageService;

    @PostMapping("/getPackageLists")
    @PreAuthorize("hasAuthority('package:write')")
    public R<PageResult<TenantPackageEntity>> getPackageLists(@RequestBody TenantPackageService.PackageQueryDto dto) {
        return packageService.getPackageLists(dto);
    }

    /**
     * 启用中的套餐下拉（开通租户时选择；tenant:manage权限即可）
     */
    @PostMapping("/getEnabledPackages")
    @PreAuthorize("hasAuthority('tenant:manage')")
    public R<List<TenantPackageEntity>> getEnabledPackages() {
        return packageService.getEnabledPackages();
    }

    @PostMapping("/createPackage")
    @PreAuthorize("hasAuthority('package:write')")
    public R<Void> createPackage(@Valid @RequestBody TenantPackageDto dto) {
        return packageService.createPackage(dto);
    }

    @PostMapping("/updatePackage")
    @PreAuthorize("hasAuthority('package:write')")
    public R<Void> updatePackage(@Valid @RequestBody TenantPackageDto dto) {
        return packageService.updatePackage(dto);
    }

    @PostMapping("/delPackage")
    @PreAuthorize("hasAuthority('package:remove')")
    public R<Void> delPackage(@RequestBody TenantPackageDto dto) {
        return packageService.deletePackage(dto.getId());
    }
}
