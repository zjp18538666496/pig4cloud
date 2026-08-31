-- =============================================================
-- V5 两步认证(2FA) + 接口监控：sys_user增加TOTP字段、2FA总开关配置、监控中心菜单
-- 由启动器按 sys_schema_version 版本登记自动执行，也可手动 source。
-- 注意：脚本由 continueOnError 模式执行，重复执行导致的已存在报错可忽略。
-- =============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 1. 用户TOTP字段
-- ----------------------------
ALTER TABLE `sys_user`
  ADD COLUMN `totp_secret` varchar(64) NULL DEFAULT NULL COMMENT 'TOTP密钥(Base32)' AFTER `pwd_update_time`,
  ADD COLUMN `totp_enabled` tinyint(1) NULL DEFAULT 0 COMMENT '是否开启两步认证' AFTER `totp_secret`,
  ADD COLUMN `backup_codes` text NULL COMMENT '备用恢复码(SHA256哈希,逗号分隔)' AFTER `totp_enabled`;

-- ----------------------------
-- 2. 2FA总开关配置
-- ----------------------------
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `remark`)
SELECT 'login.2fa-enabled', '两步认证总开关', 'true', '关闭后即使已绑定也不校验动态码'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'login.2fa-enabled');

-- ----------------------------
-- 3. 监控中心菜单（系统管理下）
-- ----------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 207, 2, '监控中心', '/monitor', '1', '1', '2', '@/views/monitor/Index.vue', 'monitor', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 207);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 20701, 207, '监控查看', NULL, '1', '2', '3', NULL, NULL, 'monitor:read'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 20701);

-- ----------------------------
-- 4. 授权：root(102)与super(100)
-- ----------------------------
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`)
SELECT 718, 207, 102 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 718);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`)
SELECT 719, 20701, 102 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 719);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`)
SELECT 720, 207, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 720);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`)
SELECT 721, 20701, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 721);
