package com.pig4cloud.controller;

import com.pig4cloud.dao.Response;
import com.pig4cloud.dto.MenuDto;
import com.pig4cloud.entity.MenuEntity;
import com.pig4cloud.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RestController
@RequestMapping("/menu")
public class MenuController {
    private final MenuService menuService;

    @Autowired
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping("/createMenu")
    public Response createMenu(@RequestBody MenuEntity MenuEntity) {
        return menuService.createMenu(MenuEntity);
    }

    @PostMapping("/delMenu")
    public Response deleteMenu(@RequestBody MenuEntity MenuEntity) {
        return menuService.deleteMenu(MenuEntity);
    }

    @PostMapping("/updateMenu")
    public Response updateMenu(@RequestBody MenuEntity MenuEntity) {
        return menuService.updateMenu(MenuEntity);
    }

    @PostMapping("/getMenuLists")
    public Response getMenuLists(@RequestBody MenuDto MenuDto) {
        return menuService.getMenuLists(MenuDto);
    }

    @PostMapping("/selectMenuLists")
    public Response selectMenuLists(@RequestBody MenuDto MenuDto) {
        return menuService.selectMenuLists(MenuDto);
    }
}
