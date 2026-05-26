# 对象存储模块（OSS Module）说明文档

## 概述

基于阿里云 OSS 的文件存储模块，采用 **Pre-signed URL 直传**方案。客户端先从后端获取预签名的上传 URL，然后直接 PUT 文件到 OSS，文件流不经过后端服务器，节省带宽。适用于头像、活动图片等用户上传场景。

## 涉及文件

```
JustGo-backend/src/main/java/com/dyu/justgobackend/
├── oss/
│   ├── OssProperties.java          # OSS 配置 record（@ConfigurationProperties）
│   └── OssService.java             # Pre-signed URL 生成服务 + 启动自检
├── controller/
│   └── FileController.java         # 上传凭证 REST 接口
├── dto/
│   ├── request/UploadTokenRequest.java     # 上传凭证请求 DTO
│   └── response/UploadTokenResponse.java   # 上传凭证响应 DTO
├── pom.xml                          # 添加 aliyun-sdk-oss 依赖
└── resources/application.yaml       # 添加 app.oss.* 配置

JustGo-frontend/src/
├── api/
│   ├── file.ts                      # getUploadToken() 接口调用
│   └── error.ts                     # getErrorMessage() 错误消息提取（支持 JSON + OSS XML）
└── views/
    └── SettingsView.vue             # 头像上传全流程（获取凭证 → PUT OSS → 更新资料）
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

## 排错实录：Pre-signed URL 签名不匹配问题

### 时间线

```
[现象] 前端头像上传 → "头像上传失败"
  ↓
[排查1] 后端正常返回 getUploadToken（code=0），排除了获取凭证环节
  ↓
[排查2] 前端 catch 块用硬编码 fallback 消息，看不到真实错误 → 新增 getErrorMessage() 工具函数
  ↓
[排查3] 错误变为 "请求失败 (403)" → 说明 OSS CORS 已配通，请求到达了 OSS
  ↓
[排查4] OSS 返回 XML: "The request signature we calculated does not match the signature you provided"
  ↓
[排查5] 尝试去掉预签名 URL 的 Content-Type 约束 → 仍报签名不匹配
  ↓
[排查6] 新增后端自检（服务端发 PUT 到 OSS），结果 PUT 成功但 DELETE 失败
  ↓
[关键发现] 自检 PUT 没带 Content-Type 头 → 成功。浏览器 PUT 带了 Content-Type 头 → 失败
  ↓
[根因确认] 预签名 URL 未指定 Content-Type → 签名中 Content-Type 为空值。
           浏览器发送 File 时自动带 Content-Type → OSS 发现请求头与签名不一致 → 403
  ↓
[修复] 预签名 URL 重新加入 Content-Type，前后端用相同算法推导 MIME 类型，保证一致
  ↓
[验证] 自检通过 + 前端上传成功
```

### 发现的 4 个缺陷与修复

#### 缺陷 1：前端错误消息丢失（P1）

**症状**：后端 `BusinessException` 已正确抛出（如 `"不能关注自己"`、`"OSS 凭证未配置"`），但前端 catch 块使用硬编码 fallback（`'操作失败'`），用户看不到真实错误原因。

**根因**：`ProfileView.vue`、`FollowListView.vue`、`SettingsView.vue` 的 catch 块未捕获 error 对象，直接使用硬编码消息。

**修复**：
- 新增 `src/api/error.ts` — `getErrorMessage(err, fallback)` 工具函数，按优先级提取：
  1. JSON 响应：`err.response.data.message`
  2. OSS XML 响应：正则提取 `<Message>...</Message>`
  3. HTTP 错误状态码：`请求失败 (${status})`
  4. Network Error：提示检查 CORS 配置
  5. 以上都没有：使用 fallback
- 所有视图的 catch 块改为 `catch (err) { ElMessage.error(getErrorMessage(err, 'xxx')) }`

#### 缺陷 2：OSS 凭证空值未提前校验（P1）

**症状**：`application.yaml` 中 `access-key-id: ${OSS_ACCESS_KEY_ID:}` 默认空字符串。`OssService.init()` 只检查了 `endpoint` 和 `bucketName` 非空就创建 client，但凭证为空时 client 仍能创建（SDK 不验证），`generatePresignedUrl` 是纯本地计算也能成功。真正的错误要到前端直传 OSS 时才暴露。

**修复**：`ensureConfigured()` 增加凭证空值检查：
```java
if (properties.accessKeyId().isBlank() || properties.accessKeySecret().isBlank()) {
    throw new BusinessException(503, "OSS 凭证未配置，请设置 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET 环境变量");
}
```
问题在第一步（获取凭证）就暴露，而非等到第二步（PUT OSS）才出现模糊错误。

#### 缺陷 3：Pre-signed URL 签名与请求 Content-Type 不一致（P0）★ 核心问题

**症状**：前端直传 OSS 报 `403 The request signature we calculated does not match the signature you provided`。

**排错过程**：

1. **初步怀疑 CORS**：加了 Network Error 检测后发现是 403 而非 CORS 错误，排除。
2. **怀疑 RAM 权限**：日志显示 `AccessDenied / NoPermissionType: ImplicitDeny / AuthAction: oss:GetBucketAcl`，授予 `PutObject` 权限后仍 403，排除（该 AccessDenied 是 `doesBucketExist` 内部调用的 `GetBucketAcl` 无权限导致的，不影响 PUT）。
3. **后端自检**：在 `OssService.runSelfTest()` 中用 `HttpURLConnection` 从服务端直接发 PUT 到 OSS — **成功**。但浏览器发同样的 PUT — **失败**。关键区别：自检没带 Content-Type 头，浏览器带了。
4. **确认根因**：OSS 签名算法中 `Content-Type` 是签名要素之一。预签名 URL 未指定 `Content-Type` → 签名中该项为空。浏览器发送 File/Blob 时自动附加 `Content-Type: image/png` → OSS 服务端计算签名时期望为空，实际收到非空 → 签名不匹配。

**OSS 签名规范（Signature V1）**：
```
Signature = base64(hmac-sha1(AccessKeySecret,
            HTTP-METHOD + "\n"
            + Content-MD5 + "\n"
            + Content-Type + "\n"     ← 这里！
            + Expires + "\n"
            + CanonicalizedOSSHeaders
            + CanonicalizedResource))
```

**修复**：

后端 `OssService.generateUploadToken()` — 预签名 URL 恢复 `Content-Type`：
```java
request.setContentType(contentType(ext));
```

前端 `SettingsView.vue` — 用 `ext` 推导 Content-Type（与后端 `contentType()` 一致）：
```javascript
const mimeType = { jpg: 'image/jpeg', jpeg: 'image/jpeg', png: 'image/png', 
                   gif: 'image/gif', webp: 'image/webp' }[ext] || 'application/octet-stream'
await axios.put(uploadUrl, file, { headers: { 'Content-Type': mimeType } })
```

**为什么之前去掉 Content-Type 反而出错**：直觉上"签名不包含 Content-Type 就不需要匹配"，但实际是——签名包含 Content-Type 且值为空，请求中却带了非空的 Content-Type，空 ≠ 非空，签名不匹配。

#### 缺陷 4：后端自检误报（P2）

**症状**：`runSelfTest` 中 PUT 成功（HTTP 200），但随后 `client.deleteObject()` 因 RAM 用户无 `oss:DeleteObject` 权限而抛异常，整个 `runSelfTest` 被 catch 捕获后输出"OSS 自检失败"，误导出"上传功能不可用"的结论。

**修复**：`deleteObject` 用独立 try-catch 包裹，忽略清理失败，不干扰主流程判断。

### 关键教训

| # | 教训 |
|---|------|
| 1 | `ossClient.generatePresignedUrl()` 是**纯本地计算**，不验证凭证是否有效、Bucket 是否存在。必须通过实际 HTTP 请求验证 |
| 2 | 预签名 URL 的 Content-Type 必须与实际请求**完全一致**（含大小写）。不要依赖 `file.type`，而是用 `ext` 推导 |
| 3 | 错误消息链路要完整：后端 → GlobalExceptionHandler → `ApiResponse.fail()` → 前端 `err.response.data.message` → 用户可见。每个环节都不能断 |
| 4 | 浏览器和 Java 发 HTTP 请求行为不同：浏览器发 File 会自动带 Content-Type，`HttpURLConnection` 不设置就不带。排错时要在**同环境**复现 |

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
| **前端错误消息丢失** | **新增 `getErrorMessage()` 工具函数，支持 JSON / OSS XML / Network Error 三级提取** |
| **OSS 凭证空值未提前暴露** | **`ensureConfigured()` 启动时即检查凭证是否为空** |
| **Pre-signed URL 签名不匹配** | **预签名 URL 指定 Content-Type，前后端用相同算法（按 ext）推导 MIME 类型** |
| **后端自检误报** | **`deleteObject` 失败不影响自检结论，独立 catch 处理** |

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

#### 3. OSS 配置缺失时启动不报错 ✅ 已解决

> **已于 2026-05-26 修复**：`init()` 方法增加凭证空值检查和 `runSelfTest()` 启动自检。
> - 凭证未配置 → WARN 日志明确提示
> - 凭证已配置 → 启动时发 PUT 请求到 OSS 验证连通性
> - 自检失败 → ERROR 日志包含具体原因

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
