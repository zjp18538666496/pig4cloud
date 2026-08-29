package com.pig4cloud.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.common.exception.BizException;
import com.pig4cloud.common.result.PageResult;
import com.pig4cloud.common.result.R;
import com.pig4cloud.log.annotation.LogRecord;
import com.pig4cloud.menu.entity.MenuEntity;
import com.pig4cloud.menu.mapper.MenuMapper;
import com.pig4cloud.role.entity.RoleEntity;
import com.pig4cloud.role.mapper.RoleMapper;
import com.pig4cloud.tenant.dto.TenantCreateDto;
import com.pig4cloud.tenant.dto.TenantUpdateDto;
import com.pig4cloud.tenant.dto.TenantDto;
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
        // 1. 创建租户
        TenantEntity tenant = new TenantEntity();
        tenant.setTenant_code(dto.getTenantCode());
        tenant.setTenant_name(dto.getTenantName());
        tenant.setStatus("1");
        tenant.setCreate_time(new Timestamp(System.currentTimeMillis()));
        tenantMapper.insert(tenant);

        // 2. 创建租户管理员账号（初始密码12345678）
        String adminUsername = dto.getTenantCode() + "_admin";
        if (userMapper.selectUserByUsername(adminUsername) != null) {
            throw new BizException("管理员账号已存在：" + adminUsername);
        }
        UserEntity admin = new UserEntity();
        admin.setUsername(adminUsername);
        admin.setName(dto.getTenantName() + "管理员");
        admin.setPassword(passwordEncoder.encode(INITIAL_ADMIN_PASSWORD));
        admin.setTenant_id(tenant.getId());
        admin.setCreate_time(new Timestamp(System.currentTimeMillis()));
        userMapper.insert(admin);

        // 3. 创建租户管理员角色
        RoleEntity role = new RoleEntity();
        role.setRole_code(TENANT_ADMIN_ROLE_CODE);
        role.setRole_name(dto.getTenantName() + "管理员");
        role.setDescription("开通租户自动创建");
        role.setTenant_id(tenant.getId());
        roleMapper.insert(role);

        // 4. 租户管理员角色绑定全部菜单（含按钮权限点）
        List<MenuEntity> menus = menuMapper.selectList(null);
        List<Map<String, Object>> roleMenus = menus.stream()
                .map(menu -> {
                    Map<String, Object> roleMenu = new HashMap<>();
                    roleMenu.put("menu_id", menu.getId());
                    roleMenu.put("role_id", role.getId());
                    return roleMenu;
                })
                .toList();
        menuMapper.insertUserRoles(roleMenus);

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
    public R<Void> updateTenant(TenantUpdateDto dto) {
        UpdateWrapper<TenantEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", dto.getId())
                .set("tenant_name", dto.getTenantName())
                .set("status", dto.getStatus())
                .set("update_time", new Date());
        int rows = tenantMapper.update(null, updateWrapper);
        return R.ok(rows > 0 ? "更新成功" : "更新失败", null);
    }
}
