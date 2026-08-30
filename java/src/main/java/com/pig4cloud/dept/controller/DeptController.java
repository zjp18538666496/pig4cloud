package com.pig4cloud.dept.controller;

import com.pig4cloud.common.result.R;
import com.pig4cloud.dept.dto.DeptDto;
import com.pig4cloud.dept.entity.DeptEntity;
import com.pig4cloud.dept.service.DeptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门管理：树形组织架构，仅部门内的用户可见数据受数据权限控制
 */
@RestController
@RequestMapping("/api/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    /**
     * 部门树（登录即可，用户编辑弹窗等部门选择器需要）；超管可传tenant_id只看指定租户
     */
    @PostMapping("/getDeptTree")
    public R<List<DeptEntity>> getDeptTree(@RequestBody(required = false) DeptDto dto) {
        return deptService.getDeptTree(dto == null ? null : dto.getTenantId());
    }

    @PostMapping("/createDept")
    @PreAuthorize("hasAuthority('dept:write')")
    public R<Void> createDept(@Valid @RequestBody DeptDto dto) {
        return deptService.createDept(dto);
    }

    @PostMapping("/updateDept")
    @PreAuthorize("hasAuthority('dept:write')")
    public R<Void> updateDept(@Valid @RequestBody DeptDto dto) {
        return deptService.updateDept(dto);
    }

    @PostMapping("/delDept")
    @PreAuthorize("hasAuthority('dept:remove')")
    public R<Void> delDept(@RequestBody DeptDto dto) {
        return deptService.deleteDept(dto.getId());
    }
}
