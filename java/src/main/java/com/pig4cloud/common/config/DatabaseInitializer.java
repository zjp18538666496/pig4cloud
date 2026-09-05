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
 * 执行classpath下的sql/pigx_admin_init.sql建表并灌入演示数据（脚本已包含全部
 * 表结构与种子数据）；表已存在则跳过，不触碰已有数据。
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

    private static final String INIT_SCRIPT = "sql/pigx_admin_init.sql";

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
            if (tableExists(connection, MARKER_TABLE)) {
                log.info("检测到业务表已存在，跳过数据库初始化");
                return;
            }
            log.info("业务表不存在，开始执行数据库初始化脚本 {}", INIT_SCRIPT);
            ScriptUtils.executeSqlScript(connection, new EncodedResource(
                    new ClassPathResource(INIT_SCRIPT), StandardCharsets.UTF_8));
            log.info("数据库初始化完成（演示账号：root/12345678，多公司演示数据已内置）");
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
