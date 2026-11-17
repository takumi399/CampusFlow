#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MYSQL_HOME="$ROOT_DIR/.runtime/mysql-8.4.13-macos15-arm64"
RUN_DIR="$ROOT_DIR/.runtime/mysql-run"
SOCKET="$RUN_DIR/mysql.sock"

if [[ -S "$SOCKET" ]] && "$MYSQL_HOME/bin/mysqladmin" --socket="$SOCKET" -uroot ping >/dev/null 2>&1; then
  echo "MySQL 正在运行"
else
  echo "MySQL 未运行"
  exit 1
fi
