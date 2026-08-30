-- =============================================================
-- 老库增量升级脚本 2026-08-30
-- 适用：已按旧版 pigx_admin.sql 初始化过的数据库。
-- 后端启动时会自动检测（sys_dept 表不存在则执行本脚本），也可手动 source 执行。
-- 内容：部门/通知公告/租户套餐三张新表，用户/角色/租户表新列，
--       新功能菜单与演示角色授权，默认租户套餐演示数据。
-- 注意：脚本由 continueOnError 模式执行，个别列已存在导致的报错可忽略。
-- =============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 1. 新表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_dept`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` int(11) NOT NULL DEFAULT 0 COMMENT '父部门id(0为根)',
  `dept_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门名称',
  `sort` int(11) NULL DEFAULT 0 COMMENT '显示顺序',
  `tenant_id` int(11) NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `sys_notice`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '公告id',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '公告内容',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态(0草稿1发布)',
  `tenant_id` int(11) NOT NULL DEFAULT 0 COMMENT '租户id(0为平台全员可见)',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '发布人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通知公告表' ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `sys_tenant_package`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '套餐id',
  `package_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '套餐名称',
  `menu_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '关联菜单id(逗号分隔)',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '状态(0停用1启用)',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户套餐表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 2. 已有表加列（列已存在时报错可忽略）
-- ----------------------------
ALTER TABLE `sys_user` ADD COLUMN `dept_id` int(11) NULL DEFAULT NULL COMMENT '部门id' AFTER `tenant_id`;
ALTER TABLE `sys_role` ADD COLUMN `data_scope` char(1) NULL DEFAULT '1' COMMENT '数据权限(1本租户全部2本部门及以下3仅本人)' AFTER `tenant_id`;
ALTER TABLE `sys_tenant`
  ADD COLUMN `package_id` int(11) NULL DEFAULT NULL COMMENT '绑定套餐id' AFTER `update_time`,
  ADD COLUMN `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间(空为永不过期)' AFTER `package_id`,
  ADD COLUMN `user_limit` int(11) NULL DEFAULT NULL COMMENT '用户数上限(空为不限制)' AFTER `expire_time`;

-- ----------------------------
-- 3. 新功能菜单（部门/公告/在线用户挂系统管理，套餐挂平台管理）
-- ----------------------------
INSERT INTO `sys_menu` VALUES (20104, 201, '部门管理', '/dept-manager', '1', '1', '3', '@/views/dept-manager/Index.vue', 'dept-manager', NULL);
INSERT INTO `sys_menu` VALUES (2010401, 20104, '部门编辑', NULL, '1', '2', '5', NULL, NULL, 'dept:write');
INSERT INTO `sys_menu` VALUES (2010402, 20104, '部门删除', NULL, '1', '2', '5', NULL, NULL, 'dept:remove');
INSERT INTO `sys_menu` VALUES (203, 2, '通知公告', '/notice-manager', '1', '1', '2', '@/views/notice-manager/Index.vue', 'notice-manager', NULL);
INSERT INTO `sys_menu` VALUES (20301, 203, '公告编辑', NULL, '1', '2', '3', NULL, NULL, 'notice:write');
INSERT INTO `sys_menu` VALUES (20302, 203, '公告删除', NULL, '1', '2', '3', NULL, NULL, 'notice:remove');
INSERT INTO `sys_menu` VALUES (204, 2, '在线用户', '/online-manager', '1', '1', '2', '@/views/online-manager/Index.vue', 'online-manager', NULL);
INSERT INTO `sys_menu` VALUES (20401, 204, '在线查询', NULL, '1', '2', '3', NULL, NULL, 'online:read');
INSERT INTO `sys_menu` VALUES (20402, 204, '强制下线', NULL, '1', '2', '3', NULL, NULL, 'online:kick');
INSERT INTO `sys_menu` VALUES (302, 3, '租户套餐', '/package-manager', '1', '1', '2', '@/views/package-manager/Index.vue', 'package-manager', NULL);
INSERT INTO `sys_menu` VALUES (30201, 302, '套餐编辑', NULL, '1', '2', '3', NULL, NULL, 'package:write');
INSERT INTO `sys_menu` VALUES (30202, 302, '套餐删除', NULL, '1', '2', '3', NULL, NULL, 'package:remove');

-- ----------------------------
-- 4. 给演示角色补授权（root=102 系统侧新菜单；super=100 全部新菜单）
--    其他存量租户的 tenant_admin 角色如需新菜单，请在角色管理里自行勾选保存
-- ----------------------------
INSERT INTO `role_menu` VALUES (65, 20104, 102);
INSERT INTO `role_menu` VALUES (66, 2010401, 102);
INSERT INTO `role_menu` VALUES (67, 2010402, 102);
INSERT INTO `role_menu` VALUES (68, 203, 102);
INSERT INTO `role_menu` VALUES (69, 20301, 102);
INSERT INTO `role_menu` VALUES (70, 20302, 102);
INSERT INTO `role_menu` VALUES (71, 204, 102);
INSERT INTO `role_menu` VALUES (72, 20401, 102);
INSERT INTO `role_menu` VALUES (73, 20402, 102);
INSERT INTO `role_menu` VALUES (74, 20104, 100);
INSERT INTO `role_menu` VALUES (75, 2010401, 100);
INSERT INTO `role_menu` VALUES (76, 2010402, 100);
INSERT INTO `role_menu` VALUES (77, 203, 100);
INSERT INTO `role_menu` VALUES (78, 20301, 100);
INSERT INTO `role_menu` VALUES (79, 20302, 100);
INSERT INTO `role_menu` VALUES (80, 204, 100);
INSERT INTO `role_menu` VALUES (81, 20401, 100);
INSERT INTO `role_menu` VALUES (82, 20402, 100);
INSERT INTO `role_menu` VALUES (83, 302, 100);
INSERT INTO `role_menu` VALUES (84, 30201, 100);
INSERT INTO `role_menu` VALUES (85, 30202, 100);

-- ----------------------------
-- 5. 演示数据：默认套餐 + 演示部门 + 默认租户绑套餐
-- ----------------------------
INSERT INTO `sys_tenant_package` VALUES (1, '标准套餐', '2,201,20101,2010101,2010102,20102,2010201,2010202,20103,2010301,2010302,20104,2010401,2010402,202,20201', '1', '权限管理与日志管理全套', NOW(), NULL);
INSERT INTO `sys_dept` VALUES (1, 0, '总部', 1, 1, NOW(), NULL);
INSERT INTO `sys_dept` VALUES (2, 1, '研发部', 1, 1, NOW(), NULL);
INSERT INTO `sys_dept` VALUES (3, 1, '市场部', 2, 1, NOW(), NULL);
UPDATE `sys_tenant` SET `package_id` = 1 WHERE `id` = 1 AND `package_id` IS NULL;
