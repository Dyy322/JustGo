# 认证模块（Auth Module）说明文档

## 概述

认证模块负责用户注册、登录、令牌刷新、登出等身份认证相关功能。采用自研 JWT（HMAC-SHA256）双令牌机制，结合 Redis 黑名单实现令牌吊销。

## 涉及文件

```
JustGo-backend/src/main/java/com/dyu/justgobackend/
├── controller/AuthController.java          # REST 接口层
├── service/AuthService.java                # 服务接口
├── service/impl/AuthServiceImpl.java       # 服务实现
├── security/
│   ├── JwtTokenProvider.java               # JWT 令牌生成/解析（自研）
│   ├── JwtAuthenticationInterceptor.java   # 请求拦截器，校验访问令牌
│   ├── JwtDenylistService.java             # Redis 令牌黑名单
│   ├── JwtProperties.java                  # JWT 配置属性
│   ├── UserContext.java                    # ThreadLocal 用户上下文
│   ├── LoginUser.java                      # 当前用户记录
│   └── ParsedToken.java                    # 解析后令牌记录
├── config/
│   ├── SecurityBeansConfig.java            # BCrypt 密码编码器
│   └── WebMvcConfig.java                   # 拦截器注册 + CORS
├── entity/User.java                        # 用户实体
├── mapper/UserMapper.java                  # 用户 Mapper
├── dto/request/
│   ├── LoginRequest.java                   # 登录请求
│   ├── RegisterRequest.java                # 注册请求
│   ├── RefreshTokenRequest.java            # 刷新令牌请求
│   └── ChangePasswordRequest.java          # 修改密码请求
├── dto/response/LoginResponse.java         # 登录/刷新响应
├── enums/TokenKind.java                    # 令牌类型枚举
├── common/ApiResponse.java                 # 统一响应格式
├── exception/
│   ├── BusinessException.java              # 业务异常
│   └── GlobalExceptionHandler.java         # 全局异常处理
└── util/
    ├── AuthorizationHeaderUtils.java       # Bearer 令牌提取
    └── HttpServletResponseUtils.java       # 错误响应写入
```

## API 端点

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| POST | `/api/auth/register` | 否 | 用户注册 |
| POST | `/api/auth/login` | 否 | 用户登录，返回双令牌+用户信息 |
| POST | `/api/auth/refresh` | 否（使用刷新令牌） | 令牌轮换，旧刷新令牌进入黑名单 |
| GET | `/api/auth/me` | 是 | 获取当前用户完整信息 |
| POST | `/api/auth/logout` | 是 | 登出，访问令牌进入黑名单 |

## 实现细节

### 1. 密码安全

- **算法**：BCrypt（Spring Security Crypto），强度因子 10（2^10 轮哈希）
- **加盐**：BCrypt 自动内置随机盐，每次哈希结果不同
- **存储**：数据库中仅存储 BCrypt 哈希值，不可逆

**未使用 pepper**（服务端额外密钥），这是进一步提升安全性的方向。

### 2. JWT 令牌机制（自研实现）

**核心要点：本模块未使用任何 JWT 标准库（如 jjwt、nimbus-jose-jwt），而是自行实现了完整的 JWT 生成与解析逻辑。**

#### 令牌结构

两部分组成：**访问令牌（Access Token）** + **刷新令牌（Refresh Token）**。

```
访问令牌：
- 算法：HMAC-SHA256 (HS256)
- 有效期：2 小时（可配置）
- 用途：携带用户身份访问 API

刷新令牌：
- 算法：HMAC-SHA256 (HS256)
- 有效期：7 天（可配置）
- 用途：在访问令牌过期后获取新的令牌对
```

#### 令牌载荷（Payload）

```json
{
  "iss": "JustGo-backend",
  "sub": "<userId>",
  "username": "<username>",
  "accountType": <int>,
  "jti": "<UUID>",
  "iat": <epoch_seconds>,
  "exp": <epoch_seconds>,
  "token_use": "access" | "refresh"
}
```

#### 签名验证

- 使用 `MessageDigest.isEqual()` 进行**常数时间**签名比对，防止时序攻击
- 签名密钥来自配置 `app.jwt.secret`，缺省值为 `JustGoDevelopmentSecretKeyChangeMeInProduction`（**生产环境必须通过环境变量覆盖**）

### 3. 登录流程

```
POST /api/auth/login { username, password }
  -> 查找用户（WHERE username = ? AND deleted_at IS NULL）
  -> 用户不存在？返回 401 "用户名或密码错误"（模糊提示，防用户枚举）
  -> 用户被禁用（status != 1）？返回 403
  -> 密码不匹配？累计 failed_login_attempts +1，返回 401
  -> 密码匹配？更新 lastLoginTime、lastLoginIp、loginCount+1、failedLoginAttempts 清零
  -> 生成 access_token + refresh_token
  -> 返回 LoginResponse { tokenType, accessToken, expiresIn, refreshToken, refreshExpiresIn, user }
```

### 4. 令牌刷新流程（令牌轮换）

```
POST /api/auth/refresh { refreshToken }
  -> 解析刷新令牌（验证签名、有效期、issuer、token_use="refresh"）
  -> 检查 jti 是否在黑名单中
  -> 查找用户，检查是否启用
  -> 将旧刷新令牌的 jti 加入黑名单（TTL = 令牌剩余有效期）
  -> 生成新的 access_token + refresh_token
  -> 返回新的 LoginResponse
```

**设计意图**：每次刷新都使旧刷新令牌失效（单次使用），如果刷新令牌被盗，当合法用户正常刷新后，攻击者的旧令牌将立即失效。攻击者若先于合法用户刷新，则合法用户下次刷新时会发现令牌已被加入黑名单。

### 5. 登出流程

```
POST /api/auth/logout（携带 Authorization: Bearer <accessToken>）
  -> 提取 Bearer 令牌
  -> 校验当前用户 == 令牌中用户（防止用别人的令牌登出）
  -> 将访问令牌 jti 加入黑名单（TTL = 令牌剩余有效期）
```

### 6. 请求拦截器

除白名单路径外，所有 `/api/**` 请求经过 `JwtAuthenticationInterceptor`：
1. 跳过 OPTIONS 请求（CORS 预检）
2. 提取 Authorization 头中的 Bearer 令牌
3. 无令牌 -> 返回 401
4. 解析访问令牌（验证签名、过期、issuer）
5. 检查令牌是否在黑名单中
6. 将用户信息写入 `UserContext`（ThreadLocal）
7. 请求结束后 `afterCompletion` 中清除 ThreadLocal

**白名单路径**：`/api/auth/login`、`/api/auth/register`、`/api/auth/refresh`、`/api/health`

### 7. 令牌黑名单（Redis）

- **Key 格式**：`jwt:denylist:<UUID>`（默认前缀可配置）
- **Value**：`"1"`
- **TTL**：设置为令牌剩余有效时间（`exp - now`），到期自动清除
- **风险**：Redis 重启后黑名单丢失，所有已"吊销"的令牌重新有效

---

## 已解决的问题

| 问题 | 方案 |
|---|---|
| 密码不可逆存储 | BCrypt 哈希，内置随机盐 |
| 时序攻击 | `MessageDigest.isEqual()` 常数时间签名比对 |
| 用户枚举 | 登录失败统一返回"用户名或密码错误" |
| 令牌泄漏 | 双令牌机制 + 刷新令牌单次使用 + 黑名单 |
| 跨请求用户信息传递 | ThreadLocal（`UserContext`） |
| 密码长度限制 | 前端后端均限制 6-72 字符，72 为 BCrypt 单次输入上限 |
| ThreadLocal 内存泄漏 | `afterCompletion` 保证清理 |

---

## 未解决的问题与安全隐患

### 严重（P0）

#### 1. 自研 JWT 实现——密码学风险（OWASP A02:2021）

当前 200 行自研 JWT 代码无法匹配成熟 JWT 库（如 jjwt、nimbus-jose-jwt）多年积累的安全补丁：
- 解析时不验证 Header 中的 `alg` 字段——虽然签名始终用 HMAC-SHA256 校验，但不符合 JWT 最佳实践
- 缺少对算法混淆攻击、密钥混淆攻击的防护
- 访问令牌的 `token_use` 校验过于宽松：只要不等于 `"refresh"` 就接受为访问令牌，包括 null、缺失、任意字符串

**建议**：替换为标准 JWT 库（推荐 `io.jsonwebtoken:jjwt` 或 `com.nimbusds:nimbus-jose-jwt`）。

#### 2. 无暴力破解防护（OWASP A07:2021）

- `failed_login_attempts` 递增了但**从未在登录时检查**
- `UserMapper.xml` 中有锁定 SQL 逻辑（`locked_until = DATE_ADD(NOW(), INTERVAL 30 MINUTE)`）但**从未被调用**——是死代码
- 无任何速率限制中间件

**建议**：
- 登录前检查 `failed_login_attempts >= 5` 且 `locked_until > NOW()`，拒绝登录
- 添加速率限制（如 Spring Cloud Gateway / Nginx `limit_req` / Bucket4j）

#### 3. 刷新令牌 TOCTOU 竞态条件

```
Thread-A: isDenied(refreshJti) -> false  ← 通过
Thread-B: isDenied(refreshJti) -> false  ← 也通过！
Thread-A: denyUntilExpiry(refreshJti)
Thread-B: denyUntilExpiry(refreshJti)    ← 两个都成功了
```

两个并发刷新请求使用同一刷新令牌可能同时成功，违反了"单次使用"的设计意图。

**建议**：使用 Redis `SET NX` 原子操作：`SET jti "1" NX EX ttl`，返回 null = 已在黑名单。

#### 4. 登出不吊销刷新令牌

只将访问令牌加入黑名单，刷新令牌仍然有效最多 7 天。如果攻击者持有刷新令牌，用户登出后攻击者仍可调用 `/api/auth/refresh`。

**建议**：登出时同时吊销访问令牌和刷新令牌（需要服务端持有刷新令牌或建立令牌关联）。

#### 5. 硬编码密钥

- JWT Secret 缺省值 `JustGoDevelopmentSecretKeyChangeMeInProduction` 在源码中明文，且在版本控制中
- MySQL 密码 `root/12345678` 硬编码在 `application.yaml`
- Redis 无密码认证

**建议**：所有密钥必须通过环境变量或密钥管理服务注入，生产环境无缺省值直接启动失败。

### 重要（P1）

#### 6. 无角色/权限控制（RBAC）

有认证（你是谁）无授权（你能做什么）。`accountType` 字段存在但从未用于任何访问控制决策。任何已认证用户可访问任何端点。

#### 7. Redis 故障无容错

Redis 不可用时，`JwtDenylistService.isDenied()` 抛出未捕获异常，所有认证请求返回 500。没有降级策略。

**建议**：
- 捕获 Redis 异常，降级为"允许通过"（fail-open）+ 告警
- 或者引入熔断器（Resilience4j CircuitBreaker）

#### 8. 注册端点泄漏用户名

注册返回"用户名已存在"，登录返回"用户名或密码错误"。攻击者可通过注册端点枚举已注册用户名。

#### 9. 弱密码策略

- 最短 6 位，无复杂度要求（无大小写/数字/特殊字符要求）
- 不检查常见弱密码列表

### 一般（P2）

#### 10. 密码哈希不可升级

BCrypt `matches()` 不会在成本因子提高后自动升级已存储的低成本哈希。老用户密码长期保持较弱哈希。

**建议**：使用 `BCryptPasswordEncoder.upgradeEncoding()` 模式，登录成功时检查并在需要时重新哈希。

#### 11. 账号状态字段不一致

- `User.java` 有 `lockedUntil` 逻辑但字段不存在于实体
- XML Mapper 引用 `salt`、`birthday`、`remark` 等列但实体中没有
- 多个 XML 方法（`selectByUsername`、`updateLoginInfo`、`incrementFailedAttempts`）从未被调用——死代码

#### 12. Bearer 前缀大小写

RFC 7235 规定认证方案名大小写不敏感，但当前只接受精确的 `"Bearer "`（大写 B），不接受 `"bearer "`。

#### 13. 无邮箱验证

注册接受邮箱但从不验证。用户可用任意邮箱（包括他人邮箱）注册。

---

## 大厂标准对比

| 维度 | 现状 | 大厂标准 | 差距 |
|---|---|---|---|
| JWT 实现 | 自研 | 成熟库（jjwt/nimbus） | 高 |
| 暴力破解防护 | 无 | 账号锁定 + 速率限制 + CAPTCHA | 高 |
| 密码策略 | 6位，无复杂度 | 8+位，大小写+数字+符号，检查弱密码库 | 中 |
| 密钥管理 | 配置文件明文 | 密钥管理服务（Vault/KMS）+ 定期轮换 | 高 |
| 令牌吊销 | 仅吊销访问令牌 | 双令牌同时吊销 + 设备指纹关联 | 中 |
| 多因素认证 | 无 | 支持 MFA（短信/邮箱/TOTP） | 高 |
| Redis 容错 | 无降级 | 熔断 + fail-open + 多副本 | 中 |
| 审计日志 | 无 | 每次登录/登出/令牌刷新完整审计 | 中 |
| 并发安全 | TOCTOU 竞态 | 原子操作（Redis SETNX/Lua） | 中 |
| RBAC | 无 | 细粒度角色权限控制 | 高 |
| 响应头安全 | 无 | CSP, HSTS, X-Content-Type-Options 等 | 低（Nginx/网关层可补救） |

---

## 高可用 & 高并发分析

### 当前瓶颈

| 组件 | 问题 | 影响 |
|---|---|---|
| MySQL（单节点） | 登录写操作依赖单主 | 并发登录 QPS 受限于 DB 写入能力 |
| Redis（单节点） | 黑名单查询是认证链路的强依赖 | Redis 故障 -> 全站 500 |
| 无状态服务 | Spring Boot 本身无状态，可水平扩展 | 好，但受限于以上两个依赖 |

### 规模化方案

1. **MySQL**：读写分离（登录写主、信息查从），或切换到分布式数据库（TiDB）
2. **Redis**：Redis Sentinel（主从自动切换）或 Redis Cluster（分片）
3. **熔断降级**：Redis 不可用时 fail-open（允许请求通过），配合告警
4. **令牌优化**：考虑无状态令牌吊销方案（如缩短令牌有效期 + 不提供吊销），降低对 Redis 的强依赖
5. **网关层**：将速率限制和基础安全头放到 Nginx / API Gateway

---

## 数据库

### user 表（关键字段）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 自增主键 |
| username | VARCHAR(50) | 用户名，唯一索引 |
| password | VARCHAR(255) | BCrypt 哈希 |
| email, phone | VARCHAR | 可空 |
| status | TINYINT | 1=正常，0=禁用 |
| account_type | TINYINT | 账户类型 |
| failed_login_attempts | INT | 登录失败次数（递增但未使用） |
| locked_until | DATETIME | 锁定截止时间（写入逻辑在死代码中） |
| last_login_time, last_login_ip | - | 登录追踪 |
| deleted_at | DATETIME | 软删除（MyBatis-Plus 自动过滤） |
