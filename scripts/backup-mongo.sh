#!/usr/bin/env bash
# ============================================================
# MongoDB日志库备份脚本（配合crontab执行）
# 环境变量：MONGO_URI/BACKUP_DIR/KEEP_DAYS
# ============================================================
set -e

MONGO_URI="${MONGO_URI:-mongodb://root:123456@10.126.126.3:27017/pigx_log?authSource=admin}"
BACKUP_DIR="${BACKUP_DIR:-/opt/pigx-admin/backups/mongo}"
KEEP_DAYS="${KEEP_DAYS:-7}"

mkdir -p "$BACKUP_DIR"
STAMP=$(date +%Y%m%d_%H%M%S)

mongodump --uri "$MONGO_URI" --archive="$BACKUP_DIR/pigx_log_$STAMP.archive" --gzip
echo "备份完成: $BACKUP_DIR/pigx_log_$STAMP.archive"

find "$BACKUP_DIR" -name "pigx_log_*.archive" -mtime +"$KEEP_DAYS" -delete
echo "已清理${KEEP_DAYS}天前的旧备份"
