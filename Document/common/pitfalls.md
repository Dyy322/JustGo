# 注意事项（Pitfalls）

本文档记录项目中已踩过的坑及其解决方案。**每次开始实现新模块前必须完整阅读一遍**，避免旧错再犯。

---

## 1. MyBatis 与 Java Record 不兼容

**现象**：`DataIntegrityViolationException: Cannot convert string '...' to java.time.LocalDateTime value`

**原因**：MyBatis 3.5.x 对 Java record 默认使用构造器注入，按 SELECT 列的**顺序**（非名称）映射到构造器参数。当结果集含嵌套对象（`<association>`）或字段与构造器参数顺序不一致时，列会错位——例如 URL 字符串被注入到 `LocalDateTime` 参数。

**规则**：

- ❌ MyBatis `<resultMap>` / `<resultType>` 的结果类型**禁止**使用 Java record
- ✅ 必须使用普通 Java 类（有无参构造器 + getter/setter），嵌套类型同理
- ❌ `<constructor>` 不能包含 `<association>` 作为子元素（MyBatis DTD 禁止），无法用 `<constructor>` 映射含嵌套对象的 record

**来源**：2026-05-27 活动模块接口报错

---

## 2. OSS objectKey 不可直接返回给前端

**现象**：前端页面图片显示为裂图

**原因**：DB 中存储的是 OSS objectKey（如 `justgo/activity/abc.jpg`），不是可访问的 URL。前端直接当 `<img src>` 使用会导致图片无法加载。

**规则**：

- API 响应返回前必须将 objectKey 解析为预签名 URL（`OssService.generatePresignedGetUrl(objectKey)`）
- 如果字段值已是完整 URL（以 `http://` 或 `https://` 开头），直接透传
- `OssService.generatePresignedGetUrl()` 是纯本地签名计算，无网络开销，可在循环中调用

**涉及场景**：`Activity.coverImage`、`ActivityImage.url`、`User.avatar` 等所有存储 OSS objectKey 的字段，在 Service 层构建响应 DTO 时必须解析。

**来源**：2026-05-27 活动模块前端封面图不显示

---

## 3. Maven 命令必须在后端目录下执行

**现象**：`The goal you specified requires a project to execute but there is no POM in this directory`

**原因**：项目根目录是 `/JustGo`，后端 `pom.xml` 在 `/JustGo/JustGo-backend/` 子目录下。

**规则**：

- 执行 Maven 命令前先 `cd JustGo-backend`
- 或在根目录下显式指定：`mvn -f JustGo-backend/pom.xml <goal>`

**来源**：多模块 monorepo 目录结构

---

## 4. Vite 代理不会消除浏览器 Origin 校验

**现象**：`http://localhost:5173` 可以登录，但 `http://127.0.0.1:5173` 或局域网 IP 无法登录；浏览器中的 `POST /api/auth/login` 返回 `403`，而不带 `Origin` 的 `curl` 请求正常。

**原因**：浏览器通过 Vite `/api` 代理请求后端时仍会携带原始页面的 `Origin`。后端 CORS 白名单如果只允许 `http://localhost:5173`，会拒绝 `127.0.0.1` 和局域网 IP 来源。

**规则**：

- 开发环境需要同时加入 `localhost`、`127.0.0.1` 和当前局域网 IP 的前端地址
- 修改 `application.yaml` 后必须重启 Spring Boot 服务
- 局域网 IP 变化后需同步更新 `app.cors.allowed-origins`
- 生产环境仅允许正式前端域名，禁止使用宽泛的通配来源

**来源**：2026-05-30 移动端与 `127.0.0.1` 登录失败
