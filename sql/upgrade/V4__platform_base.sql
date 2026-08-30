-- =============================================================
-- V4 平台化基座：参数配置/字典/岗位/站内信/定时任务/版本表 + 菜单图标 + 密码策略字段
-- 由启动器按 sys_schema_version 版本登记自动执行，也可手动 source。
-- 注意：脚本由 continueOnError 模式执行，重复执行导致的已存在报错可忽略。
-- =============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 1. 系统参数配置
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_config`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `config_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键',
  `config_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置名称',
  `config_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '配置值',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统参数配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 2. 字典
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_dict`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '字典id',
  `dict_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典编码',
  `dict_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典名称',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '状态(0停用1启用)',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dict_code`(`dict_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典表' ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `sys_dict_item`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '字典项id',
  `dict_id` int(11) NOT NULL COMMENT '字典id',
  `label` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '显示标签',
  `value` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '键值',
  `sort` int(11) NULL DEFAULT 0 COMMENT '排序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '状态(0停用1启用)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典项表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 3. 岗位（租户隔离）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_post`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '岗位id',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位名称',
  `sort` int(11) NULL DEFAULT 0 COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '状态(0停用1启用)',
  `tenant_id` int(11) NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '岗位表' ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `user_post`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(40) NOT NULL COMMENT '用户id',
  `post_id` int(11) NOT NULL COMMENT '岗位id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户岗位表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 4. 站内信（发送时按目标用户扇出落库）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_message`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '消息id',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息标题',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '消息内容',
  `msg_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '类型(1系统消息2公告通知)',
  `tenant_id` int(11) NOT NULL DEFAULT 0 COMMENT '租户id(0为平台)',
  `target_user_id` int(40) NOT NULL COMMENT '目标用户id',
  `read_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '已读(0未读1已读)',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '发送人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_msg_target`(`target_user_id`, `read_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '站内信表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 5. 定时任务
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_job`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '任务id',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `handler` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '处理器Bean名',
  `cron` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'cron表达式',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '状态(0停用1启用)',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '定时任务表' ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `sys_job_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '日志id',
  `job_id` int(11) NOT NULL COMMENT '任务id',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务名称',
  `success` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '结果(0失败1成功)',
  `message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '执行信息',
  `cost_ms` bigint NULL DEFAULT NULL COMMENT '耗时(ms)',
  `create_time` datetime NULL DEFAULT NULL COMMENT '执行时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_joblog_job`(`job_id`, `id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务执行日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 6. 已有表加列（已存在时报错可忽略）
-- ----------------------------
ALTER TABLE `sys_menu` ADD COLUMN `icon` varchar(64) NULL DEFAULT NULL COMMENT '菜单图标' AFTER `perms`;
ALTER TABLE `sys_user`
  ADD COLUMN `force_pwd_change` tinyint(1) NULL DEFAULT 0 COMMENT '强制修改密码(首次登录/管理员重置后)' AFTER `dept_id`,
  ADD COLUMN `pwd_update_time` datetime NULL DEFAULT NULL COMMENT '密码最后修改时间' AFTER `force_pwd_change`;

-- ----------------------------
-- 7. 参数配置种子
-- ----------------------------
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `remark`) VALUES
('pwd.min-length', '密码最小长度', '8', '注册/创建/改密时校验'),
('pwd.require-complex', '密码必须含字母和数字', 'false', 'true=复杂模式'),
('pwd.force-change-initial', '初始/重置密码强制首登修改', 'true', '租户管理员初始密码、管理员重置密码后生效'),
('pwd.expire-days', '密码有效期(天)', '0', '0=永不过期'),
('login.max-attempts', '登录失败锁定阈值', '5', '同账号窗口内连续失败次数'),
('login.lock-minutes', '登录锁定时长(分钟)', '10', ''),
('login.ip-window-max', '单IP登录尝试上限', '30', '10分钟窗口内'),
('captcha.enabled', '图形验证码开关', 'true', '关闭后登录不校验验证码'),
('log.retention-days', '日志保留天数', '90', '0=永久保留，logCleanJob使用');

-- ----------------------------
-- 8. 字典种子示例
-- ----------------------------
INSERT INTO `sys_dict` (`id`, `dict_code`, `dict_name`, `status`, `remark`, `create_time`) VALUES (1, 'gender', '性别', '1', '示例字典', NOW());
INSERT INTO `sys_dict` (`id`, `dict_code`, `dict_name`, `status`, `remark`, `create_time`) VALUES (2, 'education', '学历', '1', '示例字典', NOW());
INSERT INTO `sys_dict_item` (`dict_id`, `label`, `value`, `sort`, `status`)
SELECT 1, '男', '1', 1, '1' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = 1 AND value = '1');
INSERT INTO `sys_dict_item` (`dict_id`, `label`, `value`, `sort`, `status`)
SELECT 1, '女', '2', 2, '1' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = 1 AND value = '2');
INSERT INTO `sys_dict_item` (`dict_id`, `label`, `value`, `sort`, `status`)
SELECT 2, '高中及以下', '1', 1, '1' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = 2 AND value = '1');
INSERT INTO `sys_dict_item` (`dict_id`, `label`, `value`, `sort`, `status`)
SELECT 2, '大专', '2', 2, '1' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = 2 AND value = '2');
INSERT INTO `sys_dict_item` (`dict_id`, `label`, `value`, `sort`, `status`)
SELECT 2, '本科', '3', 3, '1' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = 2 AND value = '3');
INSERT INTO `sys_dict_item` (`dict_id`, `label`, `value`, `sort`, `status`)
SELECT 2, '硕士', '4', 4, '1' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = 2 AND value = '4');
INSERT INTO `sys_dict_item` (`dict_id`, `label`, `value`, `sort`, `status`)
SELECT 2, '博士', '5', 5, '1' WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = 2 AND value = '5');

-- ----------------------------
-- 9. 新功能菜单：字典/参数配置(系统管理)、岗位(权限管理)、定时任务(平台管理)
-- ----------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`) VALUES (205, 2, '字典管理', '/dict-manager', '1', '1', '2', '@/views/dict-manager/Index.vue', 'dict-manager', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`) VALUES (20501, 205, '字典编辑', NULL, '1', '2', '3', NULL, NULL, 'dict:write');
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`) VALUES (20502, 205, '字典删除', NULL, '1', '2', '3', NULL, NULL, 'dict:remove');
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`) VALUES (206, 2, '参数配置', '/config-manager', '1', '1', '2', '@/views/config-manager/Index.vue', 'config-manager', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`) VALUES (20601, 206, '配置编辑', NULL, '1', '2', '3', NULL, NULL, 'config:write');
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`) VALUES (20105, 201, '岗位管理', '/post-manager', '1', '1', '3', '@/views/post-manager/Index.vue', 'post-manager', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`) VALUES (2010501, 20105, '岗位编辑', NULL, '1', '2', '5', NULL, NULL, 'post:write');
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`) VALUES (2010502, 20105, '岗位删除', NULL, '1', '2', '5', NULL, NULL, 'post:remove');
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`) VALUES (303, 3, '定时任务', '/job-manager', '1', '1', '2', '@/views/job-manager/Index.vue', 'job-manager', NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`) VALUES (30301, 303, '任务编辑', NULL, '1', '2', '3', NULL, NULL, 'job:write');
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`) VALUES (30302, 303, '任务删除', NULL, '1', '2', '3', NULL, NULL, 'job:remove');
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`) VALUES (30303, 303, '任务执行', NULL, '1', '2', '3', NULL, NULL, 'job:run');

-- ----------------------------
-- 10. 授权：root(102)=字典/岗位；super(100)=全部
-- ----------------------------
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (700, 205, 102);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (701, 20501, 102);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (702, 20502, 102);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (703, 20105, 102);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (704, 2010501, 102);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (705, 2010502, 102);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (706, 205, 100);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (707, 20501, 100);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (708, 20502, 100);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (709, 20105, 100);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (710, 2010501, 100);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (711, 2010502, 100);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (712, 206, 100);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (713, 20601, 100);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (714, 303, 100);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (715, 30301, 100);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (716, 30302, 100);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) VALUES (717, 30303, 100);

-- ----------------------------
-- 11. 定时任务种子
-- ----------------------------
INSERT INTO `sys_job` (`job_name`, `handler`, `cron`, `status`, `remark`, `create_time`)
SELECT '租户过期自动禁用', 'tenantExpireCheckJob', '0 0 1 * * ?', '1', '每天凌晨1点检查并禁用已过期租户', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE handler = 'tenantExpireCheckJob');
INSERT INTO `sys_job` (`job_name`, `handler`, `cron`, `status`, `remark`, `create_time`)
SELECT '日志保留清理', 'logCleanJob', '0 30 1 * * ?', '1', '按 log.retention-days 清理MongoDB操作/登录日志', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE handler = 'logCleanJob');
