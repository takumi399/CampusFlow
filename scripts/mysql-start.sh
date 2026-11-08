#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MYSQL_HOME="$ROOT_DIR/.runtime/mysql-8.4.13-macos15-arm64"
DATA_DIR="$ROOT_DIR/.runtime/mysql-data"
RUN_DIR="$ROOT_DIR/.runtime/mysql-run"
SOCKET="$RUN_DIR/mysql.sock"
PID_FILE="$RUN_DIR/mysql.pid"
LOG_FILE="$RUN_DIR/mysql.log"

if [[ ! -x "$MYSQL_HOME/bin/mysqld" ]]; then
  echo "MySQL 8.4.13 未安装。请把 mysql-8.4.13-macos15-arm64 解压到 $ROOT_DIR/.runtime/。" >&2
  exit 1
fi

mkdir -p "$DATA_DIR" "$RUN_DIR"

if [[ ! -d "$DATA_DIR/mysql" ]]; then
  echo "初始化 MySQL 数据目录..."
  "$MYSQL_HOME/bin/mysqld" \
    --initialize-insecure \
    --basedir="$MYSQL_HOME" \
    --datadir="$DATA_DIR" \
    --user="$(whoami)"
fi

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "MySQL 已经在运行，PID=$(cat "$PID_FILE")"
  exit 0
fi

echo "启动 MySQL..."
nohup "$MYSQL_HOME/bin/mysqld" \
  --basedir="$MYSQL_HOME" \
  --datadir="$DATA_DIR" \
  --port=3306 \
  --socket="$SOCKET" \
  --pid-file="$PID_FILE" \
  --log-error="$LOG_FILE" \
  --mysqlx=0 \
  --bind-address=127.0.0.1 \
  >/dev/null 2>&1 &

for _ in $(seq 1 30); do
  if "$MYSQL_HOME/bin/mysqladmin" --socket="$SOCKET" -uroot ping >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! "$MYSQL_HOME/bin/mysqladmin" --socket="$SOCKET" -uroot ping >/dev/null 2>&1; then
  echo "MySQL 启动失败，查看日志：$LOG_FILE" >&2
  exit 1
fi

"$MYSQL_HOME/bin/mysql" --socket="$SOCKET" -uroot <<'SQL'
CREATE DATABASE IF NOT EXISTS campus_event CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS campus_event_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'campus'@'127.0.0.1' IDENTIFIED BY 'campus_dev_password';
CREATE USER IF NOT EXISTS 'campus'@'localhost' IDENTIFIED BY 'campus_dev_password';
GRANT ALL PRIVILEGES ON campus_event.* TO 'campus'@'127.0.0.1';
GRANT ALL PRIVILEGES ON campus_event.* TO 'campus'@'localhost';
GRANT ALL PRIVILEGES ON campus_event_test.* TO 'campus'@'127.0.0.1';
GRANT ALL PRIVILEGES ON campus_event_test.* TO 'campus'@'localhost';
FLUSH PRIVILEGES;
SQL

echo "MySQL 已启动：127.0.0.1:3306"
echo "开发库：campus_event，测试库：campus_event_test"
