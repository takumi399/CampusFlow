#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080/api}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

pass() {
  printf 'PASS  %s\n' "$1"
}

fail() {
  printf 'FAIL  %s\n' "$1" >&2
  exit 1
}

login() {
  local username="$1"
  local password="$2"

  curl -fsS \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${username}\",\"password\":\"${password}\"}" \
    "${BASE_URL}/auth/login" | jq -er '.data.token'
}

api_get() {
  local token="$1"
  local path="$2"

  curl -fsS -H "Authorization: Bearer ${token}" "${BASE_URL}${path}"
}

printf 'CampusFlow smoke test\n'
printf 'Target: %s\n\n' "$BASE_URL"

ADMIN_TOKEN="$(login admin admin123)"
STUDENT_TOKEN="$(login student1 student123)"
pass '管理员和学生演示账号均可登录'

ME_JSON="$(api_get "$STUDENT_TOKEN" '/auth/me')"
test "$(jq -r '.data.role' <<<"$ME_JSON")" = 'STUDENT' \
  || fail '学生身份信息不正确'
pass 'JWT 身份信息可读取'

EVENTS_JSON="$(api_get "$STUDENT_TOKEN" '/events')"
test "$(jq -r '.data | length' <<<"$EVENTS_JSON")" -ge 1 \
  || fail '活动广场没有可读取数据'
pass '学生活动广场接口返回数据'

MY_REGISTRATIONS_JSON="$(api_get "$STUDENT_TOKEN" '/me/registrations')"
test "$(jq -r '.data | length' <<<"$MY_REGISTRATIONS_JSON")" -ge 1 \
  || fail '学生票夹没有演示数据'
pass '学生票夹接口返回数据'

ADMIN_EVENTS_JSON="$(api_get "$ADMIN_TOKEN" '/admin/events')"
test "$(jq -r '.data | length' <<<"$ADMIN_EVENTS_JSON")" -ge 1 \
  || fail '管理端活动列表没有数据'
pass '管理员活动列表接口返回数据'

ADMIN_DETAIL_JSON="$(api_get "$ADMIN_TOKEN" '/admin/events/5/detail')"
test "$(jq -r '.data.id' <<<"$ADMIN_DETAIL_JSON")" = '5' \
  || fail '活动详情数据不正确'
pass '管理员活动详情接口返回数据'

ADMIN_REGISTRATIONS_JSON="$(api_get "$ADMIN_TOKEN" '/admin/events/5/registrations')"
test "$(jq -r '.data | length' <<<"$ADMIN_REGISTRATIONS_JSON")" -ge 1 \
  || fail '报名记录为空'
pass '管理员报名记录接口返回数据'

STUDENT_ADMIN_STATUS="$(
  curl -sS -o /dev/null -w '%{http_code}' \
    -H "Authorization: Bearer ${STUDENT_TOKEN}" \
    "${BASE_URL}/admin/events"
)"
test "$STUDENT_ADMIN_STATUS" = '403' \
  || fail "学生访问管理接口应返回 403，实际为 ${STUDENT_ADMIN_STATUS}"
pass '学生访问管理端接口被拒绝'

EXPORT_FILE="${TMP_DIR}/registrations.xlsx"
curl -fsS \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${BASE_URL}/admin/events/5/export" \
  -o "$EXPORT_FILE"
test "$(head -c 2 "$EXPORT_FILE")" = 'PK' \
  || fail '导出的 Excel 文件格式不正确'
test "$(wc -c <"$EXPORT_FILE" | tr -d ' ')" -gt 1000 \
  || fail '导出的 Excel 文件为空'
pass 'Excel 报名名单可导出'

TOKEN_JSON="$(
  curl -fsS \
    -X POST \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${BASE_URL}/admin/events/5/check-in-token"
)"
CHECK_IN_TOKEN="$(jq -er '.data.token' <<<"$TOKEN_JSON")"
pass '管理员可生成短时签到 token'

CHECK_IN_JSON="$(
  curl -fsS \
    -X POST \
    -H "Authorization: Bearer ${STUDENT_TOKEN}" \
    "${BASE_URL}/check-in/${CHECK_IN_TOKEN}"
)"
test "$(jq -r '.data.status' <<<"$CHECK_IN_JSON")" = 'CHECKED_IN' \
  || fail '签到结果状态不正确'
pass '学生签到接口返回已签到状态'

REPEAT_CHECK_IN_JSON="$(
  curl -fsS \
    -X POST \
    -H "Authorization: Bearer ${STUDENT_TOKEN}" \
    "${BASE_URL}/check-in/${CHECK_IN_TOKEN}"
)"
test "$(jq -r '.data.checkedInAt' <<<"$REPEAT_CHECK_IN_JSON")" = \
  "$(jq -r '.data.checkedInAt' <<<"$CHECK_IN_JSON")" \
  || fail '重复签到不应产生新的签到时间'
pass '重复签到保持幂等'

printf '\nAll smoke checks passed.\n'
