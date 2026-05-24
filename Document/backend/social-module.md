# 关注/粉丝模块（Social Module）说明文档

## 概述

关注/粉丝模块实现用户之间的社交关系管理。包含关注/取关（幂等）、粉丝列表/关注列表（游标分页）、关注统计（Redis 缓存 + MySQL 冗余表）、双向关注关系查询等功能。采用冗余统计表 + 原子更新的方式避免实时 COUNT 查询。

## 涉及文件

```
JustGo-backend/src/main/java/com/dyu/justgobackend/
├── controller/UserController.java              # 关注相关 REST 接口
├── service/UserService.java                    # 服务接口
├── service/impl/UserServiceImpl.java           # 服务实现（关注/取关/统计/列表/关系）
├── service/social/
│   ├── FollowCursorCodec.java                  # 游标编码/解码（Base64URL + JSON）
│   └── FollowStatsRedisCache.java              # 关注统计 Redis 读写
├── entity/
│   ├── UserFollow.java                         # 关注关系实体
│   └── UserFollowStats.java                    # 关注统计冗余表实体
├── mapper/
│   ├── UserFollowMapper.java                   # 关注关系 Mapper
│   ├── UserFollowMapper.xml                    # 关注关系 SQL（INSERT IGNORE / DELETE / 游标分页 / 存在性检查）
│   ├── UserFollowStatsMapper.java              # 统计 Mapper
│   └── UserFollowStatsMapper.xml               # 统计 SQL（原子增减 / ensureRow）
├── dto/
│   ├── internal/FollowListRow.java             # MyBatis 结果映射 DTO
│   └── response/
│       ├── FollowCursorPageResponse.java       # 游标分页通用响应
│       ├── FollowUserItemResponse.java         # 关注列表单项
│       ├── UserFollowRelationResponse.java     # 双向关注关系
│       └── UserFollowStatsResponse.java        # 关注统计
└── resources/SQL/user_follow.sql               # DDL（含设计注释）
```

## API 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/users/{id}/follow` | 关注用户（幂等） |
| DELETE | `/api/users/{id}/follow` | 取关用户（幂等） |
| GET | `/api/users/{id}/follow-stats` | 获取关注数/粉丝数 |
| GET | `/api/users/{id}/follow-relation` | 查询当前用户与目标用户的双向关注关系 |
| GET | `/api/users/{id}/followers` | 粉丝列表（游标分页） |
| GET | `/api/users/{id}/following` | 关注列表（游标分页） |

## 实现细节

### 1. 关注流程（幂等设计）

```
POST /api/users/{id}/follow
  -> 获取当前用户 ID（UserContext）
  -> 自己关注自己？-> 400 "不能关注自己"
  -> 验证目标用户存在且启用（status=1, deleted_at IS NULL）
  -> INSERT IGNORE INTO user_follow (follower_id, followee_id) VALUES (?, ?)
  -> affected_rows == 0？-> 已关注，直接返回（幂等）
  -> 更新统计：
       user_follow_stats.following_count += 1（当前用户）
       user_follow_stats.follower_count += 1（目标用户）
  -> 清除双方 Redis 缓存
```

**幂等性保证**：完全依赖数据库唯一约束 `uk_follower_followee (follower_id, followee_id)`。`INSERT IGNORE` 在冲突时静默跳过，返回 0 行受影响。不需要应用层分布式锁。

**原子性保证**：整个方法在 `@Transactional` 中，关系插入和统计更新要么一起提交要么一起回滚。

### 2. 取关流程

```
DELETE /api/users/{id}/follow
  -> 当前用户 == 目标用户？-> 静默返回（行为不对称——关注自己抛 400）
  -> DELETE FROM user_follow WHERE follower_id=? AND followee_id=?
  -> deleted == 0？-> 本来就没关注，直接返回（幂等）
  -> 更新统计：
       following_count -= 1（IF(count > 0, count - 1, 0) 防下溢）
       follower_count -= 1（IF(count > 0, count - 1, 0) 防下溢）
  -> 清除双方 Redis 缓存
```

### 3. 关注统计（读穿缓存 Read-Through Cache）

```
GET /api/users/{id}/follow-stats
  -> 读取 Redis：jg:social:stats:{userId}（Hash: following, followers）
  -> 命中？直接返回
  -> 未命中：
       ensureRow(userId)  -- INSERT IGNORE INTO user_follow_stats VALUES (userId, 0, 0)
       SELECT * FROM user_follow_stats WHERE user_id = userId
       写入 Redis（TTL 10 分钟）
       返回结果
```

**缓存失效策略**：关注/取关时主动删除（`evictPair`），下次读取时重新加载。

**写入操作**：
- `incrementFollowing/incrementFollowers`：使用 `ON DUPLICATE KEY UPDATE` ——如果行不存在则创建并设值为 1，存在则原子 +1
- `decrementFollowing/decrementFollowers`：使用 `IF(count > 0, count - 1, 0)` 防止无符号整数下溢

### 4. 游标分页（Cursor-based Pagination）

不使用 OFFSET，采用复合游标 `(created_at, user_id)` 的游标分页。

#### 游标编码

```
游标 = Base64URL( JSON({"t": epoch_millis, "u": tie_breaker_user_id}) )
```

示例：`eyJ0IjoxNzE2MDAwMDAwMDAwLCJ1IjoxMjN9` 解码为 `{"t":1716000000000,"u":123}`

#### 查询逻辑（粉丝列表为例）

```sql
SELECT u.id, u.username, u.nickname, u.avatar, u.gender, f.created_at AS followedAt
FROM user_follow f
INNER JOIN `user` u ON u.id = f.follower_id AND u.deleted_at IS NULL AND u.status = 1
WHERE f.followee_id = ?
  AND (
    f.created_at < ?                           -- 游标时间之后
    OR (f.created_at = ? AND f.follower_id < ?) -- 同时间的 tie-breaker
  )
ORDER BY f.created_at DESC, f.follower_id DESC
LIMIT ? + 1   -- 多取 1 条判断是否有下一页
```

#### 分页流程

1. 客户端传入 `cursor`（首页不传）和 `limit`（1-50，默认 20）
2. 解码游标 -> `(createdAt, tieBreakerUserId)`
3. 查询 `limit + 1` 行
4. 结果数 > limit -> `hasMore = true`，取前 limit 行为当前页，最后一行的位置编码为 `nextCursor`
5. 返回 `{ items, nextCursor, hasMore }`

#### 为什么用游标分页而非 OFFSET

| | OFFSET | 游标 |
|---|---|---|
| 大偏移量性能 | O(n) 扫描前 n 行 | O(log n) 索引定位 |
| 并发写入 | 数据漂移，重复/丢失 | 锚点稳定，不受新数据影响 |
| 亿级数据 | 无法使用 | 索引 seek 高效 |
| 实现复杂度 | 简单 | 需要编码/解码 + 复合排序 |

### 5. 双向关注关系查询

```
GET /api/users/{id}/follow-relation
  -> 自己查自己？-> { following: false, followsYou: false }
  -> 两次 SELECT COUNT(1) > 0：
       A 关注 B？（viewerId -> targetUserId）
       B 关注 A？（targetUserId -> viewerId）
  -> 返回 { following, followsYou }
```

### 6. 数据库索引设计

```sql
-- user_follow 表
UNIQUE KEY uk_follower_followee (follower_id, followee_id)
  -- 用途：保证唯一性 + existsEdge 快速查找

KEY idx_followee_created_follower (followee_id, created_at DESC, follower_id DESC)
  -- 用途：粉丝列表游标分页（覆盖 WHERE + ORDER BY，无 filesort）

KEY idx_follower_created_followee (follower_id, created_at DESC, followee_id DESC)
  -- 用途：关注列表游标分页（覆盖 WHERE + ORDER BY，无 filesort）

-- user_follow_stats 表
PRIMARY KEY (user_id)
  -- 用途：单行统计查询/更新
```

所有查询均命中覆盖索引，无需回表或 filesort。

---

## 已解决的问题

| 问题 | 方案 |
|---|---|
| 关注/取关重复调用 | `INSERT IGNORE` + 唯一约束（数据库级幂等） |
| 大偏移量分页 | 游标分页 + 复合索引 |
| 粉丝数实时 COUNT 开销大 | 冗余统计表 + 原子更新 |
| 热点用户统计查询 | Redis 读穿缓存（10 分钟 TTL） |
| 并发关注同一用户 | 唯一约束保证只有一个成功 |
| 关注已禁用/已删除用户 | JOIN 中过滤 `status=1 AND deleted_at IS NULL` |
| 统计计数下溢 | `IF(count > 0, count - 1, 0)` 防御 |
| ThreadLocal 泄漏 | 拦截器 `afterCompletion` 保证清理 |

---

## 未解决的问题与隐患

### 严重（P0）

#### 1. 缓存击穿（Cache Stampede）

热点用户（如名人）的统计缓存过期时，1000 个并发请求同时穿透 Redis，全部打到 MySQL 的同一行。

**场景**：某大 V 的 `follow-stats` 缓存过期 -> 瞬间 1000 个请求 -> 1000 次 `ensureRow` + 1000 次 `selecByUserId`

**建议**：使用 Redis `SETNX` 实现缓存加载锁（互斥锁），仅一个线程加载数据库，其余等待。

```
if cache miss:
    lock = SETNX("lock:stats:{userId}", "1", 5s)
    if lock:
        load from DB, put to Redis, del lock
    else:
        sleep(100ms), retry get from Redis
```

#### 2. 统计计数漂移，无自动修复

统计表 `user_follow_stats` 的计数可能因以下原因与实际边表不一致：
- 事务回滚的极端边界情况
- 数据库运维操作（部分恢复、手动删边等）
- 未来可能引入的消息队列异步处理导致丢失

一旦漂移，没有自愈机制——DDL 注释中提到了"离线对账"但尚未实现。

**建议**：实现定时对账任务（如每日凌晨），`SELECT COUNT(*) FROM user_follow WHERE ...` 与统计表比对并修正。

#### 3. 游标编码使用时区敏感的时间转换

```java
long millis = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
```

`ZoneId.systemDefault()` 取决于部署环境。如果服务器从 `Asia/Shanghai` 迁移到 `UTC`，所有历史游标将失效（时间映射错误）。

**建议**：统一使用 UTC 存储/传输时间，MySQL `DATETIME` 改为 `TIMESTAMP` 或统一存储 epoch millis。

### 重要（P1）

#### 4. 无速率限制

关注/取关端点无线速限制。恶意用户可以：
- 快速关注/取关（骚扰通知，如果有通知功能）
- 大量请求消耗数据库写入能力

**建议**：添加速率限制（每人每分钟最多 N 次关注/取关）。

#### 5. 软删除用户仍计入统计

用户 A 关注用户 B -> B 被软删除 -> A 的 `following_count` 仍然包含 B，B 的 `follower_count` 也仍然包含 A。但在列表查询中 B 被 JOIN 过滤掉不显示。

**建议**：软删除用户时级联处理——删除其所有关注边 + 更新关联用户的统计（可通过异步事件触发）。

#### 6. 关注关系查询两次 SQL

`getFollowRelation` 执行两条 `SELECT COUNT(1) > 0` 查询。可优化为单次查询：

```sql
SELECT
  EXISTS(SELECT 1 FROM user_follow WHERE follower_id=A AND followee_id=B) AS following,
  EXISTS(SELECT 1 FROM user_follow WHERE follower_id=B AND followee_id=A) AS followsYou
```

### 一般（P2）

#### 7. 无关注通知

关注后不通知被关注者。社交应用通常有此功能（push/站内信）。

#### 8. 无批量操作接口

导入社交图谱（如从其他平台迁移）时缺少批量关注接口。

#### 9. ensureRow 延迟初始化可优化

每次缓存未命中都调用 `ensureRow`。更好的做法是在用户注册时创建统计行（初始为 0）。

#### 10. 游标无签名/防篡改

用户可解码、伪造游标。虽然不造成越权（查询仍限当前用户/目标用户的关注列表），但可能导致异常的分页结果。

---

## 大厂标准对比

| 维度 | 现状 | 大厂标准 | 差距 |
|---|---|---|---|
| 分页方案 | 游标分页 + 复合索引 | 同 | 无（方案正确） |
| 计数方案 | 冗余表 + 原子更新 | 冗余表 + 异步对账 + 最终一致性 | 中（缺对账） |
| 缓存策略 | 读穿 + 写入失效 | 读穿 + SETNX 防击穿 + 多级缓存 | 中（缺互斥锁） |
| 幂等性 | DB 唯一约束 | DB 唯一约束 + 请求去重 | 低 |
| 热点用户 | Redis 缓存，10 分钟 TTL | 本地缓存 + Redis + 熔断 | 中 |
| 事件驱动 | 同步更新 | 关注事件 -> MQ -> 异步处理 | 高（但当前规模同步足够） |
| 可观测性 | 无 | 统计漂移监控、慢查询告警、缓存命中率 | 高 |
| 通知系统 | 无 | 关注/取关触发通知 | 中 |
| 软删除级联 | 无 | 用户注销联动清理社交关系 | 中 |
| 时区处理 | `ZoneId.systemDefault()` | 全链路 UTC | 中 |

---

## 高可用 & 高并发分析

### 当前瓶颈

| 组件 | 问题 | 影响 |
|---|---|---|
| MySQL（单节点） | 关注/取关是写操作，受单主写入能力限制 | 写 QPS ~几千 |
| Redis（单节点） | 缓存不可用时退化到 MySQL 查询（可接受） | 延迟增加，但不会全站 500 |
| 统计表写热点 | 大 V 的 `user_follow_stats` 行频繁更新 | 行锁竞争 |
| 无读写分离 | 列表查询走主库 | 写操作受读操作影响 |

### 规模化方案

#### 百万级用户

当前架构可胜任。关注关系表按当前索引设计，百万用户 * 平均 100 关注 = 1 亿条边，游标分页索引查询仍在毫秒级。

#### 千万级用户 -> 亿级边

1. **MySQL 分库分表**：对 `user_follow` 按 `followee_id` 哈希分区（DDL 注释已预留方案）
   - 粉丝列表查询路由到对应分片
   - 关注列表查询需要额外索引或反向存储
2. **读写分离**：列表查询走只读副本，关注/取关走主库
3. **Redis Cluster**：统计缓存分片存储
4. **写操作异步化**（最终一致性可接受时）：
   - 关注事件 -> Kafka -> 消费者更新统计表 + 清理缓存
   - 降低同步写入的延迟和行锁竞争
5. **热点用户特殊处理**：
   - 大 V 的统计写入合并（如攒批，每秒聚合一次增减量再写 DB）
   - 列表查询加本地缓存（Caffeine），接受短暂延迟

#### 极限场景（亿级用户，名人千万粉丝）

1. 粉丝列表不再 JOIN `user` 表，用户信息旁路查询缓存
2. 统计表改为 Redis 计数器（Redis Hash hincrby），定期刷入 MySQL
3. 引入图数据库（如 NebulaGraph）处理复杂社交关系查询
4. 最终一致性 + 定期对账替代实时精确计数

---

## 数据库

### user_follow（关注关系表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT UNSIGNED | 自增主键 |
| follower_id | BIGINT UNSIGNED | 关注方用户 ID |
| followee_id | BIGINT UNSIGNED | 被关注方用户 ID |
| created_at | DATETIME(3) | 关注时间（毫秒精度） |

### user_follow_stats（关注统计冗余表）

| 字段 | 类型 | 说明 |
|---|---|---|
| user_id | BIGINT UNSIGNED | 主键 = 用户 ID |
| following_count | BIGINT UNSIGNED | 正在关注的人数 |
| follower_count | BIGINT UNSIGNED | 粉丝数 |
| updated_at | DATETIME(3) | 自动更新（ON UPDATE CURRENT_TIMESTAMP） |
