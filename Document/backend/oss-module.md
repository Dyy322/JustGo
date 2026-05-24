# 对象存储模块（OSS Module）说明文档

## 概述

基于阿里云 OSS 的文件存储模块，采用 **Pre-signed URL 直传**方案。客户端先从后端获取预签名的上传 URL，然后直接 PUT 文件到 OSS，文件流不经过后端服务器，节省带宽。适用于头像、活动图片等用户上传场景。

## 涉及文件

```
JustGo-backend/src/main/java/com/dyu/justgobackend/
├── oss/
│   ├── OssProperties.java          # OSS 配置 record（@ConfigurationProperties）
│   └── OssService.java             # Pre-signed URL 生成服务
├── controller/
│   └── FileController.java         # 上传凭证 REST 接口
├── dto/
│   ├── request/UploadTokenRequest.java     # 上传凭证请求 DTO
│   └── response/UploadTokenResponse.java   # 上传凭证响应 DTO
├── pom.xml                          # 添加 aliyun-sdk-oss 依赖
└── resources/application.yaml       # 添加 app.oss.* 配置
```

## API 端点

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| POST | `/api/files/upload-token` | 是（JWT） | 获取预签名上传 URL |

**请求体**：
```json
{
  "ext": "jpg",
  "prefix": "avatar"
}
```

**响应体**：
```json
{
  "code": 0,
  "data": {
    "uploadUrl": "https://bucket.oss-cn-shanghai.aliyuncs.com/justgo/avatar/uuid.jpg?Expires=...&OSSAccessKeyId=...&Signature=...",
    "fileUrl": "https://bucket.oss-cn-shanghai.aliyuncs.com/justgo/avatar/uuid.jpg",
    "objectKey": "justgo/avatar/uuid.jpg",
    "expireSeconds": 300
  }
}
```

## 实现细节

### 1. 上传流程（Pre-signed URL 直传）

```
客户端                    后端                      阿里云 OSS
  |                       |                           |
  |-- POST /api/files/upload-token -->               |
  |   { ext: "jpg", prefix: "avatar" }               |
  |                       |                           |
  |                       |-- 生成 objectKey           |
  |                       |-- 调用 OSS SDK 生成预签名URL |
  |                       |                           |
  |<-- { uploadUrl, fileUrl, objectKey } --            |
  |                       |                           |
  |-- PUT uploadUrl (二进制文件) ---------------------->|
  |                       |                           |
  |<-- 200 OK ---------------------------------------|
  |                       |                           |
  |-- PATCH /api/users/me { avatar: fileUrl } -->     |
  |                       |                           |
  |<-- 200 OK ------------|                           |
```

### 2. ObjectKey 设计

```
格式：{basePath}/{prefix}/{uuid}.{ext}
示例：justgo/avatar/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg
```

- `basePath`：统一前缀（默认 `justgo/`），便于 OSS 内按项目组织
- `prefix`：业务分类（如 `avatar`、`activity`、`post`），由客户端指定
- `uuid`：防碰撞、防路径遍历
- `ext`：文件扩展名（仅白名单内的类型）

### 3. OSS Client 生命周期

- `@PostConstruct init()`：启动时创建 `OSS` 客户端实例
- `@PreDestroy destroy()`：关闭时调用 `client.shutdown()` 释放连接
- 配置未就绪时（endpoint 为空）跳过初始化，调用时返回 503

### 4. 安全措施

| 措施 | 实现 |
|---|---|
| 文件类型白名单 | `ext` 字段仅允许 jpg/jpeg/png/gif/webp（DTO 层 `@Pattern` 校验 + 服务层常量） |
| prefix 限制 | `@Pattern(regexp = "^[a-z]+$")`，仅小写字母，防路径注入 |
| ObjectKey 防遍历 | UUID 作为文件名，用户无法猜测/枚举其他文件 |
| Pre-signed URL 有效期 | 默认 5 分钟（`url-expire-seconds: 300`） |
| 签名方法 | URL 中包含 OSS 签名参数，仅允许规定时间内的 PUT 操作 |
| 认证要求 | 上传凭证接口需 JWT 认证（`/api/files/**` 不在白名单中） |
| AccessKey 保护 | 仅通过环境变量注入，无硬编码默认值，启动时缺失则直接无法使用 |

### 5. 头像更新流程（示例）

前端代码示意：
```javascript
// 1. 获取上传凭证
const { data } = await api.post('/api/files/upload-token', { ext: 'jpg', prefix: 'avatar' })

// 2. 直传 OSS（PUT 二进制）
await fetch(data.uploadUrl, { method: 'PUT', body: fileBlob, headers: { 'Content-Type': 'image/jpeg' } })

// 3. 更新用户头像
await api.patch('/api/users/me', { avatar: data.fileUrl })
```

---

## 已解决的问题

| 问题 | 方案 |
|---|---|
| 后端带宽压力 | Pre-signed URL 直传，文件流不经过后端 |
| 文件命名冲突 | UUID 作为文件名 |
| 路径遍历攻击 | prefix 白名单校验（仅小写字母），文件名使用 UUID |
| 任意文件类型上传 | ext 白名单校验（仅 5 种图片格式） |
| 上传链接泄露滥用 | Pre-signed URL 5 分钟过期 + 仅允许 PUT 操作 |
| OSS 凭证安全 | AccessKey 仅通过环境变量注入，不写入代码或配置文件 |
| 配置未就绪时优雅降级 | client 为 null 时返回 503 而非 NPE |

---

## 未解决的问题与隐患

### 重要（P1）

#### 1. Bucket 权限模型待确认

当前 Pre-signed URL 仅授予 PUT 权限。读取访问分为两种模式：
- **公共读 Bucket**：`fileUrl` 可直接访问，适合头像等公开内容
- **私有 Bucket**：读取也需签名 URL，更安全但增加复杂度

**建议**：头像 bucket 设为公共读，用户上传内容（如活动图片）可以公共读。如需私有内容，后续增加签名读取接口。

#### 2. 文件大小无限制

OssService 未设置文件大小限制。虽然 Pre-signed URL 模式下文件不经后端，但恶意用户可以上传超大文件占用 OSS 空间。

**建议**：
- 后端在生成签名 URL 时，可通过 OSS Policy 文件限制内容长度
- 或在 `UploadTokenRequest` 中增加文件大小参数

#### 3. OSS 配置缺失时启动不报错

当前设计是 endpoint 为空时静默跳过 OSS Client 初始化，仅在使用时抛 503。这样 OSS 配置缺失不会阻止应用启动，但也意味着"部署成功但上传功能不可用"的情况可能被忽略。

**建议**：启动时打印日志明确提示 OSS 是否就绪。

### 一般（P2）

#### 4. 无上传完成确认

Pre-signed URL 模式无法得知客户端是否上传成功。客户端调用 PATCH `/api/users/me` 更新 avatar 时，后端不验证该 URL 是否真实存在有效文件。

**建议**：
- 客户端在 PATCH 前通过 HEAD 请求确认文件存在
- 或增加回调接口（`POST /api/files/upload-callback`）确认上传完成后再更新

#### 5. 无图片处理

头像列表/瀑布流通常需要缩略图，当前只有原图 URL。

**建议**：利用 OSS 图片处理能力，在 `UploadTokenResponse` 中额外返回缩略图 URL：
```
原图：fileUrl
缩略图：fileUrl + "?x-oss-process=image/resize,w_200,h_200"
```

#### 6. 无文件管理/清理

用户更换头像后旧文件残留在 OSS 中。

**建议**：后续增加定时清理任务，或通过 OSS 生命周期规则自动清理超过 N 天未访问的文件。

---

## 大厂标准对比

| 维度 | 现状 | 大厂标准 | 差距 |
|---|---|---|---|
| 上传方式 | Pre-signed URL 直传 | Pre-signed URL / STS Token | 符合标准 |
| 文件类型校验 | 后端 ext 白名单 | 后端 + OSS Policy | 低 |
| 图片处理 | 无 | 缩略图 + 格式转换 + CDN | 中 |
| 敏感信息保护 | AccessKey 环境变量 | RAM 角色（ECS 免配） + STS | 低 |
| 上传进度 | 无 | 支持分片上传 + 进度回调 | 低 |
| 文件管理 | 无 | 生命周期规则自动清理 | 中 |
| CDN 加速 | 无 | OSS + CDN（阿里云 CDN / DCDN） | 中 |
| 容灾 | 单地域 | 跨区域复制（CRR） | 高 |
| 防攻击 | Pre-signed URL 过期 | + 单用户频率限制 + 总量限额 | 低 |
| 可观测性 | 无 | OSS 访问日志 + 监控告警 | 中 |

---

## 高可用 & 高并发分析

### Pre-signed URL 模式的优势

此方案天然支持高并发，因为：
- 文件上传流量完全不走后端，直接到 OSS
- 后端只需处理获取凭证的轻量请求（毫秒级）
- OSS 是云服务，自带高可用和弹性带宽

### 瓶颈分析

| 组件 | 瓶颈 | 方案 |
|---|---|---|
| 后端（获取凭证） | 单实例可处理数万 QPS 的凭证请求 | 水平扩展 Spring Boot 实例 |
| OSS | 阿里云 SLA 99.9%+ | 单 Region 默认三副本，跨区域复制可选 |
| 凭证接口 | JWT 认证依赖 Redis（黑名单） | Redis 故障时降级放行（参见认证模块文档） |

### 规模化路径

1. **百万用户**：当前方案直接胜任
2. **千万用户**：OSS + CDN 加速图片分发，降低 OSS 回源流量成本
3. **亿级**：CDN 边缘缓存 + 跨区域 OSS 复制 + 客户端上传限速
