package com.pig4cloud.role.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色层级服务：子角色沿父链继承菜单/按钮权限（角色编码与数据权限不继承）。
 * MySQL 5.7无递归CTE，祖先链在Java侧展开；深度加防护避免脏数据成环时死循环。
 */
@Service
@RequiredArgsConstructor
public class RoleHierarchyService {

    /**
     * 祖先链最大深度防护
     */
    private static final int MAX_DEPTH = 20;

    private final RoleMapper roleMapper;

    /**
     * 计算有效角色id集合：自身角色 ∪ 沿parent_id向上的全部祖先角色。
     * 角色查询走租户拦截器（super全量），祖先与角色同租户（建链时已校验），故在本租户范围内即可找齐。
     */
    public List<Integer> effectiveRoleIds(List<RoleEntity> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }
        Map<Integer, RoleEntity> roleById = new HashMap<>();
        for (RoleEntity role : roleMapper.selectList(new QueryWrapper<>())) {
            roleById.put(role.getId(), role);
        }
        Set<Integer> effectiveIds = new LinkedHashSet<>();
        for (RoleEntity role : userRoles) {
            Integer cursor = role.getId();
            int depth = 0;
            while (cursor != null && effectiveIds.add(cursor) && depth++ < MAX_DEPTH) {
                RoleEntity current = roleById.get(cursor);
                Integer parentId = current == null ? null : current.getParent_id();
                cursor = (parentId == null || parentId == 0) ? null : parentId;
            }
        }
        return new ArrayList<>(effectiveIds);
    }

    /**
     * 收集指定角色的全部子孙角色id（不含自身），供上级角色选择防成环校验
     */
    public Set<Integer> descendantIds(Integer roleId) {
        Map<Integer, List<Integer>> childrenIndex = new HashMap<>();
        for (RoleEntity role : roleMapper.selectList(new QueryWrapper<>())) {
            childrenIndex.computeIfAbsent(role.getParent_id() == null ? 0 : role.getParent_id(),
                    key -> new ArrayList<>()).add(role.getId());
        }
        Set<Integer> result = new LinkedHashSet<>();
        List<Integer> stack = new ArrayList<>(childrenIndex.getOrDefault(roleId, List.of()));
        while (!stack.isEmpty()) {
            Integer current = stack.remove(stack.size() - 1);
            if (current == null || !result.add(current)) {
                continue;
            }
            stack.addAll(childrenIndex.getOrDefault(current, List.of()));
        }
        return result;
    }
}
