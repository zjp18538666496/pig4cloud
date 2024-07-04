package com.pig4cloud.controller;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dto.RoleDto;
import com.pig4cloud.entity.RoleEntity;
import com.pig4cloud.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RestController
@RequestMapping("/role")
public class RoleController {
    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping("/createRole")
    public Response createRole(@RequestBody RoleEntity roleEntity) {
        return roleService.createRole(roleEntity);
    }

    @PostMapping("/delRole")
    public Response deleteRole(@RequestBody RoleEntity roleEntity) {
        return roleService.deleteRole(roleEntity);
    }

    @PostMapping("/updateRole")
    public Response updateRole(@RequestBody RoleEntity roleEntity) {
        return roleService.updateRole(roleEntity);
    }

    @PostMapping("/getRoleLists")
    public Response getRoleLists(@RequestBody RoleDto roleDto) {
        return roleService.getRoleLists(roleDto);
    }
}
