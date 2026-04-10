# STITCH_INPUT.md

## 1. 给 Google Stitch 的任务说明

请基于本项目当前后端业务逻辑、接口契约、角色权限和状态流程，重新设计一整套食品安全监管平台前端。

这不是对旧前端做视觉优化，也不是沿用旧前端的布局、配色、组件或交互。旧前端只可作为业务参考，用来理解已有角色、页面入口、接口和流程。新的设计应从业务目标和接口能力出发，重新组织信息架构、页面结构、工作台体验、任务流转和视觉系统。

目标产出是一套可落地的、角色驱动的 Web 前端设计，覆盖公众端、企业端、系统管理员端、监管管理员端、执法人员端。界面语言以中文为主，产品气质应偏“政务监管 + 企业服务 + 数据看板”，要求清晰、可信、效率高，避免营销站式首页。

所有无法从当前项目代码可靠确认的信息，请统一标记为“待确认”，不要编造。

## 2. 项目概述

项目名称可使用：食品安全监管平台。

系统目标：

- 面向公众：查看监管公告、企业公示、抽检结果，提交食品安全投诉，并追踪投诉处理进度。
- 面向企业：完成企业备案，维护产品档案，查看检查记录，处理和提交整改任务。
- 面向监管机构：维护监管人员，审批企业备案，管理企业和产品，创建和派发检查/抽检任务，处理投诉，复核整改，发布公告，查看预警和统计看板。
- 面向执法人员：执行被派发的检查/抽检任务，处理投诉，跟进整改，处理风险预警。

后端是 Spring Cloud 微服务架构，当前主要服务包括：

| 服务 | 业务职责 |
|---|---|
| `gateway-service` | API 网关、JWT 鉴权、角色访问规则、请求头透传 |
| `user-service` | 登录、注册、用户、角色 |
| `regulation-service` | 企业、产品、监管人员、行政区划、公告、文件上传、预警代理 |
| `regulation-operation-service` | 检查任务、抽检任务、整改任务、检查记录 |
| `complaint-service` | 公众投诉、监管处理、投诉统计 |
| `query-service` | 监管看板、预警统计、聚合查询 |
| `warning-service` | 预警记录、预警处理日志、内部预警事件和统计 |

旧前端位置为 `food-web/`，技术栈为 Vue 3 + Vite。旧前端仅用于识别业务和接口，不作为新设计的视觉或交互基础。

## 3. 角色与权限

### 3.1 用户类型与角色

| 角色 | 代码值 | 核心目标 | 典型数据范围 |
|---|---|---|---|
| 系统管理员 | `ADMIN` | 创建和维护监管人员账号、监管档案、辖区信息 | 全局管理视角，具体统计范围待确认 |
| 公众用户 | `PUBLIC` | 查看公开信息，提交投诉，追踪本人投诉 | 公开数据 + 本人投诉 |
| 企业用户 | `ENTERPRISE` | 企业备案、产品档案、整改提交、检查记录查看 | 本企业数据 |
| 监管管理员 | `REGULATOR_ADMIN` | 企业审批、任务派发、投诉流转、整改复核、预警处置、公告发布、统计看板 | 绑定辖区及下级辖区 |
| 执法人员 | `REGULATOR_ENFORCER` | 执行检查/抽检任务，处理投诉，处理本人相关预警，查看检查记录 | 分派给本人或本人监管范围内的数据 |

用户类型线索：`ADMIN`, `REGULATOR`, `ENTERPRISE`, `PUBLIC`。

角色编码线索：`ADMIN`, `REGULATOR_ADMIN`, `REGULATOR_ENFORCER`, `ENTERPRISE`, `PUBLIC`。

用户状态线索：`ENABLED = 1`, `DISABLED = 0`。

监管人员状态线索：`1 = 在岗`, `0 = 停用`。

### 3.2 权限设计要求

新前端需要基于角色显示不同导航和工作台，不要让用户看到明显无权限的主操作。

| 角色 | 应展示的主要入口 |
|---|---|
| `PUBLIC` | 公众首页、监管公告、企业公示、抽检结果、我要投诉、我的投诉 |
| `ENTERPRISE` | 企业备案、产品档案、检查记录、整改任务 |
| `ADMIN` | 监管人员创建、监管人员列表、监管人员状态维护、区域选择 |
| `REGULATOR_ADMIN` | 监管概览、企业管理、备案审核、检查任务、抽检任务、投诉流转、检查记录、整改复核、风险预警、公告管理 |
| `REGULATOR_ENFORCER` | 监管概览、企业监管、我的检查任务、我的抽检任务、检查记录、整改跟进、投诉处理、风险预警 |

权限线索：

- 网关要求受保护接口携带 `Authorization: Bearer <token>`。
- 网关通过后端注入 `X-User-Id`, `X-Username`, `X-User-Type`, `X-User-Roles`。
- 预警服务 `/api/warning/**` 不允许前端直连。前端应走 `/api/regulation/warnings/**` 代理。
- 文件上传 `/api/files/**` 只允许 `PUBLIC` 和 `ENTERPRISE` 访问；后端进一步限制 `PUBLIC` 只能上传 `COMPLAINT`，`ENTERPRISE` 只能上传 `RECTIFICATION`。
- `query-service` 对统计查询应用数据范围：监管管理员按辖区及下级辖区，执法人员按自身 `ownerRegulatorId`。

## 4. 模块与信息架构

建议将新前端组织为 5 个角色工作区 + 共享详情能力。

### 4.1 公众服务门户

核心目标：让公众快速查看公开监管信息，提交投诉，并跟踪投诉。

页面/功能：

- 公众首页
- 监管公告列表与详情
- 企业公示列表与详情
- 抽检结果列表与详情
- 投诉提交
- 投诉追踪

### 4.2 企业工作台

核心目标：让企业完成备案、维护产品、响应监管整改。

页面/功能：

- 企业概览
- 企业备案表单与审核状态
- 产品档案管理
- 检查记录列表与详情
- 整改任务列表
- 整改详情与提交整改凭证

### 4.3 系统管理员控制台

核心目标：管理监管人员账号和监管档案。

页面/功能：

- 创建监管人员账号
- 创建/维护监管人员档案
- 监管人员列表
- 角色与辖区选择
- 在岗/停用状态切换

### 4.4 监管管理员工作台

核心目标：对辖区内企业和监管业务进行管理、派发、审核、统计。

页面/功能：

- 监管概览
- 企业管理
- 企业备案审核
- 检查任务派发
- 抽检任务派发与结果发布
- 投诉受理与派发
- 检查记录
- 整改复核
- 风险预警
- 公告管理

### 4.5 执法人员工作台

核心目标：聚焦本人待办和执行结果提交。

页面/功能：

- 监管概览
- 企业监管列表
- 我的检查任务
- 我的抽检任务
- 检查记录
- 整改跟进
- 投诉处理
- 风险预警

### 4.6 共享详情与弹层

建议作为全局可复用模式：

- 企业详情页或详情抽屉
- 投诉详情页
- 检查任务详情
- 抽检任务/结果详情
- 整改详情弹层/详情页
- 预警详情抽屉
- 操作日志时间线
- 状态流转时间线
- 附件预览

## 5. 页面清单

| 页面 | 适用角色 | 页面目标 | 关键数据 |
|---|---|---|---|
| 登录/注册页 | 未登录用户 | 登录，公众/企业注册，按角色进入工作区 | 用户名、密码、角色、token |
| 公众首页 | `PUBLIC` | 聚合公开服务入口 | 公告、企业公示、抽检结果、投诉入口 |
| 监管公告列表 | `PUBLIC` | 浏览已发布公告 | 标题、发布时间、状态 |
| 监管公告详情 | `PUBLIC` | 查看公告正文 | 标题、正文、发布时间 |
| 企业公示列表 | `PUBLIC` | 搜索已公示企业 | 企业名称、地址、状态、区域 |
| 企业公示详情 | `PUBLIC` | 查看企业公开信息 | 企业基础信息、许可/备案信息，字段以接口返回为准 |
| 抽检结果列表 | `PUBLIC` | 查询已发布抽检结果 | 企业、产品、结果、发布时间 |
| 抽检结果详情 | `PUBLIC` | 查看抽检明细 | 企业、产品、结果、说明 |
| 投诉提交页 | `PUBLIC` | 提交投诉和图片附件 | 企业、问题描述、是否匿名、附件 |
| 投诉追踪页 | `PUBLIC` | 查看本人投诉列表和进度 | 投诉状态、处理进度、反馈 |
| 系统管理员工作台 | `ADMIN` | 创建和管理监管人员 | 账号、角色、辖区、状态 |
| 企业工作台 | `ENTERPRISE` | 管理备案、产品、整改、检查记录 | 企业资料、产品、整改、检查记录 |
| 企业备案页 | `ENTERPRISE` | 提交/查看备案资料 | 企业名称、地址、联系人、区域、审核状态 |
| 产品档案页 | `ENTERPRISE` | 新增/编辑产品 | 产品名称、类别、规格、状态 |
| 企业检查记录页 | `ENTERPRISE` | 查看本企业检查记录 | 检查日期、结果、检查项 |
| 企业整改任务页 | `ENTERPRISE` | 提交整改材料 | 整改状态、说明、附件、审核意见 |
| 监管管理员首页 | `REGULATOR_ADMIN` | 查看辖区监管总览和待办 | 企业、检查、抽检、投诉、预警统计 |
| 企业管理页 | `REGULATOR_ADMIN` | 查询和查看辖区企业 | 企业名称、审核状态、监管状态 |
| 企业备案审核页 | `REGULATOR_ADMIN` | 审核待审批企业 | 审核通过、驳回、批量操作 |
| 检查任务页 | `REGULATOR_ADMIN` | 创建、派发、关闭检查任务 | 企业、执法员、任务状态、截止时间 |
| 抽检任务页 | `REGULATOR_ADMIN` | 创建、派发、发布抽检结果 | 企业、产品、结果、发布状态 |
| 投诉流转页 | `REGULATOR_ADMIN` | 受理、派发、驳回投诉 | 投诉状态、企业、执法员、处理记录 |
| 整改复核页 | `REGULATOR_ADMIN` | 复核企业整改提交 | 整改状态、附件、审核动作 |
| 风险预警页 | `REGULATOR_ADMIN` | 查看和处置辖区预警 | 等级、状态、业务类型、处置动作 |
| 公告管理页 | `REGULATOR_ADMIN` | 创建、编辑、发布、下线公告 | 标题、正文、状态 |
| 执法人员首页 | `REGULATOR_ENFORCER` | 查看本人任务和待处理事项 | 我的任务、投诉、预警、整改 |
| 我的检查任务页 | `REGULATOR_ENFORCER` | 开始检查并提交结果 | 检查项、结果、备注 |
| 我的抽检任务页 | `REGULATOR_ENFORCER` | 提交抽检结果 | 企业、产品、结果、说明 |
| 执法投诉处理页 | `REGULATOR_ENFORCER` | 处理派发给本人的投诉 | 开始处理、提交反馈 |
| 执法风险预警页 | `REGULATOR_ENFORCER` | 处理本人相关预警 | 处理、解决 |
| 企业详情页 | 监管端 | 查看企业完整资料和产品 | 企业、产品、区域路径 |
| 投诉详情页 | 监管端 | 查看投诉详情和流转操作 | 状态、处理人、处理意见 |
| 整改详情页/弹层 | 企业端/监管端 | 查看整改详情和操作日志 | 整改说明、附件、日志 |

待确认：旧前端未使用独立 `vue-router`，页面由 `App.vue` 的 `view` 状态切换。新前端是否设计为正式 URL 路由待确认，但建议 Stitch 以正式页面结构和角色导航来设计。

## 6. 关键页面说明

### 6.1 登录/注册页

需要支持：

- 登录入口。
- 公众注册。
- 企业注册。
- 登录后根据角色进入不同工作区。
- 当账号属于监管用户时，需要区分 `REGULATOR_ADMIN` 和 `REGULATOR_ENFORCER`。

设计重点：

- 不要把登录页设计成营销页。
- 注册和登录应清晰区分公众与企业。
- 登录成功后的角色分流应有明确的工作区入口反馈。

### 6.2 公众首页与公开信息页

公众首页应是服务入口页，突出：

- 监管公告。
- 企业公示。
- 抽检结果。
- 我要投诉。
- 我的投诉。

公开信息列表页需要具备：

- 搜索。
- 分页。
- 状态标签。
- 详情跳转。
- 空状态和加载状态。

### 6.3 投诉提交页

投诉提交是公众端关键页面。

需要支持：

- 搜索或选择被投诉企业。
- 填写投诉内容。
- 可匿名投诉。
- 上传图片附件。
- 提交后给出追踪入口。

上传约束：

- 先调用 `POST /api/files/presign` 获取预签名地址。
- 再将文件 `PUT` 到 `uploadUrl`。
- 公众投诉上传的 `bizType` 为 `COMPLAINT`。
- 允许文件类型：`image/jpeg`, `image/png`, `image/webp`。
- 单文件最大 `5MB`。

### 6.4 企业工作台

企业工作台应围绕“备案是否通过”和“是否有待整改”组织信息。

建议首屏展示：

- 企业备案审核状态。
- 产品档案数量。
- 待整改任务数量。
- 最近检查记录。
- 主要行动按钮：完善备案、维护产品、查看整改。

关键设计要求：

- 企业备案为 `PENDING`、`APPROVED`、`REJECTED` 时要有清晰状态提示。
- 产品管理应以表格或列表形式支持新增、编辑、启停状态展示。
- 整改任务应突出截止时间、整改状态、审核意见和附件。

### 6.5 系统管理员工作台

系统管理员端重点是监管人员创建与维护。

需要支持：

- 创建监管人员账号。
- 设置监管人员角色：`REGULATOR_ADMIN` 或 `REGULATOR_ENFORCER`。
- 选择行政区划。
- 查看监管人员列表。
- 启用/停用监管人员。

区域选择设计：

- 行政区划是树结构，接口为 `GET /api/regulation/regions?parentId={id}`。
- 区域路径可通过 `GET /api/regulation/regions/{id}/path` 展示。
- 区域层级线索为 `1 省 / 2 市 / 3 区县 / 4 街道`。

### 6.6 监管管理员工作台

监管管理员端是最高复杂度工作区，应以“待办 + 辖区监管概览 + 业务模块导航”组织。

建议首屏展示：

- 监管概览卡片：企业、检查、抽检、投诉、待处理预警。
- 待办队列：待审批企业、待派发投诉、待复核整改、待处理预警。
- 最近风险预警。
- 快捷操作：创建检查任务、创建抽检任务、发布公告。

模块设计重点：

- 企业管理：筛选企业名称、监管状态、审核状态，支持查看详情。
- 备案审核：支持单个通过/驳回，支持批量通过/驳回。
- 检查任务：支持创建、派发、查看状态、关闭任务。
- 抽检任务：支持创建、派发、结果发布/下线。
- 投诉流转：支持受理、派发、驳回，查看流转状态。
- 整改复核：支持确认通过或打回重做。
- 风险预警：支持分派、处理、解决，必要时跳转到投诉/整改/企业详情。
- 公告管理：支持草稿、发布、下线。
- 数据统计：展示监管概览与预警统计。

### 6.7 执法人员工作台

执法人员端应聚焦“我的任务”和“我的待处理事项”，避免复杂管理操作。

建议首屏展示：

- 今日/近期任务。
- 我的检查任务。
- 我的抽检任务。
- 待处理投诉。
- 待处理预警。
- 整改跟进。

关键设计要求：

- 检查任务需要突出“开始检查”和“提交结果”。
- 抽检任务需要突出“提交抽检结果”。
- 投诉详情需要突出“开始处理”和“提交处理反馈”。
- 预警处理需要突出“处理”和“解决”两个动作。

### 6.8 详情页与流程时间线

以下页面建议使用右侧抽屉、详情页或弹层，保持后台工作台的上下文不丢失：

- 企业详情。
- 投诉详情。
- 整改详情。
- 检查记录详情。
- 抽检结果详情。
- 预警详情。

详情页应包含：

- 基础信息。
- 当前状态。
- 关键操作。
- 关联对象。
- 附件。
- 操作日志/时间线。
- 错误、空状态和加载状态。

## 7. 业务流程与状态流转

### 7.1 登录与分流

- 登录接口：`POST /api/auth/login`。
- 登录后根据 `roles` 优先分流：`ADMIN`, `PUBLIC`, `REGULATOR_ADMIN`, `REGULATOR_ENFORCER`, `ENTERPRISE`。
- 如果 `roles` 缺失，旧前端会按 `userType` 兜底，监管用户会再查询监管档案识别 `roleType`。

### 7.2 企业备案与产品档案

状态：

- 企业审核状态：`PENDING`, `APPROVED`, `REJECTED`。
- 企业监管状态：`NORMAL`, `KEY`。
- 产品状态：`ACTIVE`, `INACTIVE`。

流程：

1. 企业提交备案资料。
2. 备案进入 `PENDING`。
3. 监管管理员审批为 `APPROVED` 或驳回为 `REJECTED`。
4. 企业备案通过后维护产品档案。
5. 监管端可查看企业和产品详情。

### 7.3 检查任务与检查记录

检查任务状态：

- `CREATED`
- `ASSIGNED`
- `IN_PROGRESS`
- `COMPLETED`
- `CLOSED`

流程：

1. 监管管理员创建检查任务。
2. 监管管理员指派执法人员，任务进入 `ASSIGNED`。
3. 执法人员开始检查，任务进入 `IN_PROGRESS`。
4. 执法人员提交检查结果，任务进入 `COMPLETED`。
5. 监管管理员关闭归档，任务进入 `CLOSED`。
6. 如果检查结果或检查项为 `FAIL`，会触发整改任务线索。

### 7.4 抽检任务与结果公示

抽检任务状态：

- `CREATED`
- `ASSIGNED`
- `COMPLETED`
- `CLOSED`

抽检结果：

- 检测结果：`PASS`, `FAIL`。
- 公示状态：`DRAFT`, `PUBLISHED`, `OFFLINE`。

流程：

1. 监管管理员创建抽检任务。
2. 监管管理员指派执法人员。
3. 执法人员提交抽检结果。
4. 监管管理员发布或下线抽检结果。
5. 公众端只能看到已发布的抽检结果。
6. 抽检不合格可能触发重点监管和预警线索。

### 7.5 整改任务

整改状态：

- `ONGOING`
- `SUBMITTED`
- `REWORK`
- `CONFIRMED`

审核动作：

- `CONFIRM`
- `REWORK`

流程：

1. 检查不合格后生成整改任务，初始状态为 `ONGOING`。
2. 企业在 `ONGOING` 或 `REWORK` 状态提交整改说明和附件。
3. 提交后进入 `SUBMITTED`。
4. 监管管理员复核：确认通过进入 `CONFIRMED`；打回重做进入 `REWORK`。
5. 打回时应填写审核意见。

SLA 线索：

- 企业首次提交期限：72 小时。
- 监管复核期限：24 小时。
- 打回后企业重新提交期限：48 小时。

整改日志动作线索：

- `SYSTEM_CREATE`
- `ENTERPRISE_SUBMIT`
- `REVIEW_CONFIRM`
- `REVIEW_REWORK`
- `SLA_OVERDUE_SUBMIT`
- `SLA_OVERDUE_REVIEW`

### 7.6 投诉流程

投诉状态：

- `SUBMITTED`
- `PENDING`
- `ASSIGNED`
- `PROCESSING`
- `FEEDBACKED`
- `REJECTED`

状态流转线索：

- `SUBMITTED -> PENDING / REJECTED`
- `PENDING -> ASSIGNED / REJECTED`
- `ASSIGNED -> ASSIGNED / PROCESSING / REJECTED`
- `PROCESSING -> FEEDBACKED / ASSIGNED`
- `FEEDBACKED` 和 `REJECTED` 为终态

流程：

1. 公众提交投诉，初始状态为 `SUBMITTED`。
2. 监管管理员受理后进入 `PENDING`。
3. 监管管理员可派发给执法人员，进入 `ASSIGNED`。
4. 执法人员开始处理，进入 `PROCESSING`。
5. 执法人员提交处理反馈，进入 `FEEDBACKED`。
6. 监管管理员可在适当阶段驳回，进入 `REJECTED`。
7. 投诉达到配置阈值会触发企业重点监管线索 `COMPLAINT_OVERFLOW`。

### 7.7 风险预警

预警状态：

- `OPEN`
- `PROCESSING`
- `RESOLVED`
- `CLOSED`

预警等级：

- `L1`
- `L2`

预警动作：

- `EVENT_UPSERT`
- `ASSIGN`
- `PROCESS`
- `RESOLVE`
- `AUTO_LEVEL_UP`
- `AUTO_ARCHIVE`

流程：

1. 后端内部事件创建或更新预警。
2. 监管管理员按辖区范围查看预警。
3. 监管管理员可分派、处理、解决预警。
4. 执法人员按 `ownerRegulatorId` 查看本人相关预警。
5. 执法人员可处理、解决本人预警。
6. 自动升级和自动归档规则待确认。

### 7.8 公告流程

公告状态：

- `DRAFT`
- `PUBLISHED`
- `OFFLINE`

流程：

1. 监管管理员创建公告草稿。
2. 监管管理员编辑公告。
3. 监管管理员发布公告，公众端可见。
4. 监管管理员下线公告，公众端不再展示。

公告是否支持分类、置顶、附件：待确认。

### 7.9 重点监管企业

企业监管状态：

- `NORMAL`
- `KEY`

重点监管原因：

- `WARNING_TRIGGERED`
- `COMPLAINT_OVERFLOW`
- `CONSECUTIVE_FAIL`
- `SAMPLING_FAIL`
- `MANUAL_SET`

触发线索：

- 投诉超限。
- 连续检查不合格。
- 抽检不合格。
- 预警触发。
- 人工设置。

预警触发重点企业的完整实现细节：待确认。

## 8. 接口契约与联调要求

### 8.1 全局 API 规则

| 项 | 规则 |
|---|---|
| 网关默认地址 | `http://localhost:8080` |
| 前端基础地址 | `VITE_API_BASE || http://localhost:8080` |
| 认证头 | `Authorization: Bearer <token>` |
| 常规请求体 | JSON |
| 统一成功返回 | `{ code: 0, message: "ok", data, timestamp }` |
| 统一失败返回 | `{ code, message, timestamp }` |
| 网关 401/403 返回 | `{ code, message, traceId, timestamp }` |
| 分页结构 | `records`, `total`, `page`, `size`, `pages` |
| 链路追踪 | `X-Trace-Id` |
| 服务间内部头 | `X-Internal-Token`，前端不要使用 |

错误码线索：

| code | 含义 |
|---|---|
| `0` | 成功 |
| `400` | 参数错误、校验失败、非法状态流转 |
| `401` | 未认证、token 无效、身份缺失 |
| `403` | 无权限、数据范围越权、文件业务类型越权 |
| `404` | 存在线索，但跨服务使用情况待确认 |
| `500` | 服务端异常 |

### 8.2 网关路由

| 前缀 | 服务 | 用途 |
|---|---|---|
| `/api/auth/**` | `user-service` | 登录、登出、验证 |
| `/api/users/**` | `user-service` | 用户注册、用户查询 |
| `/api/roles/**` | `user-service` | 角色 |
| `/api/admin/**` | `user-service` | 管理员用户操作 |
| `/api/regulation/**` | `regulation-service` | 监管基础资料、公告、预警代理 |
| `/api/files/**` | `regulation-service` | 文件上传预签名 |
| `/api/regulation-operation/**` | `regulation-operation-service` | 检查、抽检、整改 |
| `/api/complaints/**` | `complaint-service` | 投诉 |
| `/api/query/**` | `query-service` | 统计 |
| `/api/warning/**` | `warning-service` | 网关存在路由，但前端直连被禁止 |

### 8.3 核心业务接口

认证与用户：

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `POST /api/auth/verify`
- `POST /api/users/register/public`
- `POST /api/users/register/enterprise`
- `POST /api/admin/users/regulators`
- `GET /api/roles`
- `POST /api/roles/bind`
- `GET /api/roles/user/{userId}`

企业、产品、区域、监管人员：

- `GET /api/regulation/enterprise/profile`
- `POST /api/regulation/enterprise/profile`
- `GET /api/regulation/enterprise/pending`
- `PUT /api/regulation/enterprise/{id}/approve`
- `PUT /api/regulation/enterprise/{id}/reject`
- `PUT /api/regulation/enterprise/approve-batch`
- `PUT /api/regulation/enterprise/reject-batch`
- `GET /api/regulation/enterprises`
- `GET /api/regulation/enterprises/{id}`
- `GET /api/regulation/public/enterprises`
- `GET /api/regulation/public/enterprises/{id}`
- `GET /api/regulation/products/my`
- `POST /api/regulation/products`
- `PUT /api/regulation/products/{id}`
- `GET /api/regulation/enterprises/{enterpriseId}/products`
- `GET /api/regulation/regions?parentId={id}`
- `GET /api/regulation/regions/{id}/path`
- `POST /api/regulation/regulators`
- `GET /api/regulation/regulators`
- `GET /api/regulation/regulators/eligible`
- `GET /api/regulation/regulators/me`
- `PUT /api/regulation/regulators/{id}/status`

公告与文件：

- `GET /api/regulation/bulletins`
- `GET /api/regulation/bulletins/{id}`
- `POST /api/regulation/bulletins`
- `PUT /api/regulation/bulletins/{id}`
- `POST /api/regulation/bulletins/{id}/publish`
- `POST /api/regulation/bulletins/{id}/offline`
- `GET /api/regulation/public/bulletins`
- `GET /api/regulation/public/bulletins/{id}`
- `POST /api/files/presign`

检查、抽检、整改：

- `POST /api/regulation-operation/tasks`
- `PUT /api/regulation-operation/tasks/{id}/assign`
- `GET /api/regulation-operation/tasks`
- `GET /api/regulation-operation/tasks/my`
- `PUT /api/regulation-operation/tasks/{id}/start`
- `POST /api/regulation-operation/tasks/{id}/submit`
- `PUT /api/regulation-operation/tasks/{id}/close`
- `POST /api/regulation-operation/sampling/tasks`
- `PUT /api/regulation-operation/sampling/tasks/{id}/assign`
- `GET /api/regulation-operation/sampling/tasks`
- `GET /api/regulation-operation/sampling/tasks/my`
- `POST /api/regulation-operation/sampling/tasks/{id}/result`
- `PUT /api/regulation-operation/sampling/tasks/{id}/close`
- `POST /api/regulation-operation/sampling/results/{id}/publish`
- `POST /api/regulation-operation/sampling/results/{id}/offline`
- `GET /api/regulation-operation/public/sampling/results`
- `GET /api/regulation-operation/public/sampling/results/{id}`
- `GET /api/regulation-operation/rectifications/my`
- `PUT /api/regulation-operation/rectifications/my/{id}/submit`
- `GET /api/regulation-operation/rectifications`
- `GET /api/regulation-operation/rectifications/regulator/my`
- `GET /api/regulation-operation/rectifications/{id}`
- `GET /api/regulation-operation/rectifications/{id}/actions`
- `POST /api/regulation-operation/rectifications/{id}/review`
- `GET /api/regulation-operation/inspections/my`
- `GET /api/regulation-operation/inspections`
- `GET /api/regulation-operation/inspections/{id}`
- `GET /api/regulation-operation/inspections/enterprise`
- `GET /api/regulation-operation/inspections/enterprise/{id}`

投诉：

- `POST /api/complaints/public`
- `GET /api/complaints/my`
- `GET /api/complaints/my/{id}`
- `GET /api/complaints`
- `GET /api/complaints/{id}`
- `PUT /api/complaints/{id}/accept`
- `PUT /api/complaints/{id}/assign`
- `PUT /api/complaints/{id}/process`
- `POST /api/complaints/{id}/handle`
- `PUT /api/complaints/{id}/reject`

预警与统计：

- `GET /api/regulation/warnings`
- `GET /api/regulation/warnings/{id}`
- `POST /api/regulation/warnings/{id}/assign`
- `POST /api/regulation/warnings/{id}/process`
- `POST /api/regulation/warnings/{id}/resolve`
- `GET /api/regulation/warnings/my`
- `GET /api/regulation/warnings/my/{id}`
- `POST /api/regulation/warnings/my/{id}/process`
- `POST /api/regulation/warnings/my/{id}/resolve`
- `GET /api/query/warnings/overview`
- `GET /api/query/warnings/trend`
- `GET /api/query/warnings/types`
- `GET /api/query/warnings/efficiency`
- `GET /api/query/supervision/overview`

### 8.4 统计查询参数

`query-service` 已确认存在 OpenAPI。时间参数格式为 `yyyy-MM-dd'T'HH:mm:ss`，UTC+8 本地时间。

预警统计过滤字段：

- `startTime`
- `endTime`
- `warningType`
- `bizType`
- `level`
- `status`
- `regionId`
- `regionIds`
- `ownerRegulatorId`
- `topN`
- `trendDays`
- `overdueHours`

监管概览接口复用部分过滤字段：

- `regionId`
- `regionIds`
- `ownerRegulatorId`

### 8.5 上传接口

接口：`POST /api/files/presign`。

请求字段：

- `filename`
- `contentType`
- `size`
- `bizType`

响应字段：

- `uploadUrl`
- `fileUrl`
- `objectKey`

上传规则：

- 前端先调用预签名接口。
- 前端再直接向 `uploadUrl` 发起 `PUT` 上传。
- 最大文件大小：`5MB`。
- 允许类型：`image/jpeg`, `image/png`, `image/webp`。
- 预签名有效期：`600` 秒。
- `PUBLIC` 只能上传 `COMPLAINT`。
- `ENTERPRISE` 只能上传 `RECTIFICATION`。
- `INSPECTION` 枚举存在，但当前前端可用入口待确认。

### 8.6 不应直接用于前端的接口

- `/api/auth/introspect`：网关内部调用。
- `/api/warning/**`：网关禁止前端直连，应通过 `/api/regulation/warnings/**`。
- `/api/internal/**`：服务间内部接口。
- `/api/complaint/internal/stats/**`：路径与网关 `/api/complaints/**` 不一致，当前视为服务间统计接口，是否前端可用待确认。

## 9. 技术约束

- 新前端应以现有后端接口为准，避免设计需要后端新增能力的核心功能。
- 可以重新设计路由、导航、布局和组件，不需要继承旧前端的 `view` 状态切换模式。
- 旧前端只作为业务参考，不作为视觉和交互基础。
- 主要交互场景是桌面端监管后台和企业后台；公众端也应兼顾移动端可用性。
- 接口统一返回 `ApiResponse`，分页统一返回 `PageResult`。
- 受保护接口均需要 JWT。
- 角色与数据范围由后端强制控制，但前端需要根据角色隐藏明显无权限入口。
- 字典没有确认的集中接口，当前应基于枚举值和接口返回值设计状态标签。
- 导入、导出、独立文件下载接口未可靠确认，不要在核心设计中作为已支持能力。
- 其他服务 Swagger/OpenAPI 除 `query-service` 外待确认。

## 10. 给 Stitch 的最终设计指令

请为这个食品安全监管平台重新设计整套前端，优先输出一套角色完整、流程清晰、可联调落地的产品界面方案。

设计时请遵循：

- 不要基于旧前端做美化。旧前端只提供业务线索。
- 不要设计成营销落地页。首屏应直接服务对应角色的真实工作目标。
- 采用角色化信息架构：公众门户、企业工作台、系统管理员控制台、监管管理员工作台、执法人员工作台。
- 为不同角色设计独立导航和工作区，而不是把所有功能塞进同一个菜单。
- 监管管理员和执法人员的工作台要突出待办、状态、风险和快速操作。
- 企业端要突出备案状态、整改待办、产品档案和检查记录。
- 公众端要突出公开查询、投诉提交和投诉追踪。
- 使用清晰的状态标签和流程时间线展示 `PENDING`, `APPROVED`, `REJECTED`, `ASSIGNED`, `PROCESSING`, `SUBMITTED`, `REWORK`, `CONFIRMED`, `OPEN`, `RESOLVED` 等状态。
- 复杂业务详情建议使用详情页或抽屉，并保留上层列表上下文。
- 表格页应提供筛选、分页、空状态、加载状态、错误状态和批量操作入口。
- 表单页应提供校验提示、保存/提交反馈、提交后状态变化提示。
- 附件上传要体现“先获取预签名，再上传图片”的流程，但界面不需要暴露技术细节给普通用户。
- 不要展示前端无法确认或后端未确认的能力，如导入、导出、独立下载、公告分类/置顶/附件；如确需出现，请标记为“待确认”。
- 不要设计前端直连 `/api/warning/**` 或 `/api/internal/**` 的能力。
- 视觉风格应专业、可信、清爽，适合监管部门、企业和公众共同使用。

建议 Stitch 至少产出以下关键界面：

1. 登录/注册页。
2. 公众首页。
3. 公告列表与详情。
4. 企业公示列表与详情。
5. 抽检结果列表与详情。
6. 投诉提交页。
7. 投诉追踪页。
8. 企业工作台首页。
9. 企业备案页。
10. 产品档案页。
11. 企业整改任务页和整改详情。
12. 系统管理员监管人员管理页。
13. 监管管理员首页。
14. 企业管理与备案审核页。
15. 检查任务管理页。
16. 抽检任务与结果发布页。
17. 投诉流转页和投诉详情。
18. 整改复核页。
19. 风险预警页和预警详情。
20. 公告管理页。
21. 执法人员首页。
22. 我的检查任务页。
23. 我的抽检任务页。
24. 执法投诉处理页。
25. 执法风险预警页。

## 11. 待确认清单

- 是否为新前端建立正式 URL 路由表。
- 是否需要除 `query-service` 外的 Swagger/OpenAPI。
- 是否存在统一字典接口。
- 是否存在业务导入、导出、独立文件下载接口。
- `/api/complaint/internal/stats/**` 是否需要对前端暴露。
- 公告是否支持分类、置顶、附件。
- `INSPECTION` 类型文件上传是否面向监管端开放。
- 自动预警升级、自动归档和预警触发重点企业的完整产品规则。
- 系统管理员是否需要访问统计看板的真实产品定位。
