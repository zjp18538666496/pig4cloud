package com.pig4cloud.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

/**
 * 首次启动自动初始化数据库：
 * 配合JDBC URL的createDatabaseIfNotExist=true（自动建库），检测不到业务表时
 * 执行classpath下的sql/pigx_admin_init.sql建表并灌入演示数据；表已存在则跳过，不触碰已有数据。
 * 通过app.db-init.enabled=false（环境变量DB_INIT）可在生产环境关闭。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements InitializingBean {

    /**
     * 初始化是否执行过的判定表
     */
    private static final String MARKER_TABLE = "sys_user";

    /**
     * 升级是否执行过的判定表：本版本新增，缺失即视为老库需要增量升级
     */
    private static final String UPGRADE_MARKER_TABLE = "sys_dept";

    private static final String INIT_SCRIPT = "sql/pigx_admin_init.sql";
    private static final String UPGRADE_SCRIPT = "sql/upgrade_20260830.sql";

    /**
     * 角色层级结构升级脚本：sys_role.parent_id 列不存在则执行（须在演示数据之前，保证列数匹配）
     */
    private static final String ROLE_TREE_SCRIPT = "sql/upgrade_20260830_03.sql";

    /**
     * 演示数据是否已补种的判定：多公司演示数据的标志租户
     */
    private static final String DEMO_MARKER_SQL = "SELECT COUNT(*) FROM sys_tenant WHERE tenant_code = 'tech'";
    private static final String DEMO_SCRIPT = "sql/upgrade_20260830_02_demo.sql";

    @Value("${app.db-init.enabled:true}")
    private boolean enabled;

    private final DataSource dataSource;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!enabled) {
            log.info("数据库自动初始化已关闭(app.db-init.enabled=false)");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, MARKER_TABLE)) {
                log.info("业务表不存在，开始执行数据库初始化脚本 {}", INIT_SCRIPT);
                ScriptUtils.executeSqlScript(connection, new EncodedResource(
                        new ClassPathResource(INIT_SCRIPT), StandardCharsets.UTF_8));
                log.info("数据库初始化完成（演示账号：root/12345678，多公司演示数据已内置）");
                return;
            }
            log.info("检测到业务表已存在，跳过数据库初始化");
            if (!tableExists(connection, UPGRADE_MARKER_TABLE)) {
                log.info("检测到旧版本数据库，开始执行增量升级脚本 {}", UPGRADE_SCRIPT);
                // continueOnError：个别列已存在时ALTER TABLE报错不影响后续语句
                ScriptUtils.executeSqlScript(connection, new EncodedResource(
                        new ClassPathResource(UPGRADE_SCRIPT), StandardCharsets.UTF_8),
                        true, false, "--", ";", "/*", "*/");
                log.info("数据库升级完成");
            }
            // 结构类升级须在演示数据补种之前执行（老脚本为定位列INSERT，依赖列数匹配）
            if (!columnExists(connection, "sys_role", "parent_id")) {
                log.info("检测到角色层级列缺失，开始执行升级脚本 {}", ROLE_TREE_SCRIPT);
                ScriptUtils.executeSqlScript(connection, new EncodedResource(
                        new ClassPathResource(ROLE_TREE_SCRIPT), StandardCharsets.UTF_8),
                        true, false, "--", ";", "/*", "*/");
                log.info("角色层级升级完成");
            }
            // 老库补充多公司演示数据（新库init脚本已内置，标志租户存在则跳过）
            if (tableExists(connection, "sys_tenant") && !existsByQuery(connection, DEMO_MARKER_SQL)) {
                log.info("检测到演示数据缺失，开始执行演示数据脚本 {}", DEMO_SCRIPT);
                ScriptUtils.executeSqlScript(connection, new EncodedResource(
                        new ClassPathResource(DEMO_SCRIPT), StandardCharsets.UTF_8),
                        true, false, "--", ";", "/*", "*/");
                log.info("演示数据初始化完成（新增科技/贸易/试用/过期四个演示租户）");
            }
        }
    }

    private boolean existsByQuery(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() && resultSet.getInt(1) > 0;
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }
}
