#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MYSQL_HOME="$ROOT_DIR/.runtime/mysql-8.4.13-macos15-arm64"
RUN_DIR="$ROOT_DIR/.runtime/mysql-run"
SOCKET="$RUN_DIR/mysql.sock"

if [[ -S "$SOCKET" ]]; then
  "$MYSQL_HOME/bin/mysqladmin" --socket="$SOCKET" -uroot shutdown
  echo "MySQL 已停止"
else
  echo "没有发现运行中的 MySQL socket：$SOCKET"
fi
