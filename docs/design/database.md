# 数据库设计

## 1. 设计目标

本项目只需要保证校园活动场景下的三类一致性：

1. 活动占位名额不能超过容量，不能超卖。
2. 同一个学生对同一个活动最多只有一条报名记录。
3. 取消报名、释放名额和候补递补必须在同一个事务中完成。

MVP 使用单 MySQL 数据库，不引入 Redis、消息队列或分布式事务。

## 2. 表结构

### 2.1 `users`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `username` | `VARCHAR(50)` | 登录名，唯一 |
| `password_hash` | `VARCHAR(100)` | BCrypt 哈希 |
| `display_name` | `VARCHAR(50)` | 展示名称 |
| `role` | `VARCHAR(20)` | `STUDENT` 或 `ADMIN` |

关键约束：

- `uk_users_username`：用户名唯一。
- `chk_users_role`：角色只能是学生或管理员。

### 2.2 `events`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `title` | `VARCHAR(100)` | 活动标题 |
| `description` | `VARCHAR(2000)` | 活动描述 |
| `location` | `VARCHAR(100)` | 活动地点 |
| `category` | `VARCHAR(30)` | 活动分类 |
| `organizer` | `VARCHAR(100)` | 主办方 |
| `start_time` | `DATETIME(3)` | 活动开始时间 |
| `end_time` | `DATETIME(3)` | 活动结束时间 |
| `registration_start` | `DATETIME(3)` | 报名开始时间 |
| `registration_end` | `DATETIME(3)` | 报名截止时间 |
| `capacity` | `INT` | 活动名额 |
| `occupied_count` | `INT` | 当前占位数量 |
| `status` | `VARCHAR(20)` | 活动状态 |
| `created_by` | `BIGINT` | 创建管理员 |

关键约束：

- `occupied_count >= 0 AND occupied_count <= capacity`。
- `capacity > 0`。
- 活动状态只能是 `DRAFT / PUBLISHED / CANCELED / FINISHED`。
- `idx_events_status_start` 支持按状态和时间查询活动。

`occupied_count` 统计的是：

- `CONFIRMED`
- `PROMOTED`
- `CHECKED_IN`

`WAITLISTED` 不占座位，因此不增加 `occupied_count`。

### 2.3 `registrations`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `event_id` | `BIGINT` | 活动 ID |
| `user_id` | `BIGINT` | 学生 ID |
| `status` | `VARCHAR(20)` | 报名状态 |
| `waitlist_time` | `DATETIME(3)` | 进入候补队列的时间 |
| `confirm_deadline` | `DATETIME(3)` | 递补确认截止时间 |
| `checked_in_at` | `DATETIME(3)` | 签到时间 |
| `canceled_at` | `DATETIME(3)` | 取消时间 |

关键索引：

- `uk_registrations_event_user (event_id, user_id)`：防止同一用户重复创建报名记录。
- `idx_registrations_event_status_waitlist (event_id, status, waitlist_time)`：按候补顺序取队首。
- `idx_registrations_promotion_deadline (status, confirm_deadline)`：扫描过期的待确认递补。
- `idx_registrations_user_status (user_id, status)`：查询个人报名记录。

## 3. 状态机

### 3.1 报名状态

```text
CONFIRMED ──取消──> CANCELED
CONFIRMED ──签到──> CHECKED_IN

WAITLISTED ──有座位释放──> PROMOTED
PROMOTED ──确认──> CONFIRMED
PROMOTED ──超时──> EXPIRED
PROMOTED ──放弃──> CANCELED

CANCELED / EXPIRED ──重新报名──> CONFIRMED 或 WAITLISTED
```

`PROMOTED` 仍然占用一个名额。它表示系统已经把这个座位保留给该学生，等待学生在 10 分钟内确认。

### 3.2 活动状态

```text
DRAFT ──发布──> PUBLISHED
PUBLISHED ──结束──> FINISHED
PUBLISHED ──取消──> CANCELED
```

活动被取消时，所有 `CONFIRMED / WAITLISTED / PROMOTED / CHECKED_IN` 报名统一转为 `CANCELED`，`occupied_count` 归零。

活动被结束时只改变活动状态，不把报名自动改成签到或取消。报名历史和签到记录保留，但接口通过 `eventStatus` 和 `canCancel/canCheckIn` 标记禁止后续操作。

## 4. 名额控制

报名事务的核心顺序：

```text
开始事务
  SELECT events ... FOR UPDATE
  校验活动状态和报名窗口
  SELECT registrations ... FOR UPDATE
  如果有座位：
    INSERT 或更新报名为 CONFIRMED
    UPDATE events SET occupied_count = occupied_count + 1
  如果无座位：
    INSERT 或更新报名为 WAITLISTED
提交事务
```

关键点：

- 活动行是名额竞争的统一锁入口。
- 同一活动的报名请求因此串行执行，避免 `occupied_count` 超卖。
- `(event_id, user_id)` 唯一索引作为重复报名的数据库兜底。
- 名额检查不能只依赖前端显示的剩余数量。

## 5. 取消与递补事务

```text
开始事务
  SELECT events ... FOR UPDATE
  SELECT registrations ... FOR UPDATE
  将当前报名标记为 CANCELED
  如果当前报名占座：
    查找 waitlist_time 最早、id 最小的 WAITLISTED 报名
    如果存在：标记为 PROMOTED，写入 confirm_deadline
    如果不存在：occupied_count - 1
提交事务
```

被递补者的占位数量没有增加，因为取消者释放的座位已经在同一个事务中转移给了递补者。

## 6. 超时扫描

`WaitlistExpiryJob` 每 30 秒扫描一次 `PROMOTED` 且 `confirm_deadline <= now()` 的记录。

每条记录都会：

1. 锁活动行。
2. 重新锁报名行并再次检查状态和截止时间。
3. 标记为 `EXPIRED`。
4. 尝试递补下一位。

确认接口也会在锁内再次检查截止时间，因此“用户刚好在最后一秒确认”和“定时任务刚好扫描到”不会同时成功。

## 7. 已知局限

- 单活动报名会串行化，热点活动吞吐受 MySQL 行锁影响。
- `occupied_count` 是冗余计数，所有名额变更必须继续遵守同一事务边界。
- 结束活动不会自动生成“缺席”记录。
- 没有审计日志、站内消息或短信提醒。
- 没有分库分表、缓存和消息队列，目标规模是校园单体应用。
