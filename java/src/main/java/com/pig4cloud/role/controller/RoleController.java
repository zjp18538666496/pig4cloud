package com.pig4cloud.role.controller;

import com.pig4cloud.common.result.R;
import com.pig4cloud.role.dto.RoleCreateDto;
import com.pig4cloud.role.dto.RoleDeleteDto;
import com.pig4cloud.role.dto.RoleDto;
import com.pig4cloud.role.dto.RoleUpdateDto;
import com.pig4cloud.role.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public R<Void> createRole(@Valid @RequestBody RoleCreateDto dto) {
        return roleService.createRole(dto);
    }

    @PostMapping("/delRole")
    public R<Void> deleteRole(@Valid @RequestBody RoleDeleteDto dto) {
        return roleService.deleteRole(dto);
    }

    @PostMapping("/updateRole")
    public R<Void> updateRole(@Valid @RequestBody RoleUpdateDto dto) {
        return roleService.updateRole(dto);
    }

    @PostMapping("/getRoleLists")
    public R<?> getRoleLists(@RequestBody RoleDto roleDto) {
        return roleService.getRoleLists(roleDto);
    }
}
