package com.pig4cloud.dept.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.R;
import com.pig4cloud.dept.dto.DeptDto;
import com.pig4cloud.dept.entity.DeptEntity;
import com.pig4cloud.dept.mapper.DeptMapper;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.tenant.mapper.TenantMapper;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;

    @Override
    public R<List<DeptEntity>> getDeptTree(Integer tenantId) {
        // 租户筛选仅超管生效（普通用户被租户拦截器限制在本租户）
        Integer effective = UserContext.isSuperTenant() ? tenantId : null;
        return R.ok("获取数据成功", buildTree(deptMapper.selectListOrdered(effective)));
    }

    @Override
    @LogRecord(module = "部门管理", operation = "新增部门")
    public R<Void> createDept(DeptDto dto) {
        // 租户归属由服务端决定：超管可指定目标租户；其余取当前登录用户租户，平台操作归平台层
        Integer tenantId;
        if (dto.getTenantId() != null) {
            if (!UserContext.isSuperTenant()) {
                throw new BizException("仅平台管理员可指定部门所属租户");
            }
            if (tenantMapper.selectById(dto.getTenantId()) == null) {
                throw new BizException("目标租户不存在");
            }
            tenantId = dto.getTenantId();
        } else {
            tenantId = UserContext.getTenantId() == null ? 0 : UserContext.getTenantId();
        }
        checkParent(dto.getParentId(), null, tenantId);
        DeptEntity dept = new DeptEntity();
        dept.setParent_id(dto.getParentId() == null ? 0 : dto.getParentId());
        dept.setDept_name(dto.getDeptName());
        dept.setSort(dto.getSort() == null ? 0 : dto.getSort());
        dept.setTenant_id(tenantId);
        dept.setCreate_time(new Timestamp(System.currentTimeMillis()));
        deptMapper.insert(dept);
        return R.ok("创建成功", null);
    }

    @Override
    @LogRecord(module = "部门管理", operation = "编辑部门")
    public R<Void> updateDept(DeptDto dto) {
        DeptEntity exists = deptMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException("部门不存在");
        }
        // 部门租户归属建后不可改，父部门必须与本部门同租户
        checkParent(dto.getParentId(), dto.getId(), exists.getTenant_id());
        // 不允许把部门挪到自己或子孙下面（成环）
        if (selfAndDescendantIds(dto.getId()).contains(dto.getParentId())) {
            throw new BizException("不能将部门移动到自身或其子部门下");
        }
        DeptEntity dept = new DeptEntity();
        dept.setId(dto.getId());
        dept.setParent_id(dto.getParentId() == null ? 0 : dto.getParentId());
        dept.setDept_name(dto.getDeptName());
        dept.setSort(dto.getSort() == null ? 0 : dto.getSort());
        dept.setUpdate_time(new Timestamp(System.currentTimeMillis()));
        deptMapper.updateById(dept);
        return R.ok("更新成功", null);
    }

    @Override
    @LogRecord(module = "部门管理", operation = "删除部门")
    public R<Void> deleteDept(Integer id) {
        Set<Integer> ids = selfAndDescendantIds(id);
        if (ids.isEmpty()) {
            throw new BizException("部门不存在");
        }
        Long userCount = userMapper.selectCount(new QueryWrapper<com.pig4cloud.user.entity.UserEntity>()
                .in("dept_id", ids));
        if (userCount > 0) {
            throw new BizException("该部门（含子部门）下仍有" + userCount + "名用户，请先移出后再删除");
        }
        deptMapper.deleteByIds(ids);
        return R.ok("删除成功", null);
    }

    @Override
    public Set<Integer> selfAndDescendantIds(Integer deptId) {
        Set<Integer> result = new LinkedHashSet<>();
        if (deptId == null) {
            return result;
        }
        // 全量内存构建父子索引，部门表数据量小，避免递归查询
        List<DeptEntity> all = deptMapper.selectAllOrdered();
        Map<Integer, List<Integer>> childrenIndex = all.stream()
                .collect(Collectors.groupingBy(DeptEntity::getParent_id,
                        Collectors.mapping(DeptEntity::getId, Collectors.toList())));
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(deptId);
        while (!stack.isEmpty()) {
            Integer current = stack.pop();
            if (!result.add(current)) {
                continue;
            }
            List<Integer> children = childrenIndex.get(current);
            if (children != null) {
                children.forEach(stack::push);
            }
        }
        return result;
    }

    @Override
    public void deleteByTenantId(Integer tenantId) {
        deptMapper.delete(new QueryWrapper<DeptEntity>().eq("tenant_id", tenantId));
    }

    private void checkParent(Integer parentId, Integer selfId, Integer tenantId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new BizException("父部门不能是自身");
        }
        DeptEntity parent = deptMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException("父部门不存在");
        }
        // 父部门与部门必须同租户，防止超管把部门挂到别的租户的父级下破坏树隔离
        if (tenantId != null && !tenantId.equals(parent.getTenant_id())) {
            throw new BizException("父部门与部门不属于同一租户");
        }
    }

    private List<DeptEntity> buildTree(List<DeptEntity> all) {
        Map<Integer, List<DeptEntity>> childrenIndex = all.stream()
                .collect(Collectors.groupingBy(DeptEntity::getParent_id));
        return all.stream()
                .filter(dept -> dept.getParent_id() == null || dept.getParent_id() == 0)
                .peek(dept -> fillChildren(dept, childrenIndex))
                .collect(Collectors.toList());
    }

    private void fillChildren(DeptEntity dept, Map<Integer, List<DeptEntity>> childrenIndex) {
        List<DeptEntity> children = childrenIndex.get(dept.getId());
        if (children == null) {
            return;
        }
        children.forEach(child -> fillChildren(child, childrenIndex));
        dept.setChildren(children);
    }
}
