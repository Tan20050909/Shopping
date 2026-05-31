# CLAUDE.md — AI 开发约束文档

本项目允许 AI 协助开发，但必须严格遵守以下规则。除非用户明确授权，否则不得突破这些约束。

## 项目概览

中文电商购物平台，包含 5 个服务：

| 服务 | 端口 | 技术栈 |
|------|------|--------|
| 用户后端 | 8082 | Spring Boot 3.2, Java 21, MyBatis |
| 商家后端 | 8081 | Spring Boot 2.7, Java 17, MyBatis-Plus |
| 用户前端 | 5173 | Vue 3, Vite, Element Plus, fetch |
| 商家前端 | 3000 | Vue 3, Vite, Element Plus, axios |
| 公共首页 | 3001 | Vue 3, Vite, Element Plus |

启动方式：根目录 `start-all.sh` 或分别在各目录运行 `mvn spring-boot:run` / `npm run dev`。

数据库：MySQL `Shopping`，端口 3307，root/123456。建表文件在 `Shopping/user/src/main/resources/db/Shopping.sql`（56 张表）。

## 核心文件（不得轻易修改）

- `Shopping/user/src/main/java/org/example/service/ShoppingService.java` — 4300 行上帝服务，用户端几乎所有业务逻辑
- `Shopping/user/src/main/resources/db/Shopping.sql` — 数据库表结构定义
- `Shopping/user/frontend/src/main.js` — 用户端路由定义
- `Shopping/mechant/frontend/src/router/index.js` — 商家端路由 + 权限守卫
- `Shopping/user/frontend/src/App.vue` — 用户端主布局
- `Shopping/mechant/frontend/src/App.vue` — 商家端主布局（908 行）
- `Shopping/user/frontend/src/api/client.js` — 用户端 HTTP 请求封装
- `Shopping/mechant/frontend/src/api/index.js` — 商家端所有 API 接口定义
- `Shopping/user/src/main/resources/application.yml` — 用户后端配置
- `Shopping/mechant/backend/src/main/resources/application.yml` — 商家后端配置

## 1. 数据库约束

未经用户明确允许，禁止：

- 修改数据库表结构（字段名、字段类型、字段长度、主键、外键、索引、唯一约束）
- 新增、删除数据库表
- 删除字段
- 修改已有正式数据
- 编写会清空、覆盖、批量删除数据的 SQL

允许的操作：

- 添加少量测试数据
- 编写查询语句用于排查问题
- 阅读现有数据库结构
- 如确实需要改表，必须先说明原因、影响范围、具体 SQL，并等待用户确认

## 2. 代码修改范围约束

必须遵守最小修改原则：

- 只允许修改与当前任务直接相关的代码
- 不得顺手重构无关代码
- 不得因为"看起来更好"而大范围调整项目结构
- 不得随意改动公共组件、公共工具类、全局样式、路由配置、权限逻辑
- 不得删除已有功能
- 不得改变已有接口的返回格式，除非用户明确允许
- 不得修改与当前功能无关的页面、接口、服务或配置文件

如果发现无关代码存在问题，只能记录并告知用户，不能直接修改。

## 3. 文件操作约束

未经用户明确允许，禁止：

- 删除、重命名、移动文件
- 大规模格式化文件
- 修改 `.env`、`.gitignore`、`package-lock.json`、`pom.xml`、数据库配置、部署配置等关键文件
- 修改 `node_modules`、`dist`、`target`、`.idea`、`.vscode` 等生成目录或本地配置目录

如确实需要修改配置文件，必须先说明原因和影响，等待用户确认。

## 4. 功能开发流程

每次开发功能时，必须按以下流程执行：

1. 先阅读相关代码，不要立即修改
2. 说明本次功能会影响哪些文件
3. 给出简短开发方案
4. 等用户确认后再修改
5. 每次只完成一个小阶段
6. 修改完成后说明改了哪些文件、每个文件改了什么、如何测试

不得一次性完成多个大功能。

## 5. 测试要求

修改完成后，必须尽可能进行测试。至少说明：

- 前端如何启动测试
- 后端如何启动测试
- 需要测试哪些页面
- 需要点击哪些按钮
- 预期结果是什么
- 是否可能影响已有功能

如果无法运行测试，必须明确说明原因，不能假装已经测试通过。

## 6. Git 约束

修改代码前必须先检查当前状态：`git status`。如果存在未提交修改，必须先告知用户，不能直接继续改。

修改完成后，必须提供建议提交信息，例如：`git commit -m "feat: add order search filter"`。

不得擅自执行 `git reset --hard`、`git clean -fd`、`git push --force`、`git rebase`，除非用户明确要求。

## 7. 输出要求

每次完成任务后，必须用以下格式总结：

```text
本次修改总结：

1. 修改文件：
- xxx：做了什么
- xxx：做了什么

2. 新增文件：
- xxx：用途是什么

3. 是否修改数据库：
- 是 / 否
- 如果是，说明具体内容

4. 是否修改接口：
- 是 / 否
- 如果是，说明具体变化

5. 测试方式：
- 启动命令：
- 测试页面：
- 测试步骤：
- 预期结果：

6. 风险说明：
- 是否影响已有功能：
- 需要用户重点检查的地方：
```

## 8. 总原则

本项目的第一原则是：**稳定优先**。

AI 的任务不是自由发挥，而是在现有项目基础上小心添加功能。所有修改都必须做到：

- 可解释
- 可测试
- 可回退
- 范围小
- 不破坏现有功能
