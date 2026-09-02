#!/usr/bin/env bash
# ============================================================
# MySQL定时备份脚本（配合crontab每日凌晨执行）
#   crontab -e: 0 2 * * * /opt/pigx-admin/scripts/backup-mysql.sh
# 环境变量：MYSQL_HOST/MYSQL_PORT/MYSQL_USER/MYSQL_PASS/MYSQL_DB/BACKUP_DIR
# ============================================================
set -e

MYSQL_HOST="${MYSQL_HOST:-10.126.126.3}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASS="${MYSQL_PASS:?请设置MYSQL_PASS}"
MYSQL_DB="${MYSQL_DB:-pigx_admin}"
BACKUP_DIR="${BACKUP_DIR:-/opt/pigx-admin/backups/mysql}"
KEEP_DAYS="${KEEP_DAYS:-7}"

mkdir -p "$BACKUP_DIR"
FILE="$BACKUP_DIR/${MYSQL_DB}_$(date +%Y%m%d_%H%M%S).sql.gz"

mysqldump -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASS" \
  --single-transaction --routines --triggers "$MYSQL_DB" | gzip > "$FILE"

echo "备份完成: $FILE ($(du -h "$FILE" | cut -f1))"

# 清理过期备份
find "$BACKUP_DIR" -name "${MYSQL_DB}_*.sql.gz" -mtime +"$KEEP_DAYS" -delete
echo "已清理${KEEP_DAYS}天前的旧备份"
