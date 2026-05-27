# CLAUDE.md — JustGo 后端

## 技术栈

Spring Boot 4 + Java 17 + Maven + MyBatis-Plus + MySQL 8 + Redis

## 架构大纲

```
controller/     REST 层 — 仅做参数校验和路由，不包含业务逻辑
service/        业务层 — 所有业务规则在此，按领域拆分子包
mapper/         持久层 — MyBatis-Plus 接口 + XML 映射文件
entity/         数据实体 — 与表结构一一对应
dto/            request/ 入参 + response/ 出参，与 entity 严格分离
common/         通用组件 — 统一响应包装、状态机引擎、上下文工具
exception/      业务异常 + 全局异常处理
config/         Spring 配置 — CORS/Jackson/MyBatis-Plus/拦截器
security/       认证鉴权 — JWT 提供器、拦截器、令牌黑名单
aspect/         切面 — 日志、脱敏等横切关注点
```

### 设计原则

- **分层解耦**：Controller → Service → Mapper，单向依赖，禁止跨层调用
- **DTO/Entity 分离**：Entity 不暴露到 Controller 层，接口契约通过 DTO 定义
- **统一响应**：所有接口返回 `ApiResponse<T>`（code=0 成功，非 0 失败）
- **异常即业务语言**：`BusinessException(code, message)` 抛出业务异常，`GlobalExceptionHandler` 统一转换为 HTTP 响应，**message 必须能直接展示给用户**（中文、具体、可操作）
- **文档先行**：实现新模块前先输出设计文档并确认方案，完成后再根据实际实现完善文档

## 开发规范

### 接口设计

- RESTful 风格，资源名用复数：`/api/users/{id}/follow`
- HTTP 方法语义化：GET 查询、POST 创建、PUT 全量更新、PATCH 部分更新、DELETE 删除
- 认证接口放在 `/api/auth/*`，业务接口放在 `/api/<资源>/*`
- 所有非认证接口需 JWT 认证，`/api/auth/*` 和 `/api/health` 在白名单
- 请求参数用 `@Valid` + Jakarta Validation 校验，Controller 类加 `@Validated`

### 异常处理规范

- **业务异常**：`throw new BusinessException(400, "不能关注自己")` — code 语义对齐 HTTP 状态码
- **全局兜底**：未捕获异常统一返回 500 + "服务器内部错误"，不泄露堆栈
- **验证异常**：`MethodArgumentNotValidException` → 400 + 字段级错误消息拼接
- catch 块不允许吞异常，必须记录日志并向上转换或返回明确错误

### 代码风格

- Java 17：`record` 替代 DTO 类、`switch` 表达式、`var`（类型明显时使用）
- Controller 方法语义化：`follow()` 而非 `doFollow()` 
- Service 接口 + Impl 模式，Impl 放在 `service/impl/` 下
- **新代码必须加注释**：类加 Javadoc 说明职责，公共方法加 Javadoc 说明入参/出参/异常，非显而易见的逻辑（如状态机 Guard 评估顺序、缓存失效策略）加行内注释解释 WHY

## 防御性编程原则

以下原则不是可选项，是每个 PR 的准入门槛：

### 幂等性

- **写操作必须可重放**：网络不可靠，客户端会重试。注册用唯一索引、关注用 `ON DUPLICATE KEY UPDATE`、支付用幂等键

### 超时与降级

- **所有外部调用必须设超时**：Redis、OSS、第三方 API，不设超时 = 线程耗尽 = 雪崩
- **有降级方案才叫高可用**：Redis 挂了 → 查 DB；OSS 挂了 → 返回占位图 + 异步重试；推荐服务挂了 → 返回热度排序兜底
- **快速失败 > 苦苦等待**：能立刻返回错误的绝不阻塞等待，释放连接给其他请求

### 资源管理

- **连接池有上限**：DB 连接池、Redis 连接池、HTTP 线程池都要配置 max-size 和 timeout
- **请求线程不阻塞**：发邮件、推通知、生成报表一律异步（`@Async` + 线程池 / MQ）
- **大事务拆小**：一个事务内只做必须原子完成的事，RPC 调用、文件 IO 一律放事务外

### 数据一致性

- **先写 DB，后失效缓存**（Cache-Aside 模式）：先更新 DB 再删缓存，而非先删缓存再更新 DB
- **软删除是常态**：用户数据用 `deleted_at` 标记，不物理删除，方便数据恢复和审计

## 高可用 & 高并发：决策框架

不同阶段面临不同瓶颈，关键是**知道当前阶段该做什么、不该做什么**：

### 选型决策树

```
你的 QPS 是多少？
  ├─ < 1000  → 单体 + 合理索引 + 缓存热点数据（当前阶段）
  ├─ 1000~1万 → 读写分离 +  Redis 集群 + 本地缓存
  ├─ 1万~10万 → 分库分表 + MQ 削峰 + 缓存预热
  └─ > 10万   → 单元化 + 最终一致性 + 多级缓存
```

### 各阶段核心矛盾

| 阶段 | 核心矛盾 | 优先投入 | 暂时不做 |
|---|---|---|---|
| MVP（当前） | 快速验证 vs 过度设计 | 领域建模、索引、缓存 | 分库分表、MQ、微服务 |
| 增长期 | 读压力 vs 写压力 | 读写分离、缓存预热、慢查询治理 | 单元化、多活 |
| 大规模 | 全局一致性 vs 可用性 | 分库分表、MQ 削峰、最终一致性 | — |

### 写高并发三板斧

1. **削峰填谷**：MQ 缓冲突发写入，消费者匀速处理
2. **合并写**：计数类操作先 Redis 聚合，定时刷 DB（有丢失风险，需评估业务容忍度）
3. **拆分写**：热点数据分片（如活动报名按 activity_id 分表），打散写入压力

### 读高并发三板斧

1. **缓存前置**：热点数据提前预热到 Redis / Caffeine，不让流量打到 DB
2. **读写分离**：写走主库，读走从库，从库可水平扩展
3. **降级兜底**：缓存 miss 时返回静态兜底数据，比空白页好一万倍

## 可观测性设计

日志、指标、链路，三位一体。

### 日志

- **统一框架**：使用 SLF4J，禁止 `System.out`；Controller 层由 `WebLayerLoggingAspect` 统一记录入参/耗时/出参
- **关键节点必打**：认证结果、DB 操作耗时、外部调用耗时、业务异常
- **携带上下文**：每条日志带 `userId` / `requestId`，多用户并发时可追踪
- **敏感脱敏**：密码、token、手机号、身份证号
- **分级合理**：DEBUG（本地）/ INFO（关键节点）/ WARN（可恢复异常）/ ERROR（需人工介入）

### 指标 & 告警（待建设）

- 技术栈：Spring Boot Actuator + Micrometer + Prometheus + Grafana
- 黄金信号：延迟（P50/P90/P99）、QPS、5xx 率、连接池饱和度 + 业务指标（注册/登录/关注数）
- 告警分级：P0 立即响应 → P1 30min 内 → P2 工作日处理，每条告警有对应 runbook

## 软件质量管理

### 测试策略（待落地）

关键路径必须有测试：Service 核心逻辑 → Mapper SQL + Redis 缓存 → Controller 异常分支（401/403/400/404）。

### 代码审查

**原则**：关注正确性和边界，不关注风格（交给 linter）。一次只解决一个问题，不夹带无关重构。

#### 强制检查项（一条不通过 = 拒绝合并）

- SQL 注入：MyBatis 参数是否使用 `#{}` 而非 `${}`
- 认证鉴权：新接口是否注册了拦截器白名单或正确要求认证
- 越权风险：涉及用户数据的查询是否校验了归属
- 敏感信息泄露：日志/响应中是否打印了密码、token
- 事务边界：写操作的事务是否包含了不该包含的远程调用

#### 重点关注项

- N+1 查询：循环内是否有数据库调用
- 缓存一致性：写 DB 和失效缓存的顺序是否正确（先写 DB 后删缓存）
- 并发安全：并发写是否有唯一索引保护、是否有竞态窗口
- 异常处理：catch 块是否吞了异常、错误消息是否对用户可读
- 大事务：事务内是否有 RPC 调用、文件 IO 等长耗时操作

### 安全红线

- **禁止** 硬编码凭证，密钥/密码必须通过环境变量注入（`${ENV_VAR:}` 或 Vault）
- JWT secret ≥ 32 字节，生产环境通过环境变量覆盖，禁止写在 `application.yaml`
- 涉及用户数据的查询必须校验归属，防止越权（SQL 注入等已由代码审查强制检查项覆盖）

## 数据库设计规范

- 主键使用自增 ID（`id-type: auto`）
- 时间字段统一 `created_at` / `updated_at`，MyBatis-Plus 自动填充
- 软删除使用 `deleted_at` 字段（`logic-delete-field: deletedAt`）
- 唯一约束在数据库层必须建索引（防止并发写入穿透应用层去重）
- 统计类字段（`following_count`）使用反范式设计，避免 COUNT 查询

## 配置管理

- 环境相关配置（DB 密码、Redis 地址、OSS 凭证）通过环境变量注入
- `application.yaml` 仅存放开发环境默认值，敏感值留空（`${VAR:}`）
- MyBatis-Plus 的 `mapper-locations` 变更需同步修改 `MyBatisPlusConfig`
