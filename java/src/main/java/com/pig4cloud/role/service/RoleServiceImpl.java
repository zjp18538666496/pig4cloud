package com.pig4cloud.role.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.context.UserContext;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.auth.online.SessionKickService;
import com.pig4cloud.menu.mapper.MenuMapper;
import com.pig4cloud.role.dto.RoleCreateDto;
import com.pig4cloud.role.dto.RoleDeleteDto;
import com.pig4cloud.role.dto.RoleDto;
import com.pig4cloud.role.dto.RoleUpdateDto;
import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final RoleHierarchyService roleHierarchyService;
    private final SessionKickService sessionKickService;
    private final UserMapper userMapper;
    private final com.pig4cloud.config.service.ConfigService configService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public R<Void> createRole(RoleCreateDto dto) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRole_name(dto.getRoleName());
        roleEntity.setRole_code(dto.getRoleCode());
        roleEntity.setDescription(dto.getDescription());
        // 数据权限默认本租户全部
        roleEntity.setData_scope(dto.getDataScope() == null || dto.getDataScope().isBlank()
                ? "1" : dto.getDataScope());
        // 租户归属由服务端决定：取当前登录用户的租户，未认证上下文(平台操作)归平台层
        Integer tenantId = UserContext.getTenantId() == null ? 0 : UserContext.getTenantId();
        validateParent(dto.getParentId(), null, tenantId);
        roleEntity.setParent_id(dto.getParentId() == null || dto.getParentId() == 0 ? 0 : dto.getParentId());
        roleEntity.setTenant_id(tenantId);
        int rows = roleMapper.insert(roleEntity);
        if (rows <= 0) {
            return R.fail("创建失败");
        }
        saveRoleMenus(roleEntity.getId(), dto.getMenuCodes());
        return R.ok("更新成功", null);
    }

    @Override
    @Transactional
    public R<Void> updateRole(RoleUpdateDto dto) {
        RoleEntity exists = roleMapper.selectById(dto.getId());
        if (exists == null) {
            return R.fail("角色不存在");
        }
        if ("super".equals(exists.getRole_code())
                && dto.getParentId() != null && dto.getParentId() != 0
                && !dto.getParentId().equals(exists.getParent_id())) {
            throw new BizException("超级管理员角色不支持调整上级角色");
        }
        // 审计：记录变更前后字段对比
        com.pig4cloud.log.audit.AuditDiffContext.set(com.pig4cloud.log.audit.DiffUtil.diff(
                Map.of("role_name", nvl(exists.getRole_name()), "description", nvl(exists.getDescription()),
                        "data_scope", nvl(exists.getData_scope())),
                Map.of("role_name", nvl(dto.getRoleName()), "description", nvl(dto.getDescription()),
                        "data_scope", nvl(dto.getDataScope()))));
        validateParent(dto.getParentId(), exists, exists.getTenant_id());
        UpdateWrapper<RoleEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", dto.getId());
        updateWrapper.set("role_code", dto.getRoleCode());
        updateWrapper.set("role_name", dto.getRoleName());
        updateWrapper.set("description", dto.getDescription());
        if (dto.getDataScope() != null && !dto.getDataScope().isBlank()) {
            updateWrapper.set("data_scope", dto.getDataScope());
        }
        updateWrapper.set("parent_id", dto.getParentId() == null || dto.getParentId() == 0 ? 0 : dto.getParentId());
        int rows = roleMapper.update(null, updateWrapper);
        if (rows <= 0) {
            return R.fail("更新失败");
        }
        if (dto.getMenuCodes() != null) {
            // 显式传入菜单才重建；null表示本次不动菜单（避免误清空）
            menuMapper.deleteMenus(dto.getId());
            saveRoleMenus(dto.getId(), dto.getMenuCodes());
        }
        // 菜单/权限变更后踢掉持有该角色的在线用户，权限立即生效（token里权限是登录时烤入的）
        sessionKickService.kickUsernames(userMapper.selectUsernamesByRoleId(dto.getId()));
        return R.ok("更新成功", null);
    }

    @Override
    public R<Void> deleteRole(RoleDeleteDto dto) {
        List<Integer> roleIds = roleMapper.selectList(new QueryWrapper<RoleEntity>()
                        .eq("role_code", dto.getRoleCode()))
                .stream().map(RoleEntity::getId).toList();
        if (roleIds.isEmpty()) {
            return R.ok("删除失败", null);
        }
        for (Integer roleId : roleIds) {
            if (!roleHierarchyService.descendantIds(roleId).isEmpty()) {
                throw new BizException("该角色存在下级角色，请先删除或调整其下级角色");
            }
        }
        // 删除前收集持有者，删除后踢会话（user_role随角色级联删除，之后查不到人）
        List<String> holders = roleIds.stream()
                .flatMap(roleId -> userMapper.selectUsernamesByRoleId(roleId).stream())
                .distinct()
                .toList();
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_code", dto.getRoleCode());
        int rows;
        if (configService.getBool("recycle.enabled", true)) {
            // 软删除进回收站（MP @TableLogic自动转为update deleted=1）
            rows = roleMapper.delete(queryWrapper);
        } else {
            // 硬删除
            rows = jdbcTemplate.update("DELETE FROM sys_role WHERE role_code = ?", dto.getRoleCode());
        }
        sessionKickService.kickUsernames(holders);
        return R.ok(rows > 0 ? "删除成功" : "删除失败", null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R<?> getRoleLists(RoleDto roleDto) {
        long page = roleDto.getPage();
        long pageSize = roleDto.getPageSize();
        if (page >= 0 && pageSize >= 0) {
            String roleName = roleDto.getRoleName();
            List<Map<String, Object>> list = roleMapper.selectList1(roleName, pageSize, page - 1);
            long total = roleMapper.selectUserList2Count();
            return R.ok("获取数据成功", PageResult.of(list, total, pageSize, page));
        }
        // 全量（含菜单名/父角色），角色管理树形列表使用
        List<Map<String, Object>> rows = roleMapper.selectListWithMenus();
        return R.ok("获取数据成功", rows);
    }

    /**
     * 上级角色校验：存在、同租户、不能是自身或自身子孙（防成环）、祖先链上不允许出现super角色
     * （super拥有跨租户权限且其菜单含租户管理，若被继承会导致子角色越权）
     */
    private void validateParent(Integer parentId, RoleEntity selfRole, Integer tenantId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (selfRole != null && parentId.equals(selfRole.getId())) {
            throw new BizException("上级角色不能是自身");
        }
        RoleEntity parent = roleMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException("上级角色不存在");
        }
        if (!tenantId.equals(parent.getTenant_id())) {
            throw new BizException("上级角色与角色不属于同一租户");
        }
        if (selfRole != null && roleHierarchyService.descendantIds(selfRole.getId()).contains(parentId)) {
            throw new BizException("不能将上级角色设置为自己的下级角色");
        }
        Integer cursor = parent.getId();
        int depth = 0;
        while (cursor != null && depth++ < 20) {
            RoleEntity current = roleMapper.selectById(cursor);
            if (current == null) {
                break;
            }
            if ("super".equals(current.getRole_code())) {
                throw new BizException("不能挂在超级管理员角色及其下级角色之下");
            }
            Integer pid = current.getParent_id();
            cursor = (pid == null || pid == 0) ? null : pid;
        }
    }

    /**
     * 重建角色的菜单关联
     */
    private void saveRoleMenus(Integer roleId, List<String> menuCodes) {
        List<String> codes = menuCodes == null ? List.of() : menuCodes;
        if (codes.isEmpty()) {
            return;
        }
        List<Map<String, Object>> roleMenus = codes.stream()
                .map(menuId -> {
                    Map<String, Object> roleMenu = new HashMap<>();
                    roleMenu.put("menu_id", Integer.parseInt(menuId.trim()));
                    roleMenu.put("role_id", roleId);
                    return roleMenu;
                })
                .toList();
        int insertResult = menuMapper.insertUserRoles(roleMenus);
        if (insertResult <= 0) {
            throw new BizException("更新角色菜单关联失败");
        }
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
