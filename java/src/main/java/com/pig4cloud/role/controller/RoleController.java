package com.pig4cloud.role.controller;

import com.pig4cloud.common.result.R;
import com.pig4cloud.role.dto.RoleCreateDto;
import com.pig4cloud.role.dto.RoleDeleteDto;
import com.pig4cloud.role.dto.RoleDto;
import com.pig4cloud.role.dto.RoleUpdateDto;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.role.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping("/createRole")
    @LogRecord(module = "角色管理", operation = "新增角色")
    @PreAuthorize("hasAuthority('role:write')")
    public R<Void> createRole(@Valid @RequestBody RoleCreateDto dto) {
        return roleService.createRole(dto);
    }

    @PostMapping("/delRole")
    @LogRecord(module = "角色管理", operation = "删除角色")
    @PreAuthorize("hasAuthority('role:remove')")
    public R<Void> deleteRole(@Valid @RequestBody RoleDeleteDto dto) {
        return roleService.deleteRole(dto);
    }

    @PostMapping("/updateRole")
    @LogRecord(module = "角色管理", operation = "编辑角色")
    @PreAuthorize("hasAuthority('role:write')")
    public R<Void> updateRole(@Valid @RequestBody RoleUpdateDto dto) {
        return roleService.updateRole(dto);
    }

    @PostMapping("/getRoleLists")
    public R<?> getRoleLists(@RequestBody RoleDto roleDto) {
        return roleService.getRoleLists(roleDto);
    }
}
