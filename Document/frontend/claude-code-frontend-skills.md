# Claude Code 前端 UI 设计 Skills 汇总

> 整理时间：2026-05-24 | 适用于 JustGo (Vue 3 + Vite 8 + TypeScript 6) 项目

## 一、已安装 Skills（本项目当前）

| Skill | 来源 | 状态 |
|-------|------|------|
| `frontend-design` | `claude-plugins-official` (Anthropic 官方) | ✔ 已启用 |
| `superpowers` | `claude-plugins-official` | ✔ 已启用 |
| `code-review` | `claude-plugins-official` | ✔ 已启用 |
| `code-simplifier` | `claude-plugins-official` | ✔ 已启用 |
| `skill-creator` | `claude-plugins-official` | ✔ 已启用 |

---

## 二、推荐额外安装的 Skills

### 🔥 核心推荐（排名分先后）

#### 1. UI/UX Pro Max（62.6K+ GitHub Stars）

解决「设计决策瘫痪」问题——内置 240+ 设计风格、127 种字体搭配、99 条 UX 指南，v2.0 推理引擎自动匹配风格。

**安装：**
```bash
# 方式一：npm CLI（推荐，功能最完整）
npm install -g uipro-cli
cd JustGo-frontend
uipro init --ai claude

# 方式二：npx skills
npx skills add nextlevelbuilder/ui-ux-pro-max-skill
```

- GitHub: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill
- 跨平台：支持 React / Next.js / Vue / Nuxt / Svelte 等 15+ 平台

#### 2. Vercel Agent Skills — web-design-guidelines（19.5K+ Stars）

UI/UX 的 ESLint——100+ 条规则审计可访问性（WCAG）、语义化标签、ARIA、键盘导航、动画性能、表单规范等。

**安装：**
```bash
npx skills add vercel-labs/agent-skills --skill web-design-guidelines
```

- GitHub: https://github.com/vercel-labs/agent-skills
- 触发词："Review my UI"、"Check accessibility"、"Audit design"

#### 3. Taste Skill（6.6K+ Stars）

参数化风格调校器——三个参数控制设计强度：
- `DESIGN_VARIANCE`：安全 → 实验
- `MOTION_INTENSITY`：微妙 → 电影级
- `VISUAL_DENSITY`：疏朗 → 紧凑

**安装：**
```bash
npx skills add https://github.com/Leonxlnx/taste-skill
```

- GitHub: https://github.com/Leonxlnx/taste-skill
- 内含 9 个子技能：通用设计、图片转代码、重设计、极简风、野蛮主义风等

#### 4. Impeccable（"整容级"细节打磨）

20 个 Slash Commands，从"能用"升级到"高级感"。

**安装：**
```bash
git clone https://github.com/ba-archive/impeccable.git ~/.claude/skills/impeccable
```

- 核心命令：`/audit` `/critique` `/polish` `/overdrive`

#### 5. huashu-design（11.2K Stars）

"打字→回车→可交付设计"，支持 iOS 原型 / 幻灯片 / MP4 动画输出。

**安装：**
```bash
npx skills add alchaincyf/huashu-design
```

- 核心洞察：HTML 是 AI Agent 最好的交付格式

---

### 🟢 Vue 生态相关

| Skill | 说明 | 安装 |
|-------|------|------|
| **frontend-developer** | Vue 组件结构、TypeScript 约定、Inertia.js 模式 | `npx skills add @RasmusGodske/dev-agent-workflow --skill frontend-developer` |
| **UI/UX Pro Max** | 内含 Vue/Nuxt 适配，自动匹配合适的技术栈风格 | 见上方 |

---

## 三、常用工作流组合

| 阶段 | 目标 | 推荐 Skills |
|------|------|------------|
| **1. 方向** | 确定风格调性 | UI/UX Pro Max → Frontend Design |
| **2. 结构** | 构建布局与组件 | Frontend Design + Taste Skill |
| **3. 差异化** | 打磨细节 | Impeccable + Taste Skill |
| **4. 交付** | 质量审计 | Web Design Guidelines + code-review |

---

## 四、关键设计原则（所有 Skills 共识）

1. **禁止 AI Slop**：禁用 Inter/Roboto/Arial 等"烂大街"字体、紫蓝渐变配白底
2. **先定美学方向再写代码**：极简主义 / 复古未来 / 野蛮主义 / 瑞士风格等
3. **排版优先**：字体选择 > 颜色 > 动效
4. **可访问性内置**：WCAG AA 作为最低标准
5. **HTML 作为通用交付格式**：无二进制依赖，浏览器直接渲染

---

## 五、资源汇总

| 资源 | 链接 |
|------|------|
| Claude Code 官方市场 | https://claude.com/marketplace |
| 社区 Skills 目录 | https://claudeskills.info |
| 前端设计工具箱（70+ 工具） | https://github.com/wilwaldon/Claude-Code-Frontend-Design-Toolkit |
| Awesome UX Skills（20+ UX 专业技能） | https://github.com/tommyjepsen/awesome-ux-skills |
| Vercel Skills CLI | https://github.com/vercel-labs/skills |
| Awesome Claude Skills（12K+ Stars） | https://github.com/ComposioHQ/awesome-claude-skills |
| 前端开发必装 Skill 清单（掘金） | https://juejin.cn/post/7640677540941905970 |

---

## 六、针对 JustGo 项目的建议

JustGo 是一个「城市活动发现 + 搭子匹配」平台，面向年轻用户群体。建议安装优先级：

1. **UI/UX Pro Max** — 快速匹配年轻化/社交类设计风格
2. **Taste Skill** — 调校出独特的品牌视觉调性，避免千篇一律
3. **Web Design Guidelines** — 确保移动端可访问性和性能达标

这 3 个 + 已安装的 `frontend-design`，可以覆盖从风格定位到交付审计的完整流程。
