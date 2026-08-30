-- =============================================================
-- 结构升级脚本 2026-08-30（第三批）：角色层级
-- 适用：已初始化过的老库。sys_role 增加 parent_id 列，支持角色父子层级，
--       子角色沿父链继承菜单/按钮权限（角色编码与数据权限不继承）。
-- 后端启动时自动检测（sys_role.parent_id 列不存在则执行），也可手动 source。
-- 注意：脚本由 continueOnError 模式执行，列已存在导致的报错可忽略。
-- =============================================================
SET NAMES utf8mb4;

ALTER TABLE `sys_role`
  ADD COLUMN `parent_id` int(11) NULL DEFAULT 0 COMMENT '父角色id(0为顶级，菜单权限沿父链继承)' AFTER `data_scope`;
