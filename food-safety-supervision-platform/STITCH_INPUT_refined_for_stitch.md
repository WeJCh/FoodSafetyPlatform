# STITCH 输入文档（精修版）：食品安全监管平台前端全新设计说明

## 0. 你的任务

请你为一个 **食品安全监管平台** 重新设计整套 Web 前端。

这次任务的目标不是优化旧前端，也不是沿用旧前端的布局、配色、组件或交互，而是：

- 基于现有后端业务能力、接口契约、角色权限和状态流程
- 重新定义整套前端的 **信息架构、页面结构、工作台模式、详情模式、流程视图和视觉系统**
- 设计结果必须 **可落地、可联调、可按角色使用**
- 旧前端只能作为业务线索参考，**不能作为视觉继承基础**

请始终优先保证：
1. 角色分工清晰
2. 流程状态清晰
3. 页面之间的跳转关系清晰
4. 列表、详情、表单、工作台之间的协同关系清晰
5. 设计不要脱离现有后端能力

如遇到无法确认的信息，请统一标记为 **“待确认”**，不要自行补充不存在的业务能力。

---

## 1. 项目定位

项目名称可使用：**食品安全监管平台**

这是一个面向多角色使用的食品安全监管系统，服务对象包括：

- 公众用户
- 企业用户
- 系统管理员
- 监管管理员
- 执法人员

系统同时承担三类目标：

### 1.1 公众服务
让公众可以：
- 查看监管公告
- 查询企业公示信息
- 查询抽检结果
- 提交食品安全投诉
- 跟踪投诉处理进度

### 1.2 企业服务
让企业可以：
- 提交企业备案
- 维护产品档案
- 查看检查记录
- 接收并处理整改任务
- 上传整改说明和附件

### 1.3 监管业务
让监管机构和执法人员可以：
- 管理企业和产品
- 审批企业备案
- 创建和派发检查任务、抽检任务
- 处理投诉
- 管理整改闭环
- 处理风险预警
- 发布公告
- 查看监管统计看板

---

## 2. 设计总原则

请按以下原则设计整套前端：

### 2.1 不是旧前端改版
- 不要做“旧后台换皮”
- 不要保留旧前端的页面组织逻辑、布局逻辑和视图切换逻辑
- 可以完全重做导航、布局、页面层级和工作区模式

### 2.2 角色驱动
必须采用 **角色化前端架构**，不要把所有功能塞进一个统一后台。

建议拆为 5 个工作区：

1. 公众服务门户
2. 企业工作台
3. 系统管理员控制台
4. 监管管理员工作台
5. 执法人员工作台

### 2.3 流程驱动
整个平台的设计应围绕以下业务闭环展开：

- 企业备案 → 审核 → 企业纳管
- 检查任务创建 → 派发 → 执行 → 提交 → 关闭
- 抽检任务创建 → 派发 → 结果提交 → 公示
- 投诉提交 → 受理 → 派发 → 处理 → 反馈/驳回
- 整改任务生成 → 企业整改 → 监管复核 → 通过/打回
- 预警生成 → 分派 → 处理 → 解决/关闭
- 公告创建 → 发布 → 下线

### 2.4 可联调落地
前端设计必须尽量映射到现有后端接口，不要依赖未确认的后端新能力作为核心流程前提。

---

## 3. 后端架构背景（用于理解系统边界）

后端采用 **Spring Cloud 微服务架构**，主要服务如下：

| 服务 | 职责 |
|---|---|
| `gateway-service` | API 网关、JWT 鉴权、角色访问规则、请求头透传 |
| `user-service` | 登录、注册、用户、角色 |
| `regulation-service` | 企业、产品、监管人员、行政区划、公告、文件上传、预警代理 |
| `regulation-operation-service` | 检查任务、抽检任务、整改任务、检查记录 |
| `complaint-service` | 投诉、投诉处理、投诉统计 |
| `query-service` | 统计看板、预警统计、聚合查询 |
| `warning-service` | 预警记录、预警处理日志、内部预警事件 |

旧前端位于 `food-web/`，技术栈为 Vue 3 + Vite。  
**旧前端只可用于理解已有业务，不可作为新设计的视觉和交互基础。**

---

## 4. 角色与前端工作区

## 4.1 系统管理员 `ADMIN`
### 核心目标
- 创建和维护监管人员账号
- 维护监管人员档案
- 分配角色与辖区
- 管理在岗/停用状态

### 建议工作区定位
这是一个偏“组织与权限配置”的后台，不需要复杂业务流程，但需要高效的表格、表单和区域树交互。

### 主要入口
- 监管人员列表
- 创建监管人员
- 角色绑定
- 辖区选择
- 状态管理

---

## 4.2 公众用户 `PUBLIC`
### 核心目标
- 快速访问公开信息
- 提交投诉
- 跟踪投诉进度

### 建议工作区定位
应设计成 **服务门户**，但不是营销官网。首页应直接呈现公众最常用的公开服务入口。

### 主要入口
- 公众首页
- 监管公告
- 企业公示
- 抽检结果
- 我要投诉
- 我的投诉

---

## 4.3 企业用户 `ENTERPRISE`
### 核心目标
- 提交企业备案
- 维护产品档案
- 查看检查记录
- 处理整改任务

### 建议工作区定位
围绕 **“备案状态 + 整改待办 + 产品维护”** 组织，不要做成庞杂的综合后台。

### 主要入口
- 企业工作台首页
- 企业备案
- 产品档案
- 检查记录
- 整改任务

---

## 4.4 监管管理员 `REGULATOR_ADMIN`
### 核心目标
- 管理辖区企业和监管业务
- 审核企业备案
- 派发检查、抽检、投诉
- 复核整改
- 处理预警
- 发布公告
- 查看统计看板

### 建议工作区定位
这是平台最复杂的工作区，应以 **待办 + 监管概览 + 业务模块导航** 组织。

### 主要入口
- 监管概览
- 企业管理
- 企业备案审核
- 检查任务
- 抽检任务
- 投诉流转
- 整改复核
- 风险预警
- 公告管理
- 统计分析

---

## 4.5 执法人员 `REGULATOR_ENFORCER`
### 核心目标
- 处理派发给本人的任务
- 提交检查和抽检结果
- 处理投诉
- 跟进整改
- 处理本人相关预警

### 建议工作区定位
应强调 **“我的任务”“我的待办”“我的处理结果”**，避免复杂管理型界面。

### 主要入口
- 执法首页
- 我的检查任务
- 我的抽检任务
- 投诉处理
- 整改跟进
- 风险预警
- 检查记录

---

## 5. 整体信息架构建议

建议 Stitch 按下列结构设计，而不是沿用旧前端菜单组织方式。

## 5.1 公众服务门户
- 公众首页
- 公告列表
- 公告详情
- 企业公示列表
- 企业公示详情
- 抽检结果列表
- 抽检结果详情
- 投诉提交
- 投诉追踪
- 投诉详情

## 5.2 企业工作台
- 企业首页
- 企业备案
- 备案状态详情
- 产品档案列表
- 产品新增/编辑
- 检查记录列表
- 检查记录详情
- 整改任务列表
- 整改详情
- 提交整改

## 5.3 系统管理员控制台
- 监管人员列表
- 新建监管人员
- 编辑监管人员
- 角色与辖区配置
- 在岗/停用状态管理

## 5.4 监管管理员工作台
- 首页/监管概览
- 企业管理
- 企业详情
- 企业备案审核
- 检查任务列表
- 新建检查任务
- 检查任务详情
- 抽检任务列表
- 新建抽检任务
- 抽检结果详情/发布
- 投诉流转列表
- 投诉详情
- 整改复核列表
- 整改详情
- 风险预警列表
- 风险预警详情
- 公告管理列表
- 公告编辑页
- 统计分析页

## 5.5 执法人员工作台
- 首页/我的待办
- 我的检查任务
- 检查任务详情
- 检查结果提交
- 我的抽检任务
- 抽检结果提交
- 投诉处理列表
- 投诉详情/处理
- 我的整改跟进
- 我的预警列表
- 预警详情/处理

---

## 6. 关键页面设计要求

以下页面是必须重点产出的。

## 6.1 登录/注册页
### 页面目标
支持：
- 登录
- 公众注册
- 企业注册
- 登录后按角色进入不同工作区

### 设计要求
- 不要设计成营销页
- 注册和登录要清晰分区
- 公众注册与企业注册要明确区分
- 登录成功后需要明确角色分流反馈
- 监管用户需要区分 `REGULATOR_ADMIN` 和 `REGULATOR_ENFORCER`

---

## 6.2 公众首页
### 页面目标
作为公众服务总入口，承载最核心公开服务。

### 页面重点
突出以下入口：
- 监管公告
- 企业公示
- 抽检结果
- 我要投诉
- 我的投诉

### 设计要求
- 首页应偏“服务导航 + 信息聚合”
- 不要出现复杂后台元素
- 查询入口应醒目
- 支持移动端可用

---

## 6.3 公众公开信息列表页
包括：
- 公告列表
- 企业公示列表
- 抽检结果列表

### 统一要求
- 提供搜索、筛选、分页
- 提供状态标签
- 支持查看详情
- 具备空状态、加载状态、异常状态
- 视觉风格应统一，形成标准公开信息页面模板

---

## 6.4 投诉提交页
### 页面目标
让公众便捷完成投诉提交。

### 页面内容
- 选择或搜索被投诉企业
- 填写投诉问题描述
- 是否匿名
- 上传图片附件
- 提交后给出投诉追踪入口

### 设计要求
- 表单逻辑清晰，减少公众理解成本
- 附件上传应做成普通用户可理解的上传体验
- 提交成功后需要展示“后续如何跟踪”的反馈页

### 上传规则（用于约束设计，不需要暴露技术术语）
- 先获取预签名地址，再上传文件
- 公众投诉上传的 `bizType = COMPLAINT`
- 支持图片类型：`image/jpeg`, `image/png`, `image/webp`
- 单文件最大 `5MB`

---

## 6.5 企业工作台首页
### 页面目标
让企业快速知道自己当前最重要的监管状态。

### 建议首屏内容
- 企业备案审核状态
- 产品档案数量
- 待整改任务数量
- 最近检查记录
- 快捷操作：完善备案、维护产品、查看整改

### 设计重点
围绕两件事组织：
1. 我的备案是否通过
2. 我是否有待整改事项

---

## 6.6 企业备案页
### 页面目标
企业提交和查看备案资料。

### 关键状态
- `PENDING`
- `APPROVED`
- `REJECTED`

### 设计要求
- 状态提示要明显
- 若被驳回，要能清楚展示驳回原因和下一步操作
- 若已通过，应提示企业继续维护产品档案

---

## 6.7 产品档案页
### 页面目标
企业维护产品档案。

### 页面能力
- 产品列表
- 新增产品
- 编辑产品
- 状态展示

### 关键字段
- 产品名称
- 类别
- 规格
- 状态

### 关键状态
- `ACTIVE`
- `INACTIVE`

---

## 6.8 整改任务页（企业端）
### 页面目标
企业查看整改任务并提交整改材料。

### 页面重点
- 截止时间
- 当前整改状态
- 整改说明
- 附件上传
- 审核意见
- 提交动作

### 关键状态
- `ONGOING`
- `SUBMITTED`
- `REWORK`
- `CONFIRMED`

### 设计要求
- 要突出“是否临近超期”
- 要区分“待提交”“已提交待审核”“被打回重做”“已确认通过”
- 详情页要有完整时间线和审核意见

---

## 6.9 系统管理员工作台
### 页面目标
高效管理监管人员。

### 页面重点
- 列表检索
- 新建监管人员
- 编辑角色
- 选择辖区
- 启停状态切换

### 特殊交互
区域选择应支持树结构和区域路径展示。

### 区域线索
- `1 = 省`
- `2 = 市`
- `3 = 区县`
- `4 = 街道`

---

## 6.10 监管管理员首页
### 页面目标
体现辖区监管全貌，并集中展示待办。

### 建议首屏内容
- 企业数量
- 检查任务情况
- 抽检任务情况
- 投诉情况
- 待处理预警
- 待审批企业
- 待派发投诉
- 待复核整改
- 最近预警
- 快捷操作

### 设计重点
这是最核心的业务首页，应体现：
- 总览
- 风险
- 待办
- 快捷入口
- 趋势与统计

---

## 6.11 企业管理页（监管端）
### 页面目标
查看和管理辖区企业。

### 页面内容
- 企业搜索
- 监管状态筛选
- 审核状态筛选
- 企业详情查看
- 重点监管标识

### 关键状态
- 企业审核状态：`PENDING` / `APPROVED` / `REJECTED`
- 企业监管状态：`NORMAL` / `KEY`

---

## 6.12 企业备案审核页
### 页面目标
审核企业备案申请。

### 页面能力
- 查看待审核企业
- 审核通过
- 驳回
- 批量通过
- 批量驳回

### 设计重点
- 审核动作需明确
- 驳回原因输入需清晰
- 批量操作要避免误操作

---

## 6.13 检查任务页
### 页面目标
创建、派发、跟踪、关闭检查任务。

### 关键状态
- `CREATED`
- `ASSIGNED`
- `IN_PROGRESS`
- `COMPLETED`
- `CLOSED`

### 页面能力
- 创建任务
- 指派执法人员
- 查看任务状态
- 查看执行结果
- 关闭任务

### 设计重点
- 任务状态要明显
- 详情中需包含检查结果和可能触发的整改线索
- 管理端和执法端看到的操作按钮应不同

---

## 6.14 抽检任务与结果发布页
### 页面目标
管理抽检任务并对结果进行发布。

### 关键状态
任务状态：
- `CREATED`
- `ASSIGNED`
- `COMPLETED`
- `CLOSED`

结果状态：
- `PASS`
- `FAIL`

公示状态：
- `DRAFT`
- `PUBLISHED`
- `OFFLINE`

### 页面能力
- 创建抽检任务
- 指派执法人员
- 查看抽检结果
- 发布/下线结果

### 设计重点
- 结果与公示状态要区分清楚
- 公众端只能看到 `PUBLISHED` 状态的结果
- 不合格结果应突出风险提示

---

## 6.15 投诉流转页
### 页面目标
完成投诉受理、派发、处理、反馈和驳回。

### 关键状态
- `SUBMITTED`
- `PENDING`
- `ASSIGNED`
- `PROCESSING`
- `FEEDBACKED`
- `REJECTED`

### 页面能力
- 受理
- 派发
- 驳回
- 开始处理
- 提交处理反馈
- 查看处理日志

### 设计重点
- 状态流转非常关键，应使用流程时间线或状态流转视图
- 公众看到的投诉详情应偏“进度追踪”
- 监管和执法看到的投诉详情应偏“操作处理”

---

## 6.16 整改复核页
### 页面目标
监管管理员对企业提交的整改进行复核。

### 审核动作
- `CONFIRM`
- `REWORK`

### 页面重点
- 查看企业整改说明
- 查看附件
- 查看整改时间线
- 确认通过
- 打回重做
- 填写审核意见

### SLA 线索
- 企业首次提交期限：72 小时
- 监管复核期限：24 小时
- 打回后再次提交期限：48 小时

### 设计建议
- 在列表和详情页中体现时限压力
- 超期风险可做弱提醒或标签提示

---

## 6.17 风险预警页
### 页面目标
展示和处理风险预警。

### 关键状态
- `OPEN`
- `PROCESSING`
- `RESOLVED`
- `CLOSED`

### 预警等级
- `L1`
- `L2`

### 页面能力
- 查看预警列表
- 查看预警详情
- 分派
- 处理
- 解决
- 查看关联对象（企业、投诉、整改等）

### 设计重点
- 预警等级要有明显层级
- 预警详情应具备关联信息和处理日志
- 管理员与执法人员看到的操作不同

---

## 6.18 公告管理页
### 页面目标
创建、编辑、发布、下线公告。

### 关键状态
- `DRAFT`
- `PUBLISHED`
- `OFFLINE`

### 页面能力
- 列表管理
- 新建公告
- 编辑公告
- 发布
- 下线

### 待确认项
- 是否支持分类
- 是否支持置顶
- 是否支持附件

如需设计，可作为“待确认能力”标注，不要默认当作已具备能力。

---

## 6.19 执法人员首页
### 页面目标
将本人近期最重要的任务集中呈现。

### 建议首屏内容
- 我的检查任务
- 我的抽检任务
- 待处理投诉
- 待处理预警
- 整改跟进

### 设计重点
- 强调“今日/近期待办”
- 让执法人员一进入就知道下一步该做什么

---

## 6.20 详情页与全局详情模式
建议以下对象统一采用“详情页 / 右侧抽屉 / 弹层”设计模式，并尽量保留列表上下文：

- 企业详情
- 投诉详情
- 检查任务详情
- 抽检结果详情
- 整改详情
- 预警详情

详情页建议统一包含：
- 基础信息
- 当前状态
- 关键操作
- 关联对象
- 附件
- 操作日志
- 流程时间线
- 异常状态 / 空状态 / 加载状态

---

## 7. 关键业务流程与前端表现方式

## 7.1 登录与角色分流
- 登录接口：`POST /api/auth/login`
- 登录成功后按角色进入对应工作区
- 角色优先顺序线索：`ADMIN`, `PUBLIC`, `REGULATOR_ADMIN`, `REGULATOR_ENFORCER`, `ENTERPRISE`

### 前端表现建议
- 登录成功提示 + 进入角色工作区
- 多角色场景如存在，可考虑角色选择器（待确认）

---

## 7.2 企业备案流程
1. 企业提交备案资料
2. 状态进入 `PENDING`
3. 监管管理员审核：
   - 通过 → `APPROVED`
   - 驳回 → `REJECTED`
4. 审核通过后企业继续维护产品档案

### 前端表现建议
- 用状态卡、步骤条或状态条表达备案进度
- 驳回时突出驳回原因和下一步动作

---

## 7.3 检查任务流程
1. 管理员创建任务
2. 派发执法人员 → `ASSIGNED`
3. 执法人员开始处理 → `IN_PROGRESS`
4. 提交结果 → `COMPLETED`
5. 管理员关闭 → `CLOSED`

### 前端表现建议
- 用状态标签 + 时间线表示
- 详情页中展示执行人、检查项、结果和后续整改线索

---

## 7.4 抽检任务流程
1. 创建抽检任务
2. 派发执法人员
3. 提交抽检结果
4. 管理员发布或下线抽检结果
5. 公众端查询已发布结果

### 前端表现建议
- 将“任务状态”和“结果公示状态”分开显示
- 对 `FAIL` 结果给出更强风险表达

---

## 7.5 整改流程
1. 检查不合格生成整改任务
2. 企业提交整改说明和附件
3. 状态进入 `SUBMITTED`
4. 管理员复核：
   - 通过 → `CONFIRMED`
   - 打回 → `REWORK`
5. 打回后企业重新提交

### 前端表现建议
- 要体现闭环感
- 用时间线展示系统创建、企业提交、监管复核、打回重做等动作
- 体现时限压力和超期提醒

---

## 7.6 投诉流程
1. 公众提交投诉 → `SUBMITTED`
2. 管理员受理 → `PENDING`
3. 管理员派发 → `ASSIGNED`
4. 执法人员处理 → `PROCESSING`
5. 执法人员反馈 → `FEEDBACKED`
6. 特定阶段可驳回 → `REJECTED`

### 前端表现建议
- 公众端：进度视图为主
- 监管/执法端：操作视图为主
- 投诉详情中同时体现状态、处理人、处理日志、反馈内容

---

## 7.7 风险预警流程
1. 系统创建或更新预警
2. 管理员查看辖区预警
3. 管理员分派/处理/解决
4. 执法人员处理本人相关预警

### 前端表现建议
- 列表中明确等级、状态、业务类型、归属人
- 详情中展示关联企业、关联投诉/整改、处理日志
- 等级和状态颜色要有明显层次

---

## 7.8 公告流程
1. 新建草稿
2. 编辑草稿
3. 发布
4. 下线

### 前端表现建议
- 列表状态清晰
- 编辑页简洁，避免 CMS 化过重
- 公众端仅展示已发布公告

---

## 8. 状态、标签与风险表达规范

请在设计中统一处理以下状态体系，不要每个页面各自发明表达方式。

## 8.1 企业相关
- 审核状态：`PENDING`, `APPROVED`, `REJECTED`
- 监管状态：`NORMAL`, `KEY`

## 8.2 产品相关
- `ACTIVE`, `INACTIVE`

## 8.3 检查任务
- `CREATED`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CLOSED`

## 8.4 抽检结果与公示
- 结果：`PASS`, `FAIL`
- 公示：`DRAFT`, `PUBLISHED`, `OFFLINE`

## 8.5 整改
- `ONGOING`, `SUBMITTED`, `REWORK`, `CONFIRMED`

## 8.6 投诉
- `SUBMITTED`, `PENDING`, `ASSIGNED`, `PROCESSING`, `FEEDBACKED`, `REJECTED`

## 8.7 预警
- 状态：`OPEN`, `PROCESSING`, `RESOLVED`, `CLOSED`
- 等级：`L1`, `L2`

## 8.8 设计要求
- 状态标签要全局统一
- 时间线节点、步骤条、列表标签之间要统一语义
- 高风险状态应明显，但整体视觉仍保持专业、克制

---

## 9. 接口契约与联调要求

## 9.1 全局规则

| 项目 | 规则 |
|---|---|
| 网关地址 | `http://localhost:8080` |
| 前端基础地址 | `VITE_API_BASE || http://localhost:8080` |
| 认证方式 | `Authorization: Bearer <token>` |
| 请求体 | JSON |
| 统一成功返回 | `{ code: 0, message: "ok", data, timestamp }` |
| 统一失败返回 | `{ code, message, timestamp }` |
| 401/403 | `{ code, message, traceId, timestamp }` |
| 分页结构 | `records`, `total`, `page`, `size`, `pages` |
| 链路追踪 | `X-Trace-Id` |

### 错误码线索
- `0`：成功
- `400`：参数错误 / 校验失败 / 非法状态流转
- `401`：未认证 / token 无效
- `403`：无权限 / 越权
- `404`：待确认
- `500`：服务端异常

## 9.2 权限与请求头线索
网关会透传：
- `X-User-Id`
- `X-Username`
- `X-User-Type`
- `X-User-Roles`

前端设计层面需要：
- 根据角色隐藏无权限入口
- 但真正权限控制以后端为准

## 9.3 重要限制
- 不要设计前端直连 `/api/warning/**`
- 预警必须走 `/api/regulation/warnings/**`
- 不要设计前端使用 `/api/internal/**`
- 不要默认设计导入、导出、独立文件下载等未确认能力

---

## 10. 关键接口范围（用于约束页面设计）

以下接口可作为页面设计和联调映射依据。

## 10.1 认证与用户
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `POST /api/auth/verify`
- `POST /api/users/register/public`
- `POST /api/users/register/enterprise`
- `POST /api/admin/users/regulators`
- `GET /api/roles`
- `POST /api/roles/bind`
- `GET /api/roles/user/{userId}`

## 10.2 企业、产品、区域、监管人员
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

## 10.3 公告与文件
- `GET /api/regulation/bulletins`
- `GET /api/regulation/bulletins/{id}`
- `POST /api/regulation/bulletins`
- `PUT /api/regulation/bulletins/{id}`
- `POST /api/regulation/bulletins/{id}/publish`
- `POST /api/regulation/bulletins/{id}/offline`
- `GET /api/regulation/public/bulletins`
- `GET /api/regulation/public/bulletins/{id}`
- `POST /api/files/presign`

## 10.4 检查、抽检、整改
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

## 10.5 投诉
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

## 10.6 预警与统计
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

---

## 11. 数据看板与分析设计要求

`query-service` 已确认存在 OpenAPI，可支持监管统计与预警统计。

### 统计过滤字段线索
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

### 时间格式
- `yyyy-MM-dd'T'HH:mm:ss`
- 使用 UTC+8 本地时间语义

### 设计建议
监管管理员首页和统计页可考虑：
- 总览卡片
- 趋势图
- 预警类型分布
- 处理效率分析
- 区域对比
- 高风险对象列表

但请保持：
- 图表服务于决策
- 不要做“大屏化”夸张视觉
- 不要让图表压过待办和业务操作入口

---

## 12. 上传与附件体验要求

上传接口：
- `POST /api/files/presign`

请求字段：
- `filename`
- `contentType`
- `size`
- `bizType`

响应字段：
- `uploadUrl`
- `fileUrl`
- `objectKey`

### 上传规则
- 上传前先获取预签名地址
- 再向 `uploadUrl` 发起 `PUT`
- 最大文件大小：`5MB`
- 支持：`image/jpeg`, `image/png`, `image/webp`
- 有效期：600 秒

### 业务限制
- `PUBLIC` 只能上传 `COMPLAINT`
- `ENTERPRISE` 只能上传 `RECTIFICATION`

### 设计要求
- 前端界面不需要暴露“预签名”这种技术概念
- 应体现为普通用户可理解的上传体验
- 要有上传中、成功、失败、重试、预览等状态反馈

---

## 13. 视觉与交互风格要求

整体风格应体现：

- 专业
- 可信
- 清晰
- 克制
- 高效
- 适合政务监管和企业使用

### 建议视觉方向
- 后台部分：现代政务后台 + 业务工作台
- 公众部分：服务门户化，但避免商业营销感
- 风险与异常：有明确强调，但不刺眼

### 不建议
- 不要做过重渐变和营销风
- 不要做炫技型大屏
- 不要为了“好看”牺牲复杂业务可读性
- 不要让公共门户与监管后台风格完全割裂

### 页面模式建议
- 工作台首页：卡片 + 待办 + 趋势 + 快捷入口
- 列表页：筛选区 + 表格/卡片 + 状态标签 + 批量操作
- 详情页：分区信息 + 操作区 + 时间线 + 附件
- 表单页：分组、清晰校验、状态反馈明确

---

## 14. 你至少需要产出的关键界面

请至少设计以下界面：

1. 登录/注册页  
2. 公众首页  
3. 公告列表页  
4. 公告详情页  
5. 企业公示列表页  
6. 企业公示详情页  
7. 抽检结果列表页  
8. 抽检结果详情页  
9. 投诉提交页  
10. 投诉追踪页  
11. 企业工作台首页  
12. 企业备案页  
13. 产品档案页  
14. 企业整改任务页  
15. 整改详情页  
16. 系统管理员监管人员管理页  
17. 监管管理员首页  
18. 企业管理页  
19. 企业备案审核页  
20. 检查任务页  
21. 抽检任务与结果发布页  
22. 投诉流转页  
23. 投诉详情页  
24. 整改复核页  
25. 风险预警页  
26. 预警详情页  
27. 公告管理页  
28. 执法人员首页  
29. 我的检查任务页  
30. 我的抽检任务页  
31. 执法投诉处理页  
32. 执法风险预警页  

---

## 15. 给你的最终执行要求

请基于以上信息，输出一套 **全新的食品安全监管平台前端设计方案**，并满足以下要求：

1. 先给出整体信息架构和角色工作区划分  
2. 再给出主要导航结构  
3. 再给出关键页面的高保真设计方案  
4. 明确每类页面的组件模式和状态表达方式  
5. 明确列表页、详情页、表单页、工作台、预警页之间的统一设计语言  
6. 设计应能映射到现有接口联调，不依赖未确认能力  
7. 对未确认能力必须显式标注“待确认”  
8. 不要把旧前端当成视觉基础  
9. 以中文界面为主  
10. 输出结果要适合进一步落地为真实前端页面

---

## 16. 待确认事项

以下内容尚未可靠确认，如设计中需要涉及，请明确标注为 **“待确认”**：

- 是否需要建立正式 URL 路由表
- 是否存在除 `query-service` 外的完整 Swagger/OpenAPI
- 是否存在统一字典接口
- 是否存在导入、导出、独立文件下载能力
- `/api/complaint/internal/stats/**` 是否对前端开放
- 公告是否支持分类、置顶、附件
- `INSPECTION` 类型文件上传是否面向监管端开放
- 自动预警升级、自动归档的完整规则
- 预警触发重点监管企业的完整产品规则
- 系统管理员是否需要进入统计看板
