-- =============================================================
-- V7 回收站/公告定时发布/弱口令配置/大屏与生成器菜单
-- 由启动器按 sys_schema_version 版本登记自动执行，也可手动 source。
-- 注意：脚本由 continueOnError 模式执行，重复执行导致的已存在报错可忽略。
-- =============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 1. 回收站：用户/角色软删除列（recycle.enabled=true时删除为标记删除）
-- ----------------------------
ALTER TABLE `sys_user`
  ADD COLUMN `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '软删除标记(1在回收站)' AFTER `backup_codes`,
  ADD COLUMN `delete_time` datetime NULL DEFAULT NULL COMMENT '删除时间' AFTER `deleted`;
ALTER TABLE `sys_role`
  ADD COLUMN `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '软删除标记(1在回收站)' AFTER `parent_id`,
  ADD COLUMN `delete_time` datetime NULL DEFAULT NULL COMMENT '删除时间' AFTER `deleted`;

-- ----------------------------
-- 2. 公告定时发布列（status新增'2'定时待发布）
-- ----------------------------
ALTER TABLE `sys_notice`
  ADD COLUMN `publish_time` datetime NULL DEFAULT NULL COMMENT '定时发布时间(status=2时到点自动发布)' AFTER `status`;

-- ----------------------------
-- 3. 新增配置项（幂等）
-- ----------------------------
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `remark`)
SELECT 'pwd.weak-dict-enabled', '弱口令字典校验', 'true', '拒绝常见弱密码'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'pwd.weak-dict-enabled');
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `remark`)
SELECT 'recycle.enabled', '删除进回收站', 'true', '用户/角色删除改为软删除，可在回收站恢复'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'recycle.enabled');
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `remark`)
SELECT 'approval.assignee', '审批人账号', 'admin', 'Flowable审批任务的处理人'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'approval.assignee');

-- ----------------------------
-- 4. 定时发布/回收站/数据大屏/代码生成/审批中心菜单（幂等）
-- ----------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 208, 2, '回收站', '/recycle-manager', '1', '1', '2', '@/views/recycle-manager/Index.vue', 'recycle-manager', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 208);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 20801, 208, '回收站管理', NULL, '1', '2', '3', NULL, NULL, 'recycle:manage'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 20801);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 209, 2, '审批中心', '/approval-manager', '1', '1', '2', '@/views/approval-manager/Index.vue', 'approval-manager', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 209);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 305, 3, '数据大屏', '/data-screen', '1', '1', '2', '@/views/data-screen/Index.vue', 'data-screen', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 305);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 30501, 305, '大屏查看', NULL, '1', '2', '3', NULL, NULL, 'screen:read'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 30501);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 306, 3, '代码生成', '/gen-manager', '1', '1', '2', '@/views/gen-manager/Index.vue', 'gen-manager', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 306);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 30601, 306, '代码生成', NULL, '1', '2', '3', NULL, NULL, 'gen:manage'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 30601);

-- ----------------------------
-- 5. 授权（幂等）：root=回收站/审批中心；super=全部
-- ----------------------------
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 724, 208, 102 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 724);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 725, 20801, 102 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 725);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 726, 209, 102 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 726);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 727, 208, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 727);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 728, 20801, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 728);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 729, 209, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 729);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 730, 305, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 730);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 731, 30501, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 731);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 732, 306, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 732);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 733, 30601, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 733);

-- ----------------------------
-- 6. 定时发布任务种子（幂等）
-- ----------------------------
INSERT INTO `sys_job` (`job_name`, `handler`, `cron`, `status`, `remark`, `create_time`)
SELECT '公告定时发布', 'noticePublishJob', '0 * * * * ?', '1', '每分钟扫描到期的定时发布公告并发布', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE handler = 'noticePublishJob');
