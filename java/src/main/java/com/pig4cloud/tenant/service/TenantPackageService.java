package com.pig4cloud.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.auth.online.SessionKickService;
import com.pig4cloud.common.dto.BasePageQuery;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.cache.BizCacheService;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.menu.mapper.MenuMapper;
import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.tenant.dto.TenantPackageDto;
import com.pig4cloud.tenant.entity.TenantEntity;
import com.pig4cloud.tenant.entity.TenantPackageEntity;
import com.pig4cloud.tenant.mapper.TenantMapper;
import com.pig4cloud.tenant.mapper.TenantPackageMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户套餐管理（平台级，仅超管）：决定开通租户时租户管理员角色的菜单范围。
 * 套餐菜单变更时同步重绑使用该套餐租户的管理员角色并踢会话，保证菜单范围即时生效
 */
@Service
@RequiredArgsConstructor
public class TenantPackageService {

    private final TenantPackageMapper packageMapper;
    private final BizCacheService bizCacheService;
    private final TenantMapper tenantMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final SessionKickService sessionKickService;

    @Getter
    @Setter
    public static class PackageQueryDto extends BasePageQuery {
        private String package_name = "";
    }

    public R<PageResult<TenantPackageEntity>> getPackageLists(PackageQueryDto dto) {
        long page = dto.getPage();
        long pageSize = dto.getPageSize();
        Page<TenantPackageEntity> result = packageMapper.selectPage(new Page<>(page, pageSize),
                new QueryWrapper<TenantPackageEntity>()
                        .like(StringUtils.hasText(dto.getPackage_name()), "package_name", dto.getPackage_name())
                        .orderByDesc("id"));
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(), pageSize, page));
    }

    /**
     * 启用中的套餐（开通租户弹窗下拉）
     */
    public R<List<TenantPackageEntity>> getEnabledPackages() {
        return R.ok("获取数据成功", packageMapper.selectList(
                new QueryWrapper<TenantPackageEntity>().eq("status", "1").orderByAsc("id")));
    }

    @LogRecord(module = "租户套餐", operation = "新增套餐")
    public R<Void> createPackage(TenantPackageDto dto) {
        packageMapper.insert(buildEntity(dto, new TenantPackageEntity()));
        return R.ok("创建成功", null);
    }

    @LogRecord(module = "租户套餐", operation = "编辑套餐")
    @Transactional
    public R<Void> updatePackage(TenantPackageDto dto) {
        TenantPackageEntity exists = packageMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException("套餐不存在");
        }
        packageMapper.updateById(buildEntity(dto, exists));
        // 套餐菜单变化：重绑使用该套餐租户的管理员角色菜单并踢会话，菜单范围即时生效
        if (dto.getMenuCodes() != null) {
            List<String> menuIds = dto.getMenuCodes();
            List<TenantEntity> tenants = tenantMapper.selectList(
                    new QueryWrapper<TenantEntity>().eq("package_id", dto.getId()));
            for (TenantEntity tenant : tenants) {
                RoleEntity adminRole = roleMapper.selectOne(new QueryWrapper<RoleEntity>()
                        .eq("role_code", "tenant_admin")
                        .eq("tenant_id", tenant.getId()));
                if (adminRole != null) {
                    menuMapper.deleteMenus(adminRole.getId());
                    saveRoleMenus(adminRole.getId(), menuIds);
                }
                sessionKickService.kickTenant(tenant.getId());
                bizCacheService.evictMenus();
            }
        }
        return R.ok("更新成功", null);
    }

    @LogRecord(module = "租户套餐", operation = "删除套餐")
    public R<Void> deletePackage(Integer id) {
        Long usedCount = tenantMapper.selectCount(new QueryWrapper<TenantEntity>().eq("package_id", id));
        if (usedCount > 0) {
            throw new BizException("有" + usedCount + "个租户正在使用该套餐，无法删除");
        }
        packageMapper.deleteById(id);
        return R.ok("删除成功", null);
    }

    /**
     * 查询套餐菜单id（逗号分隔字符串），供开通租户时绑定角色菜单
     */
    public List<String> resolvePackageMenuIds(Integer packageId) {
        if (packageId == null) {
            throw new BizException("请选择租户套餐");
        }
        TenantPackageEntity pkg = packageMapper.selectById(packageId);
        if (pkg == null || !"1".equals(pkg.getStatus())) {
            throw new BizException("租户套餐不存在或已停用");
        }
        return StringUtils.hasText(pkg.getMenu_ids())
                ? List.of(pkg.getMenu_ids().split(","))
                : List.of();
    }

    private void saveRoleMenus(Integer roleId, List<String> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List<Map<String, Object>> roleMenus = menuIds.stream()
                .map(menuId -> {
                    Map<String, Object> roleMenu = new HashMap<>();
                    roleMenu.put("menu_id", Integer.parseInt(menuId.trim()));
                    roleMenu.put("role_id", roleId);
                    return roleMenu;
                })
                .toList();
        menuMapper.insertUserRoles(roleMenus);
    }

    private TenantPackageEntity buildEntity(TenantPackageDto dto, TenantPackageEntity entity) {
        entity.setPackage_name(dto.getPackageName());
        entity.setMenu_ids(dto.getMenuCodes() == null ? null : String.join(",", dto.getMenuCodes()));
        entity.setStatus(dto.getStatus() == null ? "1" : dto.getStatus());
        entity.setRemark(dto.getRemark());
        if (entity.getId() == null) {
            entity.setCreate_time(new Timestamp(System.currentTimeMillis()));
        } else {
            entity.setUpdate_time(new Date());
        }
        return entity;
    }
}
