package com.pig4cloud.dept.service;

import com.pig4cloud.common.result.R;
import com.pig4cloud.dept.dto.DeptDto;
import com.pig4cloud.dept.entity.DeptEntity;

import java.util.List;
import java.util.Set;

public interface DeptService {

    /**
     * 全量部门树（租户拦截器自动隔离；超管可传tenantId只看指定租户，不传看全部）
     */
    R<List<DeptEntity>> getDeptTree(Integer tenantId);

    R<Void> createDept(DeptDto dto);

    R<Void> updateDept(DeptDto dto);

    /**
     * 级联删除部门及其子部门；部门下仍有用户时拒绝删除
     */
    R<Void> deleteDept(Integer id);

    /**
     * 指定部门及其全部子孙部门id集合（含自身；部门不存在或为空返回空集），供数据权限过滤
     */
    Set<Integer> selfAndDescendantIds(Integer deptId);
}
