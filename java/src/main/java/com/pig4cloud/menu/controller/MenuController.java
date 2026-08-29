package com.pig4cloud.menu.controller;

import com.pig4cloud.common.result.R;
import com.pig4cloud.menu.dto.MenuDto;
import com.pig4cloud.menu.dto.MenuSelectDto;
import com.pig4cloud.menu.entity.MenuEntity;
import com.pig4cloud.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping("/createMenu")
    @PreAuthorize("hasAuthority('menu:write')")
    public R<Void> createMenu(@RequestBody MenuEntity menuEntity) {
        return menuService.createMenu(menuEntity);
    }

    @PostMapping("/delMenu")
    @PreAuthorize("hasAuthority('menu:remove')")
    public R<Void> deleteMenu(@RequestBody MenuEntity menuEntity) {
        return menuService.deleteMenu(menuEntity);
    }

    @PostMapping("/updateMenu")
    @PreAuthorize("hasAuthority('menu:write')")
    public R<Void> updateMenu(@RequestBody MenuEntity menuEntity) {
        return menuService.updateMenu(menuEntity);
    }

    @PostMapping("/getMenuLists")
    public R<List<MenuEntity>> getMenuLists(@RequestBody MenuDto menuDto) {
        return menuService.getMenuLists(menuDto);
    }

    @PostMapping("/selectMenuLists")
    public R<?> selectMenuLists(@RequestBody MenuSelectDto dto) {
        return menuService.selectMenuLists(dto);
    }
}
