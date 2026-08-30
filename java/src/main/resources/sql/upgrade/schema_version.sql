-- 版本化迁移登记表（由DatabaseInitializer在执行V*脚本前自动确保存在）
CREATE TABLE IF NOT EXISTS `sys_schema_version`  (
  `version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '版本号',
  `script_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '脚本文件名',
  `applied_at` datetime NULL DEFAULT NULL COMMENT '执行时间',
  PRIMARY KEY (`version`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据库脚本版本表' ROW_FORMAT = Dynamic;
