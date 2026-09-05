-- =============================================================
-- pig4cloud admin 数据库初始化脚本（完整版：建表 + 演示数据）
-- 已合并 V4~V7 全部增量：参数配置/字典/岗位/站内信/定时任务/菜单图标/
-- 密码策略列/2FA字段/监控中心/Open API密钥/回收站软删除列/公告定时发布/
-- 数据大屏/代码生成器的表结构与菜单、配置种子，新库一次执行到位。
-- 首次启动由 DatabaseInitializer 自动执行（classpath下同名脚本），
-- 也可手动 source。演示账号：root / 12345678（所有演示用户同密码）。
-- =============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '租户id(0为平台层)',
  `tenant_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编码',
  `tenant_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户名称',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '状态(0禁用1启用)',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `package_id` int(11) NULL DEFAULT NULL COMMENT '绑定套餐id',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间(空为永不过期)',
  `user_limit` int(11) NULL DEFAULT NULL COMMENT '用户数上限(空为不限制)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_code`(`tenant_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `parent_id` int(40) NOT NULL COMMENT '父级id',
  `menu_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `route` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单路由地址',
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单状态',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单类型 (0: 目录, 1: 菜单, 2: 按钮)',
  `level` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'level',
  `component_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由组件地址',
  `component_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由组件名称',
  `perms` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限标识(按钮类型菜单使用,如user:remove)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30203 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `role_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `role_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色描述',
  `tenant_id` int(11) NOT NULL DEFAULT 1 COMMENT '租户id(0为平台)',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '数据权限(1本租户全部2本部门及以下3仅本人)',
  `parent_id` int(11) NULL DEFAULT 0 COMMENT '父角色id(0为顶级，菜单权限沿父链继承)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限描述',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` int(11) NOT NULL DEFAULT 0 COMMENT '父部门id(0为根)',
  `dept_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门名称',
  `sort` int(11) NULL DEFAULT 0 COMMENT '显示顺序',
  `tenant_id` int(11) NOT NULL DEFAULT 1 COMMENT '租户id',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '公告id',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '公告内容',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态(0草稿1发布)',
  `tenant_id` int(11) NOT NULL DEFAULT 0 COMMENT '租户id(0为平台全员可见)',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '发布人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通知公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_tenant_package
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant_package`;
CREATE TABLE `sys_tenant_package`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '套餐id',
  `package_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '套餐名称',
  `menu_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '关联菜单id(逗号分隔)',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '状态(0停用1启用)',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户套餐表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` int(40) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账号',
  `mobile` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户头像地址',
  `tenant_id` int(11) NOT NULL DEFAULT 1 COMMENT '租户id(0为平台)',
  `dept_id` int(11) NULL DEFAULT NULL COMMENT '部门id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for role_menu
-- ----------------------------
DROP TABLE IF EXISTS `role_menu`;
CREATE TABLE `role_menu`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `menu_id` int(11) NOT NULL COMMENT '菜单id',
  `role_id` int(11) NOT NULL COMMENT '角色id',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_role_role_sys_menu_1`(`role_id`) USING BTREE,
  INDEX `fk_role_role_sys_menu_2`(`menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 600 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `role_id` int(11) NOT NULL COMMENT '用户id',
  `permission_id` int(11) NOT NULL COMMENT '权限id',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_role_permission_sys_permission_1`(`permission_id`) USING BTREE,
  INDEX `fk_role_permission_sys_role_1`(`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role`  (
  `id` int(40) NOT NULL AUTO_INCREMENT,
  `user_id` int(40) NOT NULL COMMENT '用户id',
  `role_id` int(40) NOT NULL COMMENT '角色id',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_user_role_sys_role_1`(`role_id`) USING BTREE,
  INDEX `fk_user_role_sys_user_1`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 70 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色表' ROW_FORMAT = Dynamic;

-- Now, set the foreign key constraints
ALTER TABLE `role_menu`
  ADD CONSTRAINT `fk_role_role_sys_menu_1` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_role_role_sys_menu_2` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `role_permission`
  ADD CONSTRAINT `fk_role_permission_sys_permission_1` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_role_permission_sys_role_1` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `user_role`
  ADD CONSTRAINT `fk_user_role_sys_role_1` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_user_role_sys_user_1` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- ----------------------------
-- Insert records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (2, 0, '系统管理', '/system', '1', '0', '1', NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (201, 2, '权限管理', '/permissions-manager', '1', '0', '2', NULL, NULL, NULL);
INSERT INTO `sys_menu` VALUES (20101, 201, '角色管理', '/role-manager', '1', '1', '3', '@/views/role-manager/Index.vue', 'role-manager', NULL);
INSERT INTO `sys_menu` VALUES (20102, 201, '用户管理', '/user-manager', '1', '1', '3', '@/views/user-manager/Index.vue', 'user-manager', NULL);
INSERT INTO `sys_menu` VALUES (20103, 201, '菜单管理', '/menu-manager', '1', '1', '3', '@/views/menu-manager/Index.vue', 'menu-manager', NULL);
INSERT INTO `sys_menu` VALUES (20104, 201, '部门管理', '/dept-manager', '1', '1', '3', '@/views/dept-manager/Index.vue', 'dept-manager', NULL);

-- 按钮(权限点)：前端v-permission与后端@PreAuthorize使用
INSERT INTO `sys_menu` VALUES (2010101, 20101, '角色编辑', NULL, '1', '2', '4', NULL, NULL, 'role:write');
INSERT INTO `sys_menu` VALUES (2010102, 20101, '角色删除', NULL, '1', '2', '4', NULL, NULL, 'role:remove');
INSERT INTO `sys_menu` VALUES (2010201, 20102, '用户编辑', NULL, '1', '2', '4', NULL, NULL, 'user:write');
INSERT INTO `sys_menu` VALUES (2010202, 20102, '用户删除', NULL, '1', '2', '4', NULL, NULL, 'user:remove');
INSERT INTO `sys_menu` VALUES (2010301, 20103, '菜单编辑', NULL, '1', '2', '4', NULL, NULL, 'menu:write');
INSERT INTO `sys_menu` VALUES (2010302, 20103, '菜单删除', NULL, '1', '2', '4', NULL, NULL, 'menu:remove');
INSERT INTO `sys_menu` VALUES (2010401, 20104, '部门编辑', NULL, '1', '2', '5', NULL, NULL, 'dept:write');
INSERT INTO `sys_menu` VALUES (2010402, 20104, '部门删除', NULL, '1', '2', '5', NULL, NULL, 'dept:remove');
INSERT INTO `sys_menu` VALUES (202, 2, '日志管理', '/log-manager', '1', '1', '2', '@/views/log-manager/Index.vue', 'log-manager', NULL);
INSERT INTO `sys_menu` VALUES (20201, 202, '日志查询', NULL, '1', '2', '3', NULL, NULL, 'log:read');
INSERT INTO `sys_menu` VALUES (203, 2, '通知公告', '/notice-manager', '1', '1', '2', '@/views/notice-manager/Index.vue', 'notice-manager', NULL);
INSERT INTO `sys_menu` VALUES (20301, 203, '公告编辑', NULL, '1', '2', '3', NULL, NULL, 'notice:write');
INSERT INTO `sys_menu` VALUES (20302, 203, '公告删除', NULL, '1', '2', '3', NULL, NULL, 'notice:remove');
INSERT INTO `sys_menu` VALUES (204, 2, '在线用户', '/online-manager', '1', '1', '2', '@/views/online-manager/Index.vue', 'online-manager', NULL);
INSERT INTO `sys_menu` VALUES (20401, 204, '在线查询', NULL, '1', '2', '3', NULL, NULL, 'online:read');
INSERT INTO `sys_menu` VALUES (20402, 204, '强制下线', NULL, '1', '2', '3', NULL, NULL, 'online:kick');
INSERT INTO `sys_menu` VALUES (3, 0, '平台管理', '/tenant-manager', '1', '1', '1', '@/views/tenant-manager/Index.vue', 'tenant-manager', NULL);
INSERT INTO `sys_menu` VALUES (301, 3, '租户管理', NULL, '1', '2', '2', NULL, NULL, 'tenant:manage');
INSERT INTO `sys_menu` VALUES (302, 3, '租户套餐', '/package-manager', '1', '1', '2', '@/views/package-manager/Index.vue', 'package-manager', NULL);
INSERT INTO `sys_menu` VALUES (30201, 302, '套餐编辑', NULL, '1', '2', '3', NULL, NULL, 'package:write');
INSERT INTO `sys_menu` VALUES (30202, 302, '套餐删除', NULL, '1', '2', '3', NULL, NULL, 'package:remove');

-- ----------------------------
-- Insert records of sys_tenant / sys_role
-- ----------------------------
INSERT INTO `sys_tenant` VALUES (1, 'default', '默认租户', '1', '2026-08-29 00:00:00', NULL, 1, NULL, NULL);
INSERT INTO `sys_tenant` VALUES (10, 'tech', '科技有限公司', '1', '2026-08-29 00:00:00', NULL, 3, NULL, NULL);
INSERT INTO `sys_tenant` VALUES (11, 'trade', '贸易有限公司', '1', '2026-08-29 00:00:00', NULL, 2, NULL, NULL);
INSERT INTO `sys_tenant` VALUES (12, 'trial', '试用租户', '1', '2026-08-29 00:00:00', NULL, 3, '2027-12-31 23:59:59', 3);
INSERT INTO `sys_tenant` VALUES (13, 'expired', '过期租户', '1', '2026-08-29 00:00:00', NULL, 1, '2026-01-01 00:00:00', NULL);
INSERT INTO `sys_role` VALUES (100, 'super', '平台超级管理员', '平台层，跨租户', 0, '1', 0);
INSERT INTO `sys_role` VALUES (102, 'root', '管理员', '管理员', 1, '1', 0);
INSERT INTO `sys_role` VALUES (150, 'tech_admin', '科技有限公司管理员', '开通租户自动创建', 10, '1', 0);
INSERT INTO `sys_role` VALUES (151, 'dev', '研发工程师', '数据权限-仅本人', 10, '3', 0);
INSERT INTO `sys_role` VALUES (152, 'prod', '产品经理', '数据权限-本部门及以下', 10, '2', 0);
INSERT INTO `sys_role` VALUES (153, 'trade_admin', '贸易有限公司管理员', '开通租户自动创建', 11, '1', 0);
INSERT INTO `sys_role` VALUES (154, 'sales', '销售专员', '数据权限-仅本人', 11, '3', 0);
INSERT INTO `sys_role` VALUES (155, 'trial_admin', '试用租户管理员', '开通租户自动创建', 12, '1', 0);
INSERT INTO `sys_role` VALUES (156, 'expired_admin', '过期租户管理员', '开通租户自动创建', 13, '1', 0);

-- ----------------------------
-- Insert records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (1, 0, '总部', 1, 1, '2026-08-29 00:00:00', NULL);
INSERT INTO `sys_dept` VALUES (2, 1, '研发部', 1, 1, '2026-08-29 00:00:00', NULL);
INSERT INTO `sys_dept` VALUES (3, 1, '市场部', 2, 1, '2026-08-29 00:00:00', NULL);
INSERT INTO `sys_dept` VALUES (20, 0, '科技总部', 1, 10, '2026-08-29 00:00:00', NULL);
INSERT INTO `sys_dept` VALUES (21, 20, '研发部', 1, 10, '2026-08-29 00:00:00', NULL);
INSERT INTO `sys_dept` VALUES (22, 20, '产品部', 2, 10, '2026-08-29 00:00:00', NULL);
INSERT INTO `sys_dept` VALUES (23, 0, '贸易总部', 1, 11, '2026-08-29 00:00:00', NULL);
INSERT INTO `sys_dept` VALUES (24, 23, '销售部', 1, 11, '2026-08-29 00:00:00', NULL);
INSERT INTO `sys_dept` VALUES (25, 23, '采购部', 2, 11, '2026-08-29 00:00:00', NULL);

-- ----------------------------
-- Insert records of sys_tenant_package
-- ----------------------------
INSERT INTO `sys_tenant_package` VALUES (1, '标准套餐', '2,201,20101,2010101,2010102,20102,2010201,2010202,20103,2010301,2010302,20104,2010401,2010402,202,20201', '1', '权限管理与日志管理全套', '2026-08-29 00:00:00', NULL);
INSERT INTO `sys_tenant_package` VALUES (2, '基础套餐', '2,201,20101,2010101,20102,2010201,203,20301,20302', '1', '用户/角色基础管理与通知公告（不含删除类权限）', '2026-08-29 00:00:00', NULL);
INSERT INTO `sys_tenant_package` VALUES (3, '专业套餐', '2,201,20101,2010101,2010102,20102,2010201,2010202,20103,2010301,2010302,20104,2010401,2010402,202,20201,203,20301,20302,204,20401,20402', '1', '系统管理全套+通知公告+在线用户', '2026-08-29 00:00:00', NULL);

-- ----------------------------
-- Insert records of sys_user（演示账号密码均为12345678）
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, '平台管理员', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'admin', NULL, NULL, '2026-08-29 00:00:00', NULL, NULL, NULL, 0, NULL);
INSERT INTO `sys_user` VALUES (2, '超级管理员', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'root', '185****6496', '1975922551@qq.com', '2025-02-13 18:12:42', '2025-02-14 11:28:14', '2025-02-13 18:12:54', NULL, 1, 2);
INSERT INTO `sys_user` VALUES (10, '科技管理员', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'techadmin', NULL, NULL, '2026-08-29 00:00:00', NULL, NULL, NULL, 10, 20);
INSERT INTO `sys_user` VALUES (11, '王伟', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'wangwei', NULL, NULL, '2026-08-29 00:00:00', NULL, NULL, NULL, 10, 21);
INSERT INTO `sys_user` VALUES (12, '李丽', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'lili', NULL, NULL, '2026-08-29 00:00:00', NULL, NULL, NULL, 10, 22);
INSERT INTO `sys_user` VALUES (13, '贸易管理员', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'tradeadmin', NULL, NULL, '2026-08-29 00:00:00', NULL, NULL, NULL, 11, 23);
INSERT INTO `sys_user` VALUES (14, '张三', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'zhangsan', NULL, NULL, '2026-08-29 00:00:00', NULL, NULL, NULL, 11, 24);
INSERT INTO `sys_user` VALUES (15, '试用管理员', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'trialadmin', NULL, NULL, '2026-08-29 00:00:00', NULL, NULL, NULL, 12, NULL);
INSERT INTO `sys_user` VALUES (16, '过期管理员', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'expiredadmin', NULL, NULL, '2026-08-29 00:00:00', NULL, NULL, NULL, 13, NULL);

-- ----------------------------
-- Insert records of role_menu
-- ----------------------------
INSERT INTO `role_menu` VALUES (37, 2, 102);
INSERT INTO `role_menu` VALUES (38, 201, 102);
INSERT INTO `role_menu` VALUES (39, 20101, 102);
INSERT INTO `role_menu` VALUES (40, 20102, 102);
INSERT INTO `role_menu` VALUES (41, 20103, 102);
INSERT INTO `role_menu` VALUES (42, 2010101, 102);
INSERT INTO `role_menu` VALUES (43, 2010102, 102);
INSERT INTO `role_menu` VALUES (44, 2010201, 102);
INSERT INTO `role_menu` VALUES (45, 2010202, 102);
INSERT INTO `role_menu` VALUES (46, 2010301, 102);
INSERT INTO `role_menu` VALUES (47, 2010302, 102);
INSERT INTO `role_menu` VALUES (48, 202, 102);
INSERT INTO `role_menu` VALUES (49, 20201, 102);
INSERT INTO `role_menu` VALUES (50, 2, 100);
INSERT INTO `role_menu` VALUES (51, 201, 100);
INSERT INTO `role_menu` VALUES (52, 20101, 100);
INSERT INTO `role_menu` VALUES (53, 20102, 100);
INSERT INTO `role_menu` VALUES (54, 20103, 100);
INSERT INTO `role_menu` VALUES (55, 2010101, 100);
INSERT INTO `role_menu` VALUES (56, 2010102, 100);
INSERT INTO `role_menu` VALUES (57, 2010201, 100);
INSERT INTO `role_menu` VALUES (58, 2010202, 100);
INSERT INTO `role_menu` VALUES (59, 2010301, 100);
INSERT INTO `role_menu` VALUES (60, 2010302, 100);
INSERT INTO `role_menu` VALUES (61, 202, 100);
INSERT INTO `role_menu` VALUES (62, 20201, 100);
INSERT INTO `role_menu` VALUES (63, 3, 100);
INSERT INTO `role_menu` VALUES (64, 301, 100);
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

-- 演示租户角色菜单：150/155=专业套餐全套，153=基础套餐，156=标准套餐，151/152/154=仅用户管理页
INSERT INTO `role_menu` VALUES (500, 2, 150);
INSERT INTO `role_menu` VALUES (501, 201, 150);
INSERT INTO `role_menu` VALUES (502, 20101, 150);
INSERT INTO `role_menu` VALUES (503, 2010101, 150);
INSERT INTO `role_menu` VALUES (504, 2010102, 150);
INSERT INTO `role_menu` VALUES (505, 20102, 150);
INSERT INTO `role_menu` VALUES (506, 2010201, 150);
INSERT INTO `role_menu` VALUES (507, 2010202, 150);
INSERT INTO `role_menu` VALUES (508, 20103, 150);
INSERT INTO `role_menu` VALUES (509, 2010301, 150);
INSERT INTO `role_menu` VALUES (510, 2010302, 150);
INSERT INTO `role_menu` VALUES (511, 20104, 150);
INSERT INTO `role_menu` VALUES (512, 2010401, 150);
INSERT INTO `role_menu` VALUES (513, 2010402, 150);
INSERT INTO `role_menu` VALUES (514, 202, 150);
INSERT INTO `role_menu` VALUES (515, 20201, 150);
INSERT INTO `role_menu` VALUES (516, 203, 150);
INSERT INTO `role_menu` VALUES (517, 20301, 150);
INSERT INTO `role_menu` VALUES (518, 20302, 150);
INSERT INTO `role_menu` VALUES (519, 204, 150);
INSERT INTO `role_menu` VALUES (520, 20401, 150);
INSERT INTO `role_menu` VALUES (521, 20402, 150);
INSERT INTO `role_menu` VALUES (522, 2, 151);
INSERT INTO `role_menu` VALUES (523, 201, 151);
INSERT INTO `role_menu` VALUES (524, 20102, 151);
INSERT INTO `role_menu` VALUES (525, 2, 152);
INSERT INTO `role_menu` VALUES (526, 201, 152);
INSERT INTO `role_menu` VALUES (527, 20102, 152);
INSERT INTO `role_menu` VALUES (528, 2, 153);
INSERT INTO `role_menu` VALUES (529, 201, 153);
INSERT INTO `role_menu` VALUES (530, 20101, 153);
INSERT INTO `role_menu` VALUES (531, 2010101, 153);
INSERT INTO `role_menu` VALUES (532, 20102, 153);
INSERT INTO `role_menu` VALUES (533, 2010201, 153);
INSERT INTO `role_menu` VALUES (534, 203, 153);
INSERT INTO `role_menu` VALUES (535, 20301, 153);
INSERT INTO `role_menu` VALUES (536, 20302, 153);
INSERT INTO `role_menu` VALUES (537, 2, 154);
INSERT INTO `role_menu` VALUES (538, 201, 154);
INSERT INTO `role_menu` VALUES (539, 20102, 154);
INSERT INTO `role_menu` VALUES (540, 2, 155);
INSERT INTO `role_menu` VALUES (541, 201, 155);
INSERT INTO `role_menu` VALUES (542, 20101, 155);
INSERT INTO `role_menu` VALUES (543, 2010101, 155);
INSERT INTO `role_menu` VALUES (544, 2010102, 155);
INSERT INTO `role_menu` VALUES (545, 20102, 155);
INSERT INTO `role_menu` VALUES (546, 2010201, 155);
INSERT INTO `role_menu` VALUES (547, 2010202, 155);
INSERT INTO `role_menu` VALUES (548, 20103, 155);
INSERT INTO `role_menu` VALUES (549, 2010301, 155);
INSERT INTO `role_menu` VALUES (550, 2010302, 155);
INSERT INTO `role_menu` VALUES (551, 20104, 155);
INSERT INTO `role_menu` VALUES (552, 2010401, 155);
INSERT INTO `role_menu` VALUES (553, 2010402, 155);
INSERT INTO `role_menu` VALUES (554, 202, 155);
INSERT INTO `role_menu` VALUES (555, 20201, 155);
INSERT INTO `role_menu` VALUES (556, 203, 155);
INSERT INTO `role_menu` VALUES (557, 20301, 155);
INSERT INTO `role_menu` VALUES (558, 20302, 155);
INSERT INTO `role_menu` VALUES (559, 204, 155);
INSERT INTO `role_menu` VALUES (560, 20401, 155);
INSERT INTO `role_menu` VALUES (561, 20402, 155);
INSERT INTO `role_menu` VALUES (562, 2, 156);
INSERT INTO `role_menu` VALUES (563, 201, 156);
INSERT INTO `role_menu` VALUES (564, 20101, 156);
INSERT INTO `role_menu` VALUES (565, 2010101, 156);
INSERT INTO `role_menu` VALUES (566, 2010102, 156);
INSERT INTO `role_menu` VALUES (567, 20102, 156);
INSERT INTO `role_menu` VALUES (568, 2010201, 156);
INSERT INTO `role_menu` VALUES (569, 2010202, 156);
INSERT INTO `role_menu` VALUES (570, 20103, 156);
INSERT INTO `role_menu` VALUES (571, 2010301, 156);
INSERT INTO `role_menu` VALUES (572, 2010302, 156);
INSERT INTO `role_menu` VALUES (573, 20104, 156);
INSERT INTO `role_menu` VALUES (574, 2010401, 156);
INSERT INTO `role_menu` VALUES (575, 2010402, 156);
INSERT INTO `role_menu` VALUES (576, 202, 156);
INSERT INTO `role_menu` VALUES (577, 20201, 156);

-- ----------------------------
-- Insert records of user_role
-- ----------------------------
INSERT INTO `user_role` VALUES (5, 2, 102);
INSERT INTO `user_role` VALUES (6, 1, 100);
INSERT INTO `user_role` VALUES (60, 10, 150);
INSERT INTO `user_role` VALUES (61, 11, 151);
INSERT INTO `user_role` VALUES (62, 12, 152);
INSERT INTO `user_role` VALUES (63, 13, 153);
INSERT INTO `user_role` VALUES (64, 14, 154);
INSERT INTO `user_role` VALUES (65, 15, 155);
INSERT INTO `user_role` VALUES (66, 16, 156);


-- ----------------------------
-- 三权分立预置角色（等保场景）：系统管理员root之外，预置安全管理员与审计员
-- security=安全管理员（用户/角色/菜单/部门/岗位/参数配置），auditor=审计员（仅日志查看与导出）
-- 角色未绑定账号，可通过角色管理按需调整后授予用户
-- ----------------------------
INSERT INTO `sys_role` VALUES (103, 'security', '安全管理员', '三权分立-安全员：账号权限与安全参数配置', 0, '1', 0);
INSERT INTO `sys_role` VALUES (104, 'auditor', '审计员', '三权分立-审计员：仅日志查看与导出报告', 0, '1', 0);
INSERT INTO `role_menu` VALUES (800, 2, 103);
INSERT INTO `role_menu` VALUES (801, 201, 103);
INSERT INTO `role_menu` VALUES (802, 20101, 103);
INSERT INTO `role_menu` VALUES (803, 2010101, 103);
INSERT INTO `role_menu` VALUES (804, 2010102, 103);
INSERT INTO `role_menu` VALUES (805, 20102, 103);
INSERT INTO `role_menu` VALUES (806, 2010201, 103);
INSERT INTO `role_menu` VALUES (807, 2010202, 103);
INSERT INTO `role_menu` VALUES (808, 20103, 103);
INSERT INTO `role_menu` VALUES (809, 2010301, 103);
INSERT INTO `role_menu` VALUES (810, 2010302, 103);
INSERT INTO `role_menu` VALUES (811, 20104, 103);
INSERT INTO `role_menu` VALUES (812, 2010401, 103);
INSERT INTO `role_menu` VALUES (813, 2010402, 103);
INSERT INTO `role_menu` VALUES (814, 20105, 103);
INSERT INTO `role_menu` VALUES (815, 2010501, 103);
INSERT INTO `role_menu` VALUES (816, 2010502, 103);
INSERT INTO `role_menu` VALUES (817, 206, 103);
INSERT INTO `role_menu` VALUES (818, 20601, 103);
INSERT INTO `role_menu` VALUES (820, 202, 104);
INSERT INTO `role_menu` VALUES (821, 20201, 104);

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
-- 以下为原 V4~V7 增量内容（合并版，按顺序执行即得完整库结构）
-- =============================================================

-- ------------------ 原 V4__platform_base ------------------

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

-- ------------------ 原 V5__two_fa_monitor ------------------

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

-- ------------------ 原 V6__openapi_health_2fa ------------------

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

-- ------------------ 原 V7__recycle_notice_screen_gen ------------------

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

-- ----------------------------
-- 4. 定时发布/回收站/数据大屏/代码生成菜单（幂等）
-- ----------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 208, 2, '回收站', '/recycle-manager', '1', '1', '2', '@/views/recycle-manager/Index.vue', 'recycle-manager', NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 208);
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `route`, `status`, `type`, `level`, `component_path`, `component_name`, `perms`)
SELECT 20801, 208, '回收站管理', NULL, '1', '2', '3', NULL, NULL, 'recycle:manage'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 20801);
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
-- 5. 授权（幂等）：root=回收站；super=全部
-- ----------------------------
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 724, 208, 102 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 724);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 725, 20801, 102 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 725);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 727, 208, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 727);
INSERT INTO `role_menu` (`id`, `menu_id`, `role_id`) SELECT 728, 20801, 100 WHERE NOT EXISTS (SELECT 1 FROM role_menu WHERE id = 728);
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
