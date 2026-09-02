package com.pig4cloud.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.auth.online.SessionKickService;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.config.service.ConfigService;
import com.pig4cloud.dept.service.DeptService;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.menu.mapper.MenuMapper;
import com.pig4cloud.notice.entity.NoticeEntity;
import com.pig4cloud.notice.mapper.NoticeMapper;
import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.tenant.dto.TenantCreateDto;
import com.pig4cloud.tenant.dto.TenantDto;
import com.pig4cloud.tenant.dto.TenantUpdateDto;
import com.pig4cloud.tenant.entity.TenantEntity;
import com.pig4cloud.tenant.mapper.TenantMapper;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    /**
     * 租户管理员初始密码
     */
    private static final String INITIAL_ADMIN_PASSWORD = "12345678";
    private static final String TENANT_ADMIN_ROLE_CODE = "tenant_admin";

    private final TenantMapper tenantMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final NoticeMapper noticeMapper;
    private final TenantPackageService packageService;
    private final SessionKickService sessionKickService;
    private final ConfigService configService;
    private final DeptService deptService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public R<PageResult<TenantEntity>> getTenantLists(TenantDto dto) {
        long page = dto.getPage();
        long pageSize = dto.getPageSize();
        Page<TenantEntity> result = tenantMapper.selectPage(new Page<>(page, pageSize),
                new QueryWrapper<TenantEntity>()
                        .like(!dto.getTenantName().isBlank(), "tenant_name", dto.getTenantName())
                        .orderByDesc("id"));
        return R.ok("获取数据成功", PageResult.of(result.getRecords(), result.getTotal(), pageSize, page));
    }

    @Override
    @LogRecord(module = "租户管理", operation = "开通租户")
    @Transactional
    public R<Void> createTenant(TenantCreateDto dto) {
        if (tenantMapper.selectCount(new QueryWrapper<TenantEntity>().eq("tenant_code", dto.getTenantCode())) > 0) {
            throw new BizException("租户编码已存在");
        }
        // 校验套餐并解析其菜单范围（开通时租户管理员只能用套餐内菜单）
        List<String> packageMenuIds = packageService.resolvePackageMenuIds(dto.getPackageId());

        // 1. 创建租户
        TenantEntity tenant = new TenantEntity();
        tenant.setTenant_code(dto.getTenantCode());
        tenant.setTenant_name(dto.getTenantName());
        tenant.setStatus("1");
        tenant.setPackage_id(dto.getPackageId());
        tenant.setExpire_time(dto.getExpireTime());
        tenant.setUser_limit(dto.getUserLimit());
        tenant.setCreate_time(new Timestamp(System.currentTimeMillis()));
        tenantMapper.insert(tenant);

        // 2. 创建租户管理员账号（初始密码12345678）
        // 账号格式 t{租户ID}admin：纯字母数字且4~12位，满足登录/注册表单的统一校验规则
        String adminUsername = "t" + tenant.getId() + "admin";
        if (userMapper.selectUserByUsername(adminUsername) != null) {
            throw new BizException("管理员账号已存在：" + adminUsername);
        }
        UserEntity admin = new UserEntity();
        admin.setUsername(adminUsername);
        admin.setName(dto.getTenantName() + "管理员");
        admin.setPassword(passwordEncoder.encode(INITIAL_ADMIN_PASSWORD));
        admin.setTenant_id(tenant.getId());
        // 初始密码按配置强制首登修改
        admin.setForce_pwd_change(configService.getBool("pwd.force-change-initial", true) ? 1 : 0);
        admin.setPwd_update_time(new Timestamp(System.currentTimeMillis()));
        admin.setCreate_time(new Timestamp(System.currentTimeMillis()));
        userMapper.insert(admin);

        // 3. 创建租户管理员角色
        RoleEntity role = new RoleEntity();
        role.setRole_code(TENANT_ADMIN_ROLE_CODE);
        role.setRole_name(dto.getTenantName() + "管理员");
        role.setDescription("开通租户自动创建");
        role.setTenant_id(tenant.getId());
        roleMapper.insert(role);

        // 4. 租户管理员角色绑定套餐内菜单（含按钮权限点）
        saveRoleMenus(role.getId(), packageMenuIds);

        // 5. 绑定管理员账号与角色
        Map<String, Object> userRole = new HashMap<>();
        userRole.put("user_id", admin.getId());
        userRole.put("role_id", role.getId());
        userMapper.insertUserRoles(List.of(userRole));

        log.info("租户[{}]开通完成，管理员账号：{}，初始密码：{}", dto.getTenantName(), adminUsername, INITIAL_ADMIN_PASSWORD);
        return R.ok("开通成功，管理员账号：" + adminUsername, null);
    }

    @Override
    @LogRecord(module = "租户管理", operation = "编辑租户")
    @Transactional
    public R<Void> updateTenant(TenantUpdateDto dto) {
        TenantEntity exists = tenantMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException("租户不存在");
        }
        boolean packageChanged = dto.getPackageId() != null && !dto.getPackageId().equals(exists.getPackage_id());
        // 审计：记录变更前后字段对比
        com.pig4cloud.log.audit.AuditDiffContext.set(com.pig4cloud.log.audit.DiffUtil.diff(
                Map.of("tenant_name", nvl(exists.getTenant_name()), "status", nvl(exists.getStatus()),
                        "user_limit", String.valueOf(exists.getUser_limit())),
                Map.of("tenant_name", nvl(dto.getTenantName()), "status", nvl(dto.getStatus()),
                        "user_limit", String.valueOf(dto.getUserLimit()))));
        // 换套餐时同步重绑该租户tenant_admin角色的菜单
        if (packageChanged) {
            List<String> packageMenuIds = packageService.resolvePackageMenuIds(dto.getPackageId());
            RoleEntity adminRole = roleMapper.selectOne(new QueryWrapper<RoleEntity>()
                    .eq("role_code", TENANT_ADMIN_ROLE_CODE)
                    .eq("tenant_id", dto.getId()));
            if (adminRole != null) {
                menuMapper.deleteMenus(adminRole.getId());
                saveRoleMenus(adminRole.getId(), packageMenuIds);
            }
        }
        UpdateWrapper<TenantEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", dto.getId())
                .set("tenant_name", dto.getTenantName())
                .set("status", dto.getStatus())
                .set("package_id", dto.getPackageId())
                .set("expire_time", dto.getExpireTime())
                .set("user_limit", dto.getUserLimit())
                .set("update_time", new Date());
        tenantMapper.update(null, updateWrapper);
        // 禁用或换套餐（菜单范围变了）时踢掉该租户全部在线会话，权限/可见菜单立即生效
        if ("0".equals(dto.getStatus()) || packageChanged) {
            sessionKickService.kickTenant(dto.getId());
        }
        return R.ok("更新成功", null);
    }

    @Override
    @LogRecord(module = "租户管理", operation = "删除租户")
    @Transactional
    public R<Void> deleteTenant(Integer id) {
        TenantEntity exists = tenantMapper.selectById(id);
        if (exists == null) {
            throw new BizException("租户不存在");
        }
        if (id == 0 || id == 1) {
            throw new BizException("平台层与默认租户不允许删除");
        }
        // 租户下仍有用户时拒绝删除（防误删活跃租户）
        if (userMapper.selectCount(new QueryWrapper<UserEntity>().eq("tenant_id", id)) > 0) {
            throw new BizException("该租户下仍存在用户，请先移出或删除用户后再删除租户");
        }
        // 踢掉该租户残留会话（已无用户，通常为空）
        sessionKickService.kickTenant(id);
        // 级联清理：角色（角色删除级联清role_menu/user_role）、部门、本租户公告、套餐绑定
        roleMapper.delete(new QueryWrapper<RoleEntity>().eq("tenant_id", id));
        deptService.deleteByTenantId(id);
        noticeMapper.delete(new QueryWrapper<NoticeEntity>().eq("tenant_id", id));
        tenantMapper.deleteById(id);
        log.info("租户[{}]已删除", exists.getTenant_name());
        return R.ok("删除成功", null);
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

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
