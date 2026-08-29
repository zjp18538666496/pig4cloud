package com.pig4cloud.common.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.pig4cloud.common.context.UserContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Set;

/**
 * 共享表多租户策略：用户/角色按tenant_id隔离，菜单与平台表全局共享。
 * 平台超管(角色编码super)和未认证上下文(登录/后台任务)不追加租户条件。
 */
public class MyTenantHandler implements TenantLineHandler {

    /**
     * 不参与租户隔离的表：平台级数据 + 无tenant_id列的关联表
     */
    private static final Set<String> IGNORE_TABLES = Set.of(
            "sys_tenant", "sys_menu", "sys_permission",
            "user_role", "role_menu", "role_permission");

    @Override
    public Expression getTenantId() {
        return new LongValue(UserContext.getTenantId() == null ? 0L : UserContext.getTenantId());
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // 未认证上下文或平台超管：所有表不加租户条件
        if (UserContext.get() == null || UserContext.isSuperTenant()) {
            return true;
        }
        return IGNORE_TABLES.contains(tableName.toLowerCase());
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }
}
