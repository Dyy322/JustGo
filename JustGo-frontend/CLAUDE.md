# CLAUDE.md — JustGo 前端

## 技术栈

Vue 3 + Vite 8 + TypeScript 6 + Composition API（`<script setup lang="ts">`）+ Element Plus + Pinia + Vue Router

路径别名 `@` → `./src`。

## 架构大纲

```
src/
├── api/           # HTTP 客户端 + 接口函数，按领域拆分
├── stores/        # Pinia 全局状态，按领域拆分
├── router/        # Vue Router 配置
├── types/         # 共享类型定义（ApiResponse<T> 等）
├── views/         # 页面级组件，与路由一一对应
├── components/    # 可复用组件，无业务逻辑
```

### 设计原则

- **View 是容器，Component 是积木**：View 负责组装和数据获取，Component 只接收 props 和 emit 事件，不直接调 API
- **乐观更新优先**：用户操作先改本地状态，接口失败时丢弃本地变更即可（下次请求自动同步服务端真相）
- **状态三态必有**：任何异步数据必须覆盖 loading / success / error 三种状态，不做"请求中 UI 无响应"的页面
- **错误边界清晰**：API 层统一处理 401 刷新和全局错误，View 层只处理业务异常

## 开发规范

### 组件设计

- `<script setup lang="ts">`，禁止 Options API
- Props 用 `defineProps<T>()` 泛型声明，Emits 用 `defineEmits<T>()`
- 组件文件名大驼峰（`FollowButton.vue`），View 文件名 `XxxView.vue`
- 公共组件放 `src/components/`，页面私有组件放 `src/views/<页面名>/components/`

### 状态管理

- **全局状态用 Pinia**：当前用户、认证 token 等跨页面共享数据
- **页面状态用 composable**：页面级 loading/error/data 用 `ref` + 组合函数封装，不放进 store
- **表单状态留在组件**：单个表单的输入、校验、提交状态不作为全局状态
- Store 不直接调 API：通过 `@/api/` 模块发请求，Store 只管理数据和调用时机

### API 调用

- 所有请求通过 `@/api/client.ts` 导出的 axios 实例，不直接使用 `axios`
- 请求拦截器自动注入 `Authorization: Bearer <token>`
- 响应拦截器处理 401：有 refreshToken → 自动刷新并重放；无 → 跳转 `/login`
- OSS 直传 PUT 用原始 `axios`（不走 client，避免注入 Authorization 头）
- 接口函数按领域拆文件：`auth.ts`、`user.ts`、`social.ts`、`file.ts`

### 错误处理

所有 catch 块统一使用 `getErrorMessage(err, fallback)`，**禁止**硬编码 fallback：

```typescript
import { getErrorMessage } from '@/api/error'

try {
  await someApi()
} catch (err) {
  ElMessage.error(getErrorMessage(err, '操作失败'))
}
```

`getErrorMessage` 提取优先级：JSON `message` 字段 → OSS XML `<Message>` → HTTP 状态码 → Network Error → fallback。

### API 响应约定

所有接口返回 `ApiResponse<T>`，`res.data.code === 0` 为成功。`code !== 0` 时直接显示 `res.data.message`，后端保证 message 对用户可读。

## 开发流程与设计哲学

接到新页面/新功能时，按以下路径推进：

- **先定数据流再写 UI**：这个页面需要哪些数据？从哪里来（API/Pinia/路由参数）？数据流向图比直接画 UI 更重要
- **API 类型先行**：在 `types/api.ts` 定义 DTO 类型，在 `api/<领域>.ts` 定义接口函数签名，前后端契约确认后再写 View
- **先跑通再打磨**：先让数据在页面上正确展示（哪怕样式丑），再处理 loading/empty/error 状态，最后打磨视觉
- **移动端优先**：先在 Chrome DevTools 手机视图下完成功能，再适配桌面布局

## 防御性编程原则

### 状态全覆盖

- **任何异步操作必须处理三种状态**：loading（骨架屏/spinner）、success（正常展示）、error（错误提示 + 重试按钮）
- **空数据不等于 loading**：列表返回 `[]` 时展示"暂无数据"空状态，不要显示骨架屏
- **表单提交中禁用按钮**：防止重复提交，`disabled="submitting"` + 按钮文字变为"提交中..."

### 竞态与清理

- **watchEffect / onMounted 中的异步必须有 cleanup**：组件卸载时 `AbortController.abort()` 或设置 `isCancelled` 标记，避免"已卸载组件 setState" 警告
- **搜索框防抖**：输入联想类请求用 `debounce(300ms)`，不做每次 keystroke 都发请求
- **切换页面时取消未完成请求**：同一位置快速切换 tab/路由，旧的请求结果不应覆盖新结果

### 乐观更新与回滚

- 关注/点赞/收藏等 toggle 操作：先改 UI → 发 API → API 失败时无需显式回滚（下次 `onMounted` 时服务端数据覆盖）

### 安全

- **Token 存 localStorage 有 XSS 风险**：生产环境考虑 httpOnly cookie。当前阶段至少确保不拼接用户输入到 URL
- **v-html 必须对内容做消毒**，禁止直接渲染用户输入或 API 返回的 HTML
- **敏感操作（删除/支付）必须有二次确认**，防误触

## 性能设计

- **路由懒加载**：所有 View 组件用 `() => import('@/views/XxxView.vue')` 动态导入
- **列表虚拟化**：超过 100 条的列表用 `vxe-table` 或 Element Plus 虚拟化，不做全量 DOM 渲染
- **图片懒加载**：列表中的图片用 `loading="lazy"` 或 Intersection Observer
- **避免响应式大对象**：只对 UI 需要响应的数据用 `ref`/`reactive`，纯展示大数据用 `shallowRef`
- **CDN 引入 Element Plus**：生产构建外置 UI 库，减小 bundle 体积

## 可观测性设计

### 错误追踪

- 全局 `app.config.errorHandler` 捕获未处理异常，上报到 Sentry 或类似平台（待建设）
- API 层错误在 `client.ts` 响应拦截器中统一记录，携带 `url`、`status`、`message`

### 日志

- 关键用户操作（登录、注册、关注、取关、文件上传）打 `console.info`，生产环境通过 vite-plugin 自动移除 `console.log`，保留 `console.error`

### 性能监控（待建设）

- Web Vitals（LCP、FID、CLS）通过 `web-vitals` 库采集

## 软件质量管理

### 代码审查

**原则**：关注数据流正确性和状态完整性，不关注样式细节（交给视觉走查）。

#### 强制检查项

- 异步操作是否覆盖了 loading / error / empty 状态
- 组件卸载时是否清理了副作用（timer、subscription、pending request）
- 是否有 XSS 风险（v-html 未消毒、URL 拼接用户输入）
- Token 或敏感信息是否打印到了 console
- 是否引入了不必要的依赖（package.json diff 必须有充分理由）

#### 重点关注项

- 大列表是否做了虚拟化或分页
- 是否存在 props drilling 超过 3 层（应该提取为 composable 或 provide/inject）
- 乐观更新后，API 失败场景是否有合理的兜底行为
- 表单提交是否防抖/防重复

## 代码风格

Prettier：无分号、单引号、100 字符、2 空格。
