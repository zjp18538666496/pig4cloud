-- =============================================================
-- V6 Open API密钥 / 健康自检 / 会话与脱敏配置 / 2FA治理
-- 由启动器按 sys_schema_version 版本登记自动执行，也可手动 source。
-- 注意：脚本由 continueOnError 模式执行，重复执行导致的已存在报错可忽略。
-- =============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 1. Open API密钥表（平台级）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_api_key`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `app_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '接入方名称',
  `api_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API Key(请求头X-Api-Key)',
  `scopes` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '授权范围(逗号分隔,如user:read,notice:read)',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '状态(0停用1启用)',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间(空为永不过期)',
  `last_used_time` datetime NULL DEFAULT NULL COMMENT '最后调用时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_api_key`(`api_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'Open API密钥表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 2. 新增配置项（幂等）
-- ----------------------------
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `remark`)
SELECT 'login.max-sessions-per-user', '单账号最大在线设备数', '3', '0=不限制，超限自动下线最旧设备'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'login.max-sessions-per-user');
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `remark`)
SELECT 'mask.enabled', '敏感字段脱敏', 'true', '开启后无user:write权限者看到的手机号/邮箱为脱敏形式'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mask.enabled');
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `remark`)
SELECT 'openapi.rate-limit', 'Open API每分钟调用上限', '60', '按API Key限流'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'openapi.rate-limit');
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `remark`)
SELECT 'login.2fa-force-enabled', '强制开启两步认证', 'false', '开启后所有未绑定2FA的账号登录时会引导绑定'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'login.2fa-force-enabled');

-- ----------------------------
-- 3. API密钥管理菜单（平台管理下，仅super）
-- ----------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 304, 3, 'API密钥', '/api-key-manager', '1', '1', '2', '@/views/api-key-manager/Index.vue', 'api-key-manager', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 304);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 30401, 304, '密钥管理', NULL, '1', '2', '3', NULL, NULL, 'apikey:manage'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 30401);

INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`)
SELECT 722, 304, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 722);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`)
SELECT 723, 30401, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 723);
