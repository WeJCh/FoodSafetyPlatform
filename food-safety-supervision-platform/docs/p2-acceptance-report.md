# P2 整体验收报告

版本：v0.1  
日期：2026-03-24  
对应阶段：P2-6 联调与验收  
特殊说明：本轮按当前要求直接执行 P2-6，P2-5 配置治理未纳入本次通过范围。

---

## 1. 验收目标

本轮验收目标不是继续扩展功能，而是确认 P2-1 到 P2-4 已经把项目从“只有企业主体、检查整改和投诉协同”推进到更贴近食品安全监管主线的状态，即至少形成以下四条业务增强线中的前三条：

1. 企业产品档案
2. 抽检任务与结果最小闭环
3. 监管概览统计聚合

按原始 P2 清单，P2 还包含第 4 条“配置治理”。但本轮已明确跳过 P2-5，因此本报告不会把配置治理写成已验收通过事项。

---

## 2. 项目分析与现状复核

### 2.1 项目结构

- 后端是 Maven 多模块微服务工程，根模块包含：
  - `gateway-service`
  - `user-service`
  - `regulation-service`
  - `regulation-operation-service`
  - `complaint-service`
  - `warning-service`
  - `query-service`
  - `platform-common`
- 前端位于 `food-web`，采用 Vue 3 + Vite。
- 当前前端不是基于 Vue Router 的门户，而是 `App.vue` 内部按角色与工作台视图做切换。

### 2.2 技术结构

- 后端：Spring Boot 3.2.5、Spring Cloud 2023、Spring Cloud Alibaba、OpenFeign、MyBatis-Plus、JWT
- 前端：Vue 3、Vite
- 数据层：MySQL，多服务各自维护 schema.sql
- 基础设施：Nacos、Sentinel、MinIO

### 2.3 与 P2-6 直接相关的业务落地状态

本轮复核确认，P2-1 到 P2-4 在当前代码中都已存在明确落点，而不是只停留在文档层：

- 产品档案：
  - `regulation-service` 已新增产品主数据实体、服务与接口
  - 企业端与监管端已接入产品档案入口
- 抽检最小闭环：
  - `regulation-operation-service` 已新增抽检任务、结果、公示和联动逻辑
  - 公众端已新增抽检结果列表与详情页面
- 监管概览：
  - `query-service` 已新增监管概览聚合接口
  - 监管管理员与执法人员工作台都已接入概览面板

---

## 3. 本轮验收方式

由于本轮没有拉起完整的线上联调环境，本次 P2-6 验收采用三层方式完成：

1. 文档与代码对照复核  
   确认 P2-6 所需的产品档案、抽检闭环、监管概览的前后端入口与接口已真实存在。

2. 后端联合测试  
   按 P2-6 清单执行多模块 Maven 测试，验证核心链路没有被本轮已有代码状态破坏。

3. 前端生产构建  
   执行 `food-web` 的生产构建，确认新增页面和组件能够通过打包。

说明：
- 本轮没有把 P2-5 配置治理纳入验收项。
- 本轮也没有进行“浏览器真实登录 + 全服务在线点点点”的录屏式人工验收，因此人工验收路径的结论以代码闭环和测试资产为主。

---

## 4. 实际执行命令与结果

### 4.1 后端联合测试

执行命令：

```powershell
mvn -q -pl regulation-service,regulation-operation-service,complaint-service,warning-service,query-service -am test
```

结果：

- 通过

说明：

- `regulation-service`、`regulation-operation-service`、`complaint-service`、`warning-service`、`query-service` 均参与本轮测试链路。
- 测试结果说明 P2-1 到 P2-4 涉及的核心后端实现当前处于可编译、可启动测试上下文、可完成关键断言的状态。

### 4.2 前端构建验证

执行命令：

```powershell
cd food-web
npm run build
```

结果：

- 通过

说明：

- Vite 生产构建成功，说明新增的产品档案、公众抽检页、监管概览面板等代码没有破坏前端构建链路。

---

## 5. 自动化测试与代码证据复核

### 5.1 产品档案

代码证据：

- 企业产品接口：`regulation-service/src/main/java/com/mortal/regulation/controller/ProductController.java`
- 内部产品接口：`regulation-service/src/main/java/com/mortal/regulation/controller/internal/InternalProductController.java`
- 企业端产品档案视图：`food-web/src/views/EnterpriseProfileView.vue`

自动化测试：

- `regulation-service/src/test/java/com/mortal/regulation/controller/ProductControllerTest.java`
- `regulation-service/src/test/java/com/mortal/regulation/controller/InternalProductControllerTest.java`

结论：

- 产品档案已形成“企业维护 + 监管查看 + 执行域内部读取”的最小闭环。

### 5.2 抽检任务与结果闭环

代码证据：

- 抽检任务接口：`regulation-operation-service/src/main/java/com/mortal/regulation/operation/controller/SamplingTaskController.java`
- 公众抽检结果接口：`regulation-operation-service/src/main/java/com/mortal/regulation/operation/controller/PublicSamplingResultController.java`
- 任务与结果核心服务：`regulation-operation-service/src/main/java/com/mortal/regulation/operation/service/impl/SamplingTaskServiceImpl.java`
- 监管端页面：`food-web/src/views/RegulatorAdminView.vue`、`food-web/src/views/RegulatorEnforcerView.vue`
- 公众端页面：`food-web/src/views/PublicSamplingResultListView.vue`、`food-web/src/views/PublicSamplingResultDetailView.vue`

自动化测试：

- `regulation-operation-service/src/test/java/com/mortal/regulation/operation/service/SamplingTaskServiceImplTest.java`

结论：

- 抽检链路已不再只是建表或空壳接口，而是覆盖了创建、派发、结果录入、失败联动与公众公示入口。

### 5.3 风险联动

代码证据：

- 抽检失败联动逻辑位于：`regulation-operation-service/src/main/java/com/mortal/regulation/operation/service/impl/SamplingTaskServiceImpl.java`
- warning outbox 投递位于：`regulation-operation-service/src/main/java/com/mortal/regulation/operation/service/impl/WarningEventOutboxServiceImpl.java`
- 企业重点监管原因枚举与服务位于：
  - `regulation-service/src/main/java/com/mortal/regulation/common/enums/EnterpriseKeyReasonType.java`
  - `regulation-service/src/main/java/com/mortal/regulation/service/impl/EnterpriseKeyReasonServiceImpl.java`

自动化测试：

- `regulation-operation-service/src/test/java/com/mortal/regulation/operation/service/SamplingTaskServiceImplTest.java`
- `complaint-service/src/test/java/com/mortal/complaint/application/ComplaintCommandServiceTest.java`

结论：

- 风险联动已经不只来自投诉和检查，抽检 `FAIL` 结果也已经进入重点监管与预警链路。

### 5.4 监管概览统计

代码证据：

- 概览聚合接口：`query-service/src/main/java/com/mortal/query/controller/SupervisionOverviewController.java`
- 概览聚合服务：`query-service/src/main/java/com/mortal/query/service/impl/SupervisionOverviewQueryServiceImpl.java`
- 前端概览面板：`food-web/src/components/SupervisionOverviewPanel.vue`
- 监管管理员/执法页挂载位置：
  - `food-web/src/views/RegulatorAdminView.vue`
  - `food-web/src/views/RegulatorEnforcerView.vue`

自动化测试：

- `query-service/src/test/java/com/mortal/query/controller/SupervisionOverviewControllerIntegrationTest.java`

结论：

- `query-service` 已从“只有预警统计”扩展为“企业、检查、抽检、投诉、预警”的轻量监管概览聚合层，但没有扩成通用 BI 平台。

---

## 6. 对照 P2-6 五条人工验收路径的判断

### 6.1 路径 1：产品档案

判断：

- 代码、接口与测试资产均已具备
- 可以支持企业维护产品、监管端查看产品列表

结论：

- 通过静态复核与自动化测试验收

### 6.2 路径 2：抽检任务

判断：

- 抽检任务创建、派发、监管端列表和执法端列表接口已存在
- 前端监管端页面也已接入对应能力

结论：

- 通过静态复核与自动化测试验收

### 6.3 路径 3：抽检结果与公示

判断：

- 抽检结果录入接口、公众列表页、公众详情页都已存在
- 前后端构建链路正常

结论：

- 通过静态复核与自动化测试验收

### 6.4 路径 4：风险联动

判断：

- 抽检失败已进入 warning outbox 和重点监管联动逻辑
- 这条链路已有服务层测试覆盖

结论：

- 通过静态复核与自动化测试验收

### 6.5 路径 5：监管概览统计

判断：

- 概览接口、聚合客户端、前端概览面板和监管端挂载位置都已存在
- 联合集成测试已覆盖聚合结果

结论：

- 通过静态复核与自动化测试验收

---

## 7. 本轮未纳入通过范围的内容

以下事项必须单独说明，避免误判：

1. P2-5 配置治理未执行  
   当前系按明确要求跳过 P2-5，因此默认 secret、内部 token、MinIO 默认凭据相关治理不计入本轮通过范围。

2. 未做完整在线人工点选验收  
   本轮未拉起完整的 Nacos、网关、数据库、MinIO 和全部服务进程进行浏览器级联调录屏，因此本报告的人工路径结论仍以“代码链路 + 自动化测试 + 构建验证”为主。

3. 工作区仍存在历史整改改动  
   当前仓库并非干净工作区，后续若继续推进 P2 之后的整改，应继续以现有业务代码现实为准，避免误把历史中间态文件当作本轮新增问题。

---

## 8. 总体验收结论

本轮 P2-6 结论如下：

- 后端联合测试通过
- 前端生产构建通过
- P2-1 产品档案、P2-2 抽检任务、P2-3 抽检结果与风险联动、P2-4 监管概览统计均已具备验收证据
- 按“跳过 P2-5”的前提，本轮可以判定为：
  - P2 业务增强主线验收通过
  - P2 全量完整验收未完成

也就是说，当前项目已经可以较稳定地讲清以下主线：

- 企业主体纳管
- 检查整改与投诉协同
- 抽检结果与风险联动
- 公众公示与监管概览

但如果你后续还需要把 P2 讲成“完整完成态”，则仍需单独决定是否恢复并完成 P2-5 配置治理。

---

## 9. 最新范围决策附录

日期：2026-04-07

基于当前交付取舍，后续范围做如下调整：

1. P2-5 配置治理不再推进  
   默认 secret、内部 token、MinIO 默认凭据相关治理继续保留为已知风险，不纳入后续开发范围。

2. 原纠偏方案中的 P3 整体放弃  
   通用操作日志中心、复杂角色调整、区域交接、检测机构独立角色、追溯/召回等低优先级或扩展项不再进入本项目整改范围。

3. P2 结论口径调整为“业务增强主线通过”  
   项目可以按“企业主体纳管、检查整改与投诉协同、抽检结果与风险联动、公众公示与监管概览”四条主线组织交付和答辩说明，但不再表述为“原始 P2 清单全量完成”。

4. 后续只保留交付收口工作  
   后续整改项以文档、演示脚本、演示数据、手工联调记录和阻断性 bugfix 为主，不再新增业务功能线。

详见：`docs/post-p2-follow-up-rectification-items.md`。
