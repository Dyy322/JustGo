# CLAUDE.md

此文件为 Claude Code（claude.ai/code）在本仓库中工作时提供指导。

## 项目概述

即行（JustGo）—— 一个发现城市活动、寻找搭子的平台(详细内容看README.md)。全栈 monorepo，包含 Vue 3 前端和 Spring Boot 后端，通过 `git subtree` 从独立仓库合并而成。

子目录 `JustGo-backend/CLAUDE.md` 和 `JustGo-frontend/CLAUDE.md` 分别包含后端和前端的架构细节。

## 开发命令

### 后端（`JustGo-backend/`）—— Spring Boot 4 + Java 17 + Maven

```bash
cd JustGo-backend
mvn spring-boot:run    # 启动开发服务器（端口 8080）
mvn test               # 运行测试
mvn clean package      # 构建 fat JAR
```

前置条件：MySQL 8（`localhost:3306/JustGo`，凭据见 `application.yaml`）和 Redis（`localhost:6379`）。首次启动前需执行 `src/main/resources/SQL/` 中的建表脚本。

### 前端（`JustGo-frontend/`）—— Vue 3 + Vite 8 + TypeScript 6

```bash
cd JustGo-frontend
npm install            # 安装依赖
npm run dev            # 启动开发服务器（端口 5173）
npm run build          # 类型检查 + 生产构建
npm run lint           # 运行 oxlint + eslint
npm run format         # Prettier 格式化
```

## 代码风格

Prettier 配置（`.prettierrc.json`）：无分号、单引号、每行最多 100 字符、2 空格缩进。

## 提交规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)，提交信息使用英文，格式为 `<type>: <简短描述>`：

| 类型 | 说明 |
|------|------|
| `feat` | 新功能或新模块 |
| `fix` | 缺陷修复 |
| `docs` | 文档或 CLAUDE.md 变更 |
| `refactor` | 重构（不改变外部行为） |
| `test` | 测试相关 |
| `chore` | 构建、依赖、配置等杂项 |

消息应简明扼要，一行说清做了什么。示例：`feat: 新增泛型状态机引擎 StateMachine`、`fix: 修复关注缓存击穿问题`。

**禁止**把不同性质的改动混在一个 commit 里（如重构 + 新功能 + 文档修改）。每个 commit 只做一类事，方便 review 和回滚。

## 关键原则

- **简单需求不走火入魔**：修个 typo 不需要写方案文档，但涉及新表或新模块的必须写
- **坏消息第一时间暴露**：发现需求理解有误/技术方案有漏洞，立刻同步，不要自己悄悄修
- **API 变更是 breaking change**：改了 DTO 字段名或端点路径，必须同步通知前端，杜绝"我以为你知道"的假设
- **前向兼容**：数据库加字段必须有默认值或允许 NULL，旧代码不报错；删除字段分两次上线（先标记废弃，再物理删除）
- **文档先行**：实现后端新模块前，必须先输出设计文档并确认方案；前端无需单独文档
- **不确定就问**：设计过程中有拿不准的决策点（技术选型、产品逻辑、交互细节），随时向用户提问确认，不要自己做决定

## 模块文档规范

每完成一个功能模块，必须在 `Document/` 目录下根据实际实现反哺完善文档：
- 后端模块文档放在 `Document/backend/` 下
- 通用组件文档放在 `Document/common/` 下（如状态机、限流器等跨模块复用组件）
- 前端无需单独文档

文档需覆盖以下内容：

1. **概述**：模块职责与功能简介
2. **涉及文件**：列出所有相关文件的路径与用途
3. **API 端点**（如有）：HTTP 方法、路径、认证要求、说明
4. **实现细节**：核心流程的逐步描述、关键设计决策、数据结构
5. **已解决的问题**：该模块针对性地解决了哪些技术问题
6. **未解决的问题与隐患**：按严重程度（P0/P1/P2）分级列出
   - 安全漏洞（OWASP Top 10 相关）
   - 并发/竞态条件
   - 数据一致性风险
   - 死代码、逻辑不一致等代码质量问题
7. **大厂标准对比**：从高可用、高并发、安全性、可观测性等维度与行业主流方案对比，标注差距
8. **高可用 & 高并发分析**：当前瓶颈识别 + 不同量级（百万/千万/亿级）的规模化方案
9. **数据库**（如有）：表结构、关键字段、索引设计
