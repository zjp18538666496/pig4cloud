package com.pig4cloud.search.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.common.result.R;
import com.pig4cloud.menu.dto.MenuSelectDto;
import com.pig4cloud.menu.entity.MenuEntity;
import com.pig4cloud.menu.service.MenuService;
import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.tenant.entity.TenantEntity;
import com.pig4cloud.tenant.mapper.TenantMapper;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 顶栏全局搜索：模糊匹配当前用户可见的菜单/用户/角色/租户（登录即可，结果范围随权限收敛）
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private static final int LIMIT = 8;

    private final MenuService menuService;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final TenantMapper tenantMapper;

    @GetMapping("/all")
    public R<Map<String, Object>> search(@RequestParam("keyword") String keyword) {
        Map<String, Object> result = new HashMap<>();
        if (!StringUtils.hasText(keyword)) {
            result.put("menus", List.of());
            result.put("users", List.of());
            result.put("roles", List.of());
            result.put("tenants", List.of());
            return R.ok("请求成功", result);
        }
        String kw = keyword.trim();

        // 菜单：当前用户可见范围内的页面菜单
        var menuR = menuService.selectMenuLists(buildFlatMenuQuery());
        List<Map<String, Object>> menus = ((List<MenuEntity>) menuR.getData()).stream()
                .filter(menu -> "1".equals(menu.getType()))
                .filter(menu -> contains(menu.getMenu_name(), kw) || contains(menu.getRoute(), kw))
                .limit(LIMIT)
                .map(menu -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", menu.getMenu_name());
                    item.put("route", menu.getRoute());
                    return item;
                })
                .toList();
        result.put("menus", menus);

        // 用户/角色：走租户拦截器自动隔离
        List<Map<String, Object>> users = userMapper.selectList(new QueryWrapper<UserEntity>()
                        .and(wrapper -> wrapper.like("username", kw).or().like("name", kw))
                        .last("LIMIT " + LIMIT))
                .stream().map(user -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("username", user.getUsername());
                    item.put("name", user.getName());
                    item.put("type", "user");
                    return item;
                }).toList();
        result.put("users", users);

        List<Map<String, Object>> roles = roleMapper.selectList(new QueryWrapper<RoleEntity>()
                        .and(wrapper -> wrapper.like("role_name", kw).or().like("role_code", kw))
                        .last("LIMIT " + LIMIT))
                .stream().map(role -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("roleName", role.getRole_name());
                    item.put("roleCode", role.getRole_code());
                    item.put("type", "role");
                    return item;
                }).toList();
        result.put("roles", roles);

        // 租户：随sys_menu一样为平台表，用户管理/租户管理有权限的人才有意义，这里仅super场景由页面入口控制
        List<Map<String, Object>> tenants = tenantMapper.selectList(new QueryWrapper<TenantEntity>()
                        .and(wrapper -> wrapper.like("tenant_name", kw).or().like("tenant_code", kw))
                        .last("LIMIT " + LIMIT))
                .stream().map(tenant -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("tenantName", tenant.getTenant_name());
                    item.put("tenantCode", tenant.getTenant_code());
                    item.put("tenantId", tenant.getId());
                    item.put("type", "tenant");
                    return item;
                }).toList();
        result.put("tenants", tenants);

        return R.ok("请求成功", result);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private MenuSelectDto buildFlatMenuQuery() {
        MenuSelectDto selectDto = new MenuSelectDto();
        selectDto.setMenuType("flatMenu");
        return selectDto;
    }
}
