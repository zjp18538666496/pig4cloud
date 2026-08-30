-- =============================================================
-- 演示数据补种脚本 2026-08-30（第二批）
-- 适用：已初始化过的老库，补充多公司演示数据（新库无需执行，init脚本已内置）。
-- 后端启动时自动检测（不存在租户编码tech则执行本脚本），也可手动 source。
-- 内容：基础/专业两个套餐，科技/贸易/试用/过期四个演示租户及各部门、
--       角色（含不同数据权限）、账号（密码均为12345678）与授权关系。
-- 注意：脚本由 continueOnError 模式执行，数据已存在导致的报错可忽略。
-- =============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 1. 套餐：基础/专业
-- ----------------------------
INSERT INTO `sys_tenant_package` VALUES (2, '基础套餐', '2,201,20101,2010101,20102,2010201,203,20301,20302', '1', '用户/角色基础管理与通知公告（不含删除类权限）', NOW(), NULL);
INSERT INTO `sys_tenant_package` VALUES (3, '专业套餐', '2,201,20101,2010101,2010102,20102,2010201,2010202,20103,2010301,2010302,20104,2010401,2010402,202,20201,203,20301,20302,204,20401,20402', '1', '系统管理全套+通知公告+在线用户', NOW(), NULL);

-- ----------------------------
-- 2. 演示租户：科技(专业套餐)/贸易(基础套餐)/试用(专业+配额3+2027到期)/过期(标准+已过期)
-- ----------------------------
INSERT INTO `sys_tenant` VALUES (10, 'tech', '科技有限公司', '1', NOW(), NULL, 3, NULL, NULL);
INSERT INTO `sys_tenant` VALUES (11, 'trade', '贸易有限公司', '1', NOW(), NULL, 2, NULL, NULL);
INSERT INTO `sys_tenant` VALUES (12, 'trial', '试用租户', '1', NOW(), NULL, 3, '2027-12-31 23:59:59', 3);
INSERT INTO `sys_tenant` VALUES (13, 'expired', '过期租户', '1', NOW(), NULL, 1, '2026-01-01 00:00:00', NULL);

-- ----------------------------
-- 3. 部门架构：科技(总部->研发/产品)、贸易(总部->销售/采购)
-- ----------------------------
INSERT INTO `sys_dept` VALUES (20, 0, '科技总部', 1, 10, NOW(), NULL);
INSERT INTO `sys_dept` VALUES (21, 20, '研发部', 1, 10, NOW(), NULL);
INSERT INTO `sys_dept` VALUES (22, 20, '产品部', 2, 10, NOW(), NULL);
INSERT INTO `sys_dept` VALUES (23, 0, '贸易总部', 1, 11, NOW(), NULL);
INSERT INTO `sys_dept` VALUES (24, 23, '销售部', 1, 11, NOW(), NULL);
INSERT INTO `sys_dept` VALUES (25, 23, '采购部', 2, 11, NOW(), NULL);

-- ----------------------------
-- 4. 角色：各租户管理员 + 不同数据权限的业务角色
-- ----------------------------
INSERT INTO `sys_role` VALUES (150, 'tech_admin', '科技有限公司管理员', '开通租户自动创建', 10, '1', 0);
INSERT INTO `sys_role` VALUES (151, 'dev', '研发工程师', '数据权限-仅本人', 10, '3', 0);
INSERT INTO `sys_role` VALUES (152, 'prod', '产品经理', '数据权限-本部门及以下', 10, '2', 0);
INSERT INTO `sys_role` VALUES (153, 'trade_admin', '贸易有限公司管理员', '开通租户自动创建', 11, '1', 0);
INSERT INTO `sys_role` VALUES (154, 'sales', '销售专员', '数据权限-仅本人', 11, '3', 0);
INSERT INTO `sys_role` VALUES (155, 'trial_admin', '试用租户管理员', '开通租户自动创建', 12, '1', 0);
INSERT INTO `sys_role` VALUES (156, 'expired_admin', '过期租户管理员', '开通租户自动创建', 13, '1', 0);

-- ----------------------------
-- 5. 账号（密码均为12345678）
-- ----------------------------
INSERT INTO `sys_user` VALUES (10, '科技管理员', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'techadmin', NULL, NULL, NOW(), NULL, NULL, NULL, 10, 20);
INSERT INTO `sys_user` VALUES (11, '王伟', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'wangwei', NULL, NULL, NOW(), NULL, NULL, NULL, 10, 21);
INSERT INTO `sys_user` VALUES (12, '李丽', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'lili', NULL, NULL, NOW(), NULL, NULL, NULL, 10, 22);
INSERT INTO `sys_user` VALUES (13, '贸易管理员', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'tradeadmin', NULL, NULL, NOW(), NULL, NULL, NULL, 11, 23);
INSERT INTO `sys_user` VALUES (14, '张三', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'zhangsan', NULL, NULL, NOW(), NULL, NULL, NULL, 11, 24);
INSERT INTO `sys_user` VALUES (15, '试用管理员', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'trialadmin', NULL, NULL, NOW(), NULL, NULL, NULL, 12, NULL);
INSERT INTO `sys_user` VALUES (16, '过期管理员', '$2a$10$9Ifg5wOb6hu94S0TcUdl/u94Uv5SMXucJPFTeQ2WeUhlWdx.z34l2', 'expiredadmin', NULL, NULL, NOW(), NULL, NULL, NULL, 13, NULL);

-- ----------------------------
-- 6. 角色菜单：150/155=专业套餐全套，153=基础套餐，156=标准套餐，151/152/154=仅用户管理页
-- ----------------------------
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
-- 7. 用户角色绑定
-- ----------------------------
INSERT INTO `user_role` VALUES (60, 10, 150);
INSERT INTO `user_role` VALUES (61, 11, 151);
INSERT INTO `user_role` VALUES (62, 12, 152);
INSERT INTO `user_role` VALUES (63, 13, 153);
INSERT INTO `user_role` VALUES (64, 14, 154);
INSERT INTO `user_role` VALUES (65, 15, 155);
INSERT INTO `user_role` VALUES (66, 16, 156);
