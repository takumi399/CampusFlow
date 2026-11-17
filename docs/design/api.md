# API 设计

## 1. 基本约定

- 后端基地址：`http://127.0.0.1:8080`
- 前端通过 Vite 代理访问 `/api`
- 除登录接口外，其余接口需要：

```http
Authorization: Bearer <JWT>
```

统一成功响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "操作成功",
  "data": {}
}
```

统一失败响应：

```json
{
  "success": false,
  "code": "REGISTRATION_CLOSED",
  "message": "活动当前不可报名"
}
```

## 2. 认证

### `POST /api/auth/login`

请求：

```json
{
  "username": "student1",
  "password": "student123"
}
```

响应 `data`：

```json
{
  "token": "<jwt>",
  "expiresInSeconds": 7200,
  "user": {
    "id": 2,
    "username": "student1",
    "displayName": "林晓",
    "role": "STUDENT"
  }
}
```

### `GET /api/auth/me`

返回当前登录用户。前端刷新页面后用它恢复登录态。

## 3. 学生端接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/events` | 已发布活动列表 |
| `GET` | `/api/events/{eventId}` | 活动详情和当前用户报名状态 |
| `POST` | `/api/events/{eventId}/registrations` | 报名或加入候补 |
| `DELETE` | `/api/registrations/{registrationId}` | 取消报名、候补或递补 |
| `POST` | `/api/registrations/{registrationId}/confirm` | 确认递补 |
| `GET` | `/api/me/registrations` | 我的报名记录 |
| `POST` | `/api/check-in/{token}` | 使用签到 token 签到 |

### 报名返回示例

```json
{
  "id": 9,
  "eventId": 5,
  "eventTitle": "校园摄影分享会",
  "eventStatus": "PUBLISHED",
  "eventStartTime": "2026-11-13T19:00:00",
  "eventEndTime": "2026-11-13T20:30:00",
  "eventLocation": "图书馆报告厅",
  "status": "WAITLISTED",
  "waitlistTime": "2026-11-08T03:46:00",
  "waitlistPosition": 1,
  "canCancel": true,
  "canConfirm": false,
  "canCheckIn": false
}
```

当名额已满时，接口返回 `WAITLISTED`；当存在空位时返回 `CONFIRMED`。重复提交同一活动报名时，如果当前报名仍有效，接口返回已有记录，不重复占座。

## 4. 管理员接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/admin/events` | 管理端活动汇总列表 |
| `GET` | `/api/admin/events/{eventId}` | 管理端活动摘要 |
| `GET` | `/api/admin/events/{eventId}/detail` | 管理端活动完整编辑数据 |
| `POST` | `/api/admin/events` | 创建活动草稿 |
| `PUT` | `/api/admin/events/{eventId}` | 修改草稿 |
| `POST` | `/api/admin/events/{eventId}/publish` | 发布活动 |
| `POST` | `/api/admin/events/{eventId}/cancel` | 取消活动并取消有效报名 |
| `POST` | `/api/admin/events/{eventId}/finish` | 结束活动 |
| `GET` | `/api/admin/events/{eventId}/registrations` | 报名名单 |
| `POST` | `/api/admin/registrations/{registrationId}/check-in` | 手动签到 |
| `GET` | `/api/admin/events/{eventId}/export` | 导出 Excel |
| `POST` | `/api/admin/events/{eventId}/check-in-token` | 生成 5 分钟签到 token |

### 创建/修改活动请求

```json
{
  "title": "秋日校园音乐节",
  "description": "校园乐队与社团联合演出",
  "location": "东区草坪",
  "category": "文艺",
  "organizer": "校学生会",
  "startTime": "2026-11-15T19:00:00",
  "endTime": "2026-11-15T21:30:00",
  "registrationStart": "2026-11-08T09:00:00",
  "registrationEnd": "2026-11-13T23:59:00",
  "capacity": 200
}
```

业务校验：

- 报名开始时间早于报名截止时间。
- 报名截止时间不晚于活动开始时间。
- 活动开始时间早于活动结束时间。
- 发布时，报名截止时间和活动开始时间都必须晚于当前时间。

### 签到 token 返回

```json
{
  "token": "<signed-token>",
  "expiresAt": "2026-11-08T04:08:00"
}
```

前端把 token 放进签到链接：

```text
${window.location.origin}/check-in?token=<url-encoded-token>
```

签到接口会同时校验：

1. token 签名和类型。
2. token 是否过期。
3. 活动是否存在且处于 `PUBLISHED`。
4. 当前用户是否有该活动的报名记录。
5. 报名是否为 `CONFIRMED` 或已经 `CHECKED_IN`。

## 5. 错误码

常见错误码：

| 错误码 | 含义 |
|---|---|
| `VALIDATION_ERROR` | 参数校验失败 |
| `UNAUTHORIZED` | 未登录或 JWT 无效 |
| `FORBIDDEN` | 角色或资源权限不足 |
| `EVENT_NOT_FOUND` | 活动不存在 |
| `EVENT_NOT_DRAFT` | 活动不是草稿，不能编辑 |
| `EVENT_NOT_PUBLISHED` | 活动不是已发布状态 |
| `REGISTRATION_CLOSED` | 当前不在报名窗口 |
| `NOT_PROMOTED` | 报名不在待确认状态 |
| `PROMOTION_EXPIRED` | 递补确认已过期 |
| `INVALID_CHECK_IN_TOKEN` | 签到 token 无效或过期 |
| `NOT_CONFIRMED` | 报名未确认，不能签到 |

## 6. 前端调用边界

- `src/api/http.js` 统一注入 Bearer token 并处理 401。
- `src/api/*.js` 只负责接口协议，不包含页面状态。
- 页面通过后端返回的 `canCancel / canConfirm / canCheckIn` 决定按钮是否可用，不自行猜测权限。
- 扩展页面或调整视觉时，应保持接口字段和上述业务语义不变。
