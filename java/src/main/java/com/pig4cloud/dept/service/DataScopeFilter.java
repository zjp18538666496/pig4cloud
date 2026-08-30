package com.pig4cloud.dept.service;

import java.util.Set;

/**
 * 数据权限过滤条件（传给Mapper拼SQL）：deptIds为可见部门id集，selfId为本人用户id；
 * 两者均为空表示不限制（本租户全部）
 */
public record DataScopeFilter(Set<Integer> deptIds, Long selfId) {

    public static DataScopeFilter all() {
        return new DataScopeFilter(null, null);
    }

    public boolean isAll() {
        return (deptIds == null || deptIds.isEmpty()) && selfId == null;
    }
}
