# food-web Vue Router 迁移分析与实施清单

## 1. 结论摘要

当前仓库里有两套前端：

- `food-web`
- `food-platform-web`

其中，`food-platform-web` 很早就已经完成了 `vue-router` 接入；`food-web` 原本是本次迁移的目标对象。
但按当前代码重新核对后，`food-web` 也已经完成了 `vue-router`、`session`、路由守卫和认证路由接入，这份文档现在更适合作为“迁移分析与阶段核对清单”，而不再是迁移前状态说明。

因此，本次“将前端调整为使用 vue router”的目标，应当明确为：

- 改造对象：`food-web`
- 参考实现：`food-platform-web`
- 首轮目标：先完成页面级路由化和登录态路由守卫
- 暂不追求：首轮不强行拆散所有大页面组件

按 2026-04-10 当前代码状态判断：

- 第一阶段：已完成
- 第二阶段：已完成
- 第三阶段：已完成
- 第四阶段：已完成
- 第五阶段：已完成
- 第六阶段：已完成
- 第七阶段：已完成
- 第八阶段：已完成
- 第九阶段：已完成
- 第十阶段：已收掉公众端退出残留

## 2. 依据项目现状的判断

### 2.1 `food-web` 已完成从手工切页到路由化的迁移

当前代码中，`food-web` 入口已经注册 `router`：

- `food-web/src/main.js`

当前 `App.vue` 已退化为基于 `RouterView` 的根容器，并只保留全局未授权处理与 session 恢复：

- `food-web/src/App.vue`
- `food-web/src/router/index.js`
- `food-web/src/session/authSession.js`
- `food-web/src/session/authRuntime.js`

当前实际状态是：

- 页面级路由已建立
- 登录态持久化与恢复已建立
- 角色守卫已建立
- 详情页参数已迁到 route params / query
- 页面已统一改为在组件内部直接读取 `useRoute()` 与 `getActiveSession()`
- `router/index.js` 中旧的 `resolveRouteProps()` 适配层已移除
- 根组件不再承担旧版 `view` 总控职责

也就是说，本节的原始分析结论已经过期，当前更适合继续核对后续阶段是否全部收口。

### 2.2 `food-platform-web` 已经提供了成熟参考

`food-platform-web` 已经具备完整的路由入口和守卫结构：

- `food-platform-web/src/main.js`
- `food-platform-web/src/App.vue`
- `food-platform-web/src/router/index.js`
- `food-platform-web/src/session/authSession.js`
- `food-platform-web/src/session/authRuntime.js`

这套代码已经体现出比较清晰的做法：

- `main.js` 中注册 router
- `App.vue` 只保留 `RouterView`
- `router.beforeEach` 做登录态和权限控制
- `router.afterEach` 做标题处理
- 独立 `session` 模块保存和恢复登录态

因此，`food-web` 的改造不建议从零设计，应优先复用 `food-platform-web` 的结构思路。

## 3. `food-web` 当前迁移难点

### 3.1 根组件中耦合了大量状态

`food-web/src/App.vue` 中目前集中管理了：

- `view`
- `adminToken`
- `enterpriseToken`
- `publicToken`
- `regulatorToken`
- `enterpriseDetailId`
- `complaintDetailId`
- `returnView`
- `complaintReturnView`
- `regulatorReturnSection`

这意味着迁移时必须把下面几类状态拆出去：

- 页面导航状态交给 `vue-router`
- 登录态交给 `session` 模块
- 详情页 ID 改为 route params
- 返回来源改为 route query 或明确的目标路由

### 3.2 登录页当前是事件驱动，不是路由驱动

`food-web/src/views/AuthView.vue` 当前逻辑是：

- 登录成功后 `emit("admin-login")`
- 或 `emit("enterprise-login")`
- 或 `emit("public-login")`
- 或 `emit("regulator-login")`

再由 `App.vue` 负责切页面。

这套机制迁到 router 后，要改为：

- 登录组件自己写入 session
- 登录组件自己根据角色调用 `router.replace(...)`

### 3.3 详情页已具备“参数驱动”的雏形

以下页面本身已经是“拿 ID -> 请求详情 -> watch ID 变化”的结构，比较适合直接迁到路由参数：

- `food-web/src/views/EnterpriseDetailView.vue`
- `food-web/src/views/PublicBulletinDetailView.vue`
- `food-web/src/views/PublicEnterpriseDetailView.vue`
- `food-web/src/views/PublicSamplingResultDetailView.vue`
- `food-web/src/views/RegulatorAdminComplaintDetailView.vue`
- `food-web/src/views/RegulatorEnforcerComplaintDetailView.vue`

这类页面改造成本相对低，重点是把原来的 `props` 来源从父组件改成 route params。

### 3.4 监管端和企业端是“大页签组件”

当前很多页面不是“一路由一页面”，而是“一个大组件里包含多个业务 section”。

典型文件：

- `food-web/src/views/RegulatorAdminView.vue`
- `food-web/src/views/RegulatorEnforcerView.vue`
- `food-web/src/views/EnterpriseProfileView.vue`
- `food-web/src/views/AdminView.vue`

其中：

- `RegulatorAdminView.vue` 已有 `initialSection`
- `RegulatorEnforcerView.vue` 已有 `initialSection`
- `EnterpriseProfileView.vue` 只有本地 `section`
- `AdminView.vue` 只有本地 `subSection`

这决定了首轮最稳妥的方案不是立即把这些文件全部拆成很多小页面，而是：

- 先保留大组件
- 通过不同 path 映射到同一个组件
- 用 `props.initialSection` 或类似参数决定初始页签

### 3.5 当前不存在统一登录态持久化

`food-web` 目前没有类似 `food-platform-web/src/session` 的模块。  
也就是说，刷新页面后登录状态大概率无法自然恢复，改到 router 之后如果不先补 session 层，用户体验会更差。

因此，会话模块应当是路由改造前置工作，而不是收尾工作。

## 4. 迁移设计原则

本项目更适合采用下面的迁移原则：

### 4.1 首轮只做页面级路由化

先解决：

- 地址栏可表达页面状态
- 浏览器前进后退可用
- 刷新不丢主页面
- 受限页面有守卫
- 详情页可直达

先不要同时做：

- 全量组件拆分
- 全量状态管理重写
- UI 重构

### 4.2 优先复用现有大组件

不要一开始就把监管端几十个业务区块全部拆散。  
更稳妥的做法是：

- `/regulator/admin/dispatch` 和 `/regulator/admin/sampling` 等多个路径
- 都指向 `RegulatorAdminView.vue`
- 用 `initialSection` 或 route meta 决定默认区块

这样可以显著降低首轮改造风险。

### 4.3 将“返回来源”标准化为路由信息

当前 `App.vue` 中依赖：

- `returnView`
- `complaintReturnView`
- `regulatorReturnSection`

改造后建议统一成：

- route params 传业务对象 ID
- route query 传来源 section，例如 `?from=warnings`

避免继续保留额外的根状态。

## 5. 建议的首版路由结构

建议首版直接规划成下面这套路由，而不是边做边补：

```text
/
/login
/register/public
/register/enterprise

/public
/public/bulletins
/public/bulletins/:bulletinId
/public/enterprises
/public/enterprises/:enterpriseId
/public/sampling-results
/public/sampling-results/:samplingResultId
/public/complaints/new
/public/complaints

/admin/regulators/create
/admin/regulators/list

/enterprise/profile
/enterprise/products
/enterprise/inspections
/enterprise/rectifications

/regulator/admin/enterprises
/regulator/admin/approvals
/regulator/admin/dispatch
/regulator/admin/sampling
/regulator/admin/inspections
/regulator/admin/complaints
/regulator/admin/complaints/:complaintId
/regulator/admin/rectifications
/regulator/admin/warnings
/regulator/admin/bulletins
/regulator/admin/stats
/regulator/admin/enterprises/:enterpriseId

/regulator/enforcer/enterprises
/regulator/enforcer/tasks
/regulator/enforcer/sampling
/regulator/enforcer/inspections
/regulator/enforcer/complaints
/regulator/enforcer/complaints/:complaintId
/regulator/enforcer/rectifications
/regulator/enforcer/warnings
/regulator/enforcer/stats
/regulator/enforcer/enterprises/:enterpriseId
```

## 6. 分阶段实施清单

以下清单按“可以连续执行、尽量减少返工”的顺序组织。

### 第一阶段：搭建基础设施

状态：已完成

完成依据：

- `food-web/package.json` 已包含 `vue-router`
- `food-web/src/router/index.js` 已存在
- `food-web/src/session/authSession.js` 已存在
- `food-web/src/session/authRuntime.js` 已存在
- 已具备 router 实例、session 持久化、登录态恢复和登出清理能力

1. 给 `food-web/package.json` 增加 `vue-router` 依赖，版本建议与 `food-platform-web` 对齐。
2. 新建 `food-web/src/router/index.js`。
3. 新建 `food-web/src/session/authSession.js`。
4. 新建 `food-web/src/session/authRuntime.js`。
5. 将 `food-platform-web/src/session/authSession.js`、`authRuntime.js` 的结构迁移过来，再按 `food-web` 的角色模型做调整。

这一阶段的目标不是完成所有跳转，而是先有：

- router 实例
- session 持久化
- 登录态恢复能力
- 登出清理能力

### 第二阶段：改造入口与根组件

状态：已完成

完成依据：

- `food-web/src/main.js` 已接入 `router`
- `food-web/src/App.vue` 已改为 `RouterView`
- `App.vue` 中已不再保留旧版 `view` 驱动的整站切页逻辑
- 旧的多角色 token、详情 ID、返回状态不再由根组件集中维护

1. 修改 `food-web/src/main.js`，接入 `router`。
2. 重写 `food-web/src/App.vue`。
3. 去掉 `App.vue` 中基于 `view` 的 `v-if / v-else-if` 页面切换。
4. 去掉 `App.vue` 中的各角色 token、用户对象、详情 ID、本地返回状态。
5. 让 `App.vue` 只保留：
   - `<RouterView />`
   - 全局未授权处理
   - 必要的 session 恢复逻辑

这一阶段完成后，`App.vue` 应从“整站状态中心”降级为真正的根容器。

### 第三阶段：优先改造认证路由

状态：已完成

完成依据：

- `/login`、`/register/public`、`/register/enterprise` 已是正式路由
- `food-web/src/views/AuthView.vue` 已改为直接写入 session 并按角色跳转，不再依赖 `App.vue` 登录事件桥接
- 公众注册、企业注册已通过正式路由切换
- 游客页守卫已在 `router.beforeEach` 中生效

结论：

- 登录逻辑已经不再依赖 `App.vue`
- 因此可以进入第四阶段
- 结合当前代码，第四阶段所述公众端路由化实际上也已经基本完成，后续更适合做“阶段完成度复核与文档校正”，而不是把第四阶段当作尚未启动

1. 将登录页拆成独立路由：
   - `/login`
   - `/register/public`
   - `/register/enterprise`
2. 改造 `food-web/src/views/AuthView.vue`：
   - 移除对父组件登录事件的依赖
   - 登录成功后写入 session
   - 根据角色直接跳转到对应首页
3. 将公众注册、企业注册从组件内部切换改为真正的页面路由切换。
4. 增加游客页面守卫：
   - 已登录用户访问 `/login` 或注册页时应重定向

这一阶段完成后，登录逻辑就不再依赖 `App.vue`。

### 第四阶段：改造公众端

状态：已完成

完成依据：

- 公众首页已落为正式路由 `/public`
- 公告列表、企业公示列表、抽检结果列表均为真实路由页面
- `PublicBulletinDetailView.vue`、`PublicEnterpriseDetailView.vue`、`PublicSamplingResultDetailView.vue` 均已改为参数路由详情页
- 公众端页面内部已直接使用 `router.push(...)` 导航，不再依赖 `App.vue` 公众端 `open-* / view-* / back` 事件桥接
- `App.vue` 中公众端相关导航桥接监听已清理；公众端 9 个页面残留的退出登录事件桥接也已改为页面内直接调用 `performLogout + router.replace(...)`

结论：

- 第四阶段已经收口完成
- 可以进入第五阶段
- 结合当前代码，管理员端路由化也已经落地，第五阶段实际上已经完成

1. 将公众首页改造成 `/public`。
2. 将公告列表、企业公示列表、抽检结果列表都改成真实路由页面。
3. 将以下详情页改成参数路由：
   - `PublicBulletinDetailView.vue`
   - `PublicEnterpriseDetailView.vue`
   - `PublicSamplingResultDetailView.vue`
4. 将原来通过 `$emit("open-...")` 的跳转改成 `router.push(...)`。
5. 保留 `PublicComplaintTrackView.vue` 当前的“列表 + 弹窗详情”模式，不在首轮强拆成详情页路由。

这样做的原因是：

- 公告/企业/抽检详情本身就是完整切页
- 投诉追踪当前更像一个列表页内弹窗，不一定值得首轮拆成独立页面

### 第五阶段：改造管理员端

状态：已完成

完成依据：

- `AdminView.vue` 已通过 `route.meta.initialSection` 决定默认区块
- `/admin/regulators/create` 与 `/admin/regulators/list` 已复用同一组件
- `subSection` 已根据路由初始化
- 页签切换已通过 `router.push(...)` 驱动，而不只是本地状态切换

结论：

- 第五阶段可以进入，但当前代码已经越过“进入”状态，管理员端路由化收口已完成
- 后续不建议再把第五阶段当作待开发项，而应继续核对第六阶段及以后是否还有真实剩余差异

1. 通过路由 meta 为 `AdminView.vue` 提供默认区块信息。
2. 用两个路径复用同一个组件：
   - `/admin/regulators/create`
   - `/admin/regulators/list`
3. 将当前 `subSection = "create" | "list"` 改成根据路由初始化。
4. 在页签切换按钮中补 `router.push(...)`，而不只是改本地状态。

### 第六阶段：改造企业端

状态：已完成

完成依据：

- `EnterpriseProfileView.vue` 已通过 `route.meta.initialSection` 决定默认区块
- `/enterprise/profile`、`/enterprise/products`、`/enterprise/inspections`、`/enterprise/rectifications` 已复用同一组件
- 组件内部的 section 切换已通过 `router.push(...)` 驱动，不再只是本地状态切换
- 组件初始化已根据当前路由决定默认 section，因此地址栏表达、刷新恢复和前进后退已成立

结论：

- 第六阶段可以进入，但当前代码实际上已经完成第六阶段
- 后续更适合继续核对第七阶段监管管理员端是否还有真实剩余差异，而不是重复改企业端

1. 通过路由 meta 为 `EnterpriseProfileView.vue` 提供默认区块信息。
2. 用以下路径复用同一个组件：
   - `/enterprise/profile`
   - `/enterprise/products`
   - `/enterprise/inspections`
   - `/enterprise/rectifications`
3. 将当前 `section` 的切换按钮改成 `router.push(...)`。
4. 组件初始化时根据当前路由决定默认 section。

这样能在不拆组件的前提下先完成地址栏表达和刷新恢复。

### 第七阶段：改造监管管理员端

状态：已完成

完成依据：

- `RegulatorAdminView.vue` 已作为首轮承载组件保留
- `/regulator/admin/enterprises`、`/approvals`、`/dispatch`、`/sampling`、`/inspections`、`/complaints`、`/rectifications`、`/warnings`、`/bulletins`、`/stats` 已通过不同路径复用同一组件，并用 `route.meta.initialSection` 区分默认区块
- 监管管理员端组件内部的 section 切换已通过 `router.push(...)` 驱动
- 企业详情页已使用独立参数路由 `/regulator/admin/enterprises/:enterpriseId`
- 投诉详情页已使用独立参数路由 `/regulator/admin/complaints/:complaintId`
- 从列表页进入详情页时，已通过 query 传递来源 section，例如 `?from=warning`、`?from=complaints`

结论：

- 第七阶段可以进入，但当前代码已经完成第七阶段
- 后续更适合继续核对第八阶段监管执法端，而不是重复改监管管理员端

1. 保留 `RegulatorAdminView.vue` 作为首轮承载组件。
2. 用不同 path 指向同一个组件，并通过 `route.meta.initialSection` 区分默认区块：
   - `/regulator/admin/enterprises`
   - `/regulator/admin/approvals`
   - `/regulator/admin/dispatch`
   - `/regulator/admin/sampling`
   - `/regulator/admin/inspections`
   - `/regulator/admin/complaints`
   - `/regulator/admin/rectifications`
   - `/regulator/admin/warnings`
   - `/regulator/admin/bulletins`
   - `/regulator/admin/stats`
3. 将当前组件内部的 section 按钮改成 `router.push(...)`。
4. 把企业详情页改成独立参数路由：
   - `/regulator/admin/enterprises/:enterpriseId`
5. 把投诉详情页改成独立参数路由：
   - `/regulator/admin/complaints/:complaintId`
6. 将当前依赖 `fromSection` 的跳转改成 query，例如：
   - `?from=warning`
   - `?from=complaints`

### 第八阶段：改造监管执法端

状态：已完成

完成依据：

- `RegulatorEnforcerView.vue` 已作为首轮承载组件保留
- `/regulator/enforcer/enterprises`、`/tasks`、`/sampling`、`/inspections`、`/complaints`、`/rectifications`、`/warnings`、`/stats` 已通过不同路径复用同一组件，并用 `route.meta.initialSection` 区分默认区块
- 执法端组件内部的 section 切换已通过 `router.push(...)` 驱动
- 企业详情页已使用独立参数路由 `/regulator/enforcer/enterprises/:enterpriseId`
- 投诉详情页已使用独立参数路由 `/regulator/enforcer/complaints/:complaintId`
- 从列表页进入详情页时，已通过 query 传递来源 section
- 原先的 `emit("view-enterprise")`、`emit("view-complaint")` 已完成下线，监管端详情跳转已统一改为组件内直接 `router.push(...)`

结论：

- 第八阶段可以进入，但当前代码已经完成第八阶段
- 到这里，第八阶段以前的主体迁移工作已经全部完成，后续更适合核对第九、第十阶段的收口项，而不是继续在第八阶段重复修改
1. 保留 `RegulatorEnforcerView.vue` 作为首轮承载组件。
2. 用不同 path 指向同一个组件，并通过 `route.meta.initialSection` 区分默认区块：
   - `/regulator/enforcer/enterprises`
   - `/regulator/enforcer/tasks`
   - `/regulator/enforcer/sampling`
   - `/regulator/enforcer/inspections`
   - `/regulator/enforcer/complaints`
   - `/regulator/enforcer/rectifications`
   - `/regulator/enforcer/warnings`
   - `/regulator/enforcer/stats`
3. 将企业详情和投诉详情迁到独立参数路由：
   - `/regulator/enforcer/enterprises/:enterpriseId`
   - `/regulator/enforcer/complaints/:complaintId`
4. 将 `emit("view-enterprise")`、`emit("view-complaint")` 改为直接跳路由。

### 第九阶段：接入路由守卫

状态：已完成

完成依据：

- `router.beforeEach` 已存在，并统一执行 session 恢复
- 游客页限制已生效：已登录用户访问 `/login` 或注册页会被重定向
- 登录后页面限制已生效：未登录访问受限页会跳回 `/login`
- 按角色访问控制已生效：`ADMIN`、`ENTERPRISE`、`PUBLIC`、`REGULATOR_ADMIN`、`REGULATOR_ENFORCER`
- 登录用户访问不匹配角色页面时，会跳转到本角色默认首页

结论：

- 第九阶段已完成
- 路由系统当前已经不是“只接入了页面”，而是完成了登录态恢复和角色守卫闭环

1. 在 `router.beforeEach` 中恢复 session。
2. 处理游客页面限制。
3. 处理登录后页面限制。
4. 按角色限制访问：
   - `ADMIN`
   - `ENTERPRISE`
   - `PUBLIC`
   - `REGULATOR_ADMIN`
   - `REGULATOR_ENFORCER`
5. 未登录访问受限页时重定向到 `/login`。
6. 登录用户访问不匹配角色的页面时跳转到本角色首页。

这一部分可以直接参考 `food-platform-web/src/router/index.js` 的守卫结构，但要扩展到 `food-web` 的多角色场景。

### 第十阶段：收口与回归

状态：已移除页面级与路由级兼容层

完成依据：

- `router.afterEach` 已存在并统一处理页面标题
- 404 路由已存在并统一重定向
- `App.vue` 已不再保留旧版手工切页导航代码
- 认证阶段遗留事件 `admin-login`、`enterprise-login`、`public-login`、`regulator-login` 已清理
- 监管端详情跳转相关旧事件 `view-enterprise`、`view-complaint` 已从 `App.vue` 桥接和监管端页面组件声明中清理，管理员端和执法端组件已直接走路由跳转
- 旧的返回状态变量 `returnView`、`complaintReturnView`、`regulatorReturnSection` 已不再存在于当前结构中
- 公众端 9 个页面残留的退出登录事件桥接已改为页面内直接调用 `performLogout + router.replace(...)`
- 认证页、公众端、管理员端、企业端、监管管理员端、监管执法端及详情页均已改为组件内部直接读取 `useRoute()` 与 `getActiveSession()`
- `router/index.js` 中旧的 `resolveRouteProps()` 适配层已删除

结论：

- 第十阶段已完成兼容层收口
- 至此，这份文档所规划的 Vue Router 迁移主体工作已经全部落地
- 后续更适合把文档整体重命名或改写为“迁移完成度复核文档”，而不是继续作为待实施清单

1. 补齐 `router.afterEach` 页面标题逻辑。
2. 统一 404 路由。
3. 清理 `App.vue` 中遗留的导航代码。
4. 清理不再需要的事件传递：
   - `admin-login`
   - `enterprise-login`
   - `public-login`
   - `regulator-login`
   - `view-enterprise`
   - `view-complaint`

补充校正：

- 上述收口项在当前代码中均已落地，不再是待办
- 当前剩余工作重点应从“迁移实施”转为“质量巡检、命名梳理、重复实现收敛”
5. 清理旧的返回状态变量：
   - `returnView`
   - `complaintReturnView`
   - `regulatorReturnSection`

## 7. 推荐的具体实施顺序

如果要实际编码，建议严格按下面顺序执行：

1. 安装 `vue-router`
2. 建立 `router` 和 `session` 基础模块
3. 改 `main.js`
4. 改 `App.vue`
5. 先改登录与注册
6. 再改公众端
7. 再改管理员端
8. 再改企业端
9. 再改监管管理员端
10. 再改监管执法端
11. 最后补守卫、404、标题和清理旧代码

这个顺序的原因是：

- 认证和 session 是所有后续页面的前提
- 公众端相对简单，适合先验证路由方案
- 监管端最复杂，应该放在后面

## 8. 本次改造中需要特别注意的风险

### 8.1 不要把“页签状态”和“详情页路由”混在一起继续存在于根组件

只要继续保留 `App.vue` 那套 `view + detailId + returnView` 体系，最后就会变成“表面用了 router，实际还是手工切页”。

### 8.2 首轮不要同时做全量拆组件

监管管理员端和执法端都很重，首轮先做“同组件多路径”更稳。  
如果一开始就拆成很多业务页面，返工风险会明显增加。

### 8.3 先补登录态恢复，再做路由守卫

否则刷新页面会直接丢失状态，用户一进入深链接就被错误踢回登录页。

### 8.4 统一 section 命名

当前代码中存在：

- `warning`
- `warnings`

这类命名不一致在迁路由时很容易出错，应该在改造过程中统一。

## 9. 建议产出物

本次改造最终建议至少产出以下文件和结果：

- `food-web/src/router/index.js`
- `food-web/src/session/authSession.js`
- `food-web/src/session/authRuntime.js`
- 重写后的 `food-web/src/App.vue`
- 接入 router 的 `food-web/src/main.js`
- 路由化后的认证页
- 路由化后的公众端、企业端、管理员端、监管端

## 10. 最终建议

对于当前项目，最合理的方案不是“把所有页面完全重做一遍”，而是：

- 先把 `food-web` 从 `App.vue` 手工切页改成真正的 `vue-router`
- 先实现页面级路由、登录态恢复、角色守卫、详情页直达
- 首轮保留监管端和企业端的大组件结构
- 等路由体系稳定之后，再考虑第二轮做细粒度组件拆分

这条路径最符合当前项目结构，也最不容易把改造做成高风险重写。
