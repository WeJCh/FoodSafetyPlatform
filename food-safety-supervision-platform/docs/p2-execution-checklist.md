# P2 整改执行清单

版本：V0.1  
日期：2026-03-23  
适用阶段：P1 完成后的第三阶段整改落地

---

## 1. P2 的目标

P2 不是把项目继续做大，而是在 P1 已经站住“监管平台”主线的基础上，补一个最小但足够有说服力的“食品安全专业增强层”。

本阶段只做四件事：

1. 增加最小产品档案，让“食品安全监管”有明确的食品/产品对象
2. 增加最小抽检任务与结果闭环，让项目具备抽检这一食品安全核心能力
3. 把监管统计从“只有预警统计”提升为“有真实业务来源的概览统计”
4. 清理默认密钥、内部 token 和 MinIO 默认凭据，让项目配置治理更像一个完成态系统

一句话定义：

> P2 的任务是让项目从“监管协同平台”进一步进化成“带有最小食品安全专业对象和抽检闭环的监管平台”。

---

## 2. P2 完成标准（Definition of Done）

P2 完成时，至少要满足下面 10 条：

- 企业端可以维护自己的产品档案，监管端可以按企业查看产品档案
- `regulation-service` 内正式出现产品主数据对象，不再只有企业对象
- 监管端可以创建抽检任务，并关联企业与产品
- 抽检任务可以被派发并录入抽检结果
- 抽检结果至少支持 `PASS / FAIL`、结论说明、处置建议、公示状态
- 抽检不合格结果可以触发真实业务联动，至少进入预警或重点监管链路之一
- 公众端可以查看已公示的抽检结果，不再只有企业公示和公告
- 监管端统计页不再只有预警统计，而是至少有“企业/检查/投诉/抽检/预警”核心概览
- `query-service` 仍然保持轻量，不扩成通用 BI 平台
- 默认密钥、内部 token、MinIO 默认凭据不再硬编码在主配置里

---

## 3. P2 范围边界

P2 必须收住，只做“最小食品安全增强”，不做下面这些内容：

- 不做检测机构独立角色
- 不做实验室回传、样本流转、送检物流
- 不做批次级追溯
- 不做召回管理
- 不做处罚/案件系统
- 不做全量统计 ETL 或离线数仓
- 不做新的微服务拆分
- 不做公众门户式复杂首页改版
- 不做复杂图表大屏

P2 的关键原则是：

- 有产品对象，但不做 ERP
- 有抽检闭环，但不做检测生态
- 有统计概览，但不做 BI 平台
- 有配置治理，但不做全套运维平台

---

## 4. 为什么 P2 应该是这四条线

这次不是重新空想范围，而是基于当前项目事实判断。

### 4.1 当前项目已经完成的部分

P1 完成后，项目已经具备：

- 企业备案/审批/公示
- 检查任务/检查记录/整改闭环
- 投诉受理/派单/反馈
- 风险预警与重点监管联动
- 公告发布与公众展示

这些能力已经足以把项目讲成“监管平台”。

### 4.2 当前项目仍然明显缺的部分

以下内容仍然是当前代码里真实缺失的：

- `food-web/src/views/EnterpriseProfileView.vue`
  - 当前只有 `企业备案 / 检查记录 / 整改任务`
  - 没有产品档案
- `regulation-service/src/main/java/com/mortal/regulation/controller`
  - 当前只有企业、监管员、公告、公示、文件、预警代理相关控制器
  - 没有产品控制器
- `regulation-operation-service/src/main/java/com/mortal/regulation/operation/controller`
  - 当前只有 `InspectionTaskController / InspectionRecordController / RectificationController`
  - 没有抽检任务或抽检结果控制器
- `query-service/src/main/java/com/mortal/query/controller`
  - 当前只有 `WarningStatsController` 和 `HealthController`
  - 统计页实际上还是“预警统计页”
- 多个 `application.yml`
  - 仍然保留了默认 JWT secret、内部 token、MinIO 默认凭据

### 4.3 为什么 P2 不再继续补别的

根据整改方案和当前项目状态，P2 最适合补的是：

- 产品档案
- 抽检结果
- 概览统计
- 配置治理

因为这 4 件事能同时解决两个核心问题：

1. 回答“为什么叫食品安全监管平台，却没有食品和抽检对象”
2. 让答辩时不仅能讲流程，还能讲出专业对象和阶段性工程治理

---

## 5. P2 实施顺序

严格按这个顺序做，不要乱穿插：

1. P2-0：建立 P2 基线和数据库改动清单
2. P2-1：补齐产品档案最小模块
3. P2-2：补齐抽检任务最小链路
4. P2-3：补齐抽检结果录入、公示和风险联动
5. P2-4：补齐监管概览统计
6. P2-5：清理默认密钥与配置治理
7. P2-6：联调与验收

---

## 6. P2-0：建立 P2 基线和数据库改动清单

### 目标

在开始 P2 前，先把本阶段会动到的表、服务和页面固定住，避免“做着做着又想加检测机构、追溯、召回”。

### 当前证据

- `regulation-service` 当前拥有企业主数据所有权，适合承载产品档案
- `regulation-operation-service` 当前拥有执行域所有权，适合承载抽检任务与抽检结果
- `query-service` 当前只做预警统计聚合，P2 只能最小扩展，不能扩成综合统计平台

### P2 预计会动到的数据库对象

- `food_regulation_db.food_enterprise`
  - 如需补少量监管字段，可在这里加最小字段
- `food_regulation_db.food_product`
  - 新增
- `food_regulation_operation_db.sampling_task`
  - 新增
- `food_regulation_operation_db.sampling_result`
  - 新增
- 如你决定做真实概览统计缓存，可额外评估 `food_query_db.stat_*`
  - 但默认不建议 P2 直接启用这三张表作为离线统计来源

### 建议动作

- 新建 P2 专用分支
- 先记录当前 4 个页面状态：
  - 企业工作台
  - 区域管理员工作台
  - 执法人员工作台
  - 公众首页 / 企业公示页
- 先记录当前 4 类能力状态：
  - 产品对象：缺失
  - 抽检对象：缺失
  - 监管概览统计：只有预警统计
  - 默认密钥治理：未完成

### 建议分支名

```powershell
git checkout -b rectify/p2-sampling-stats-governance
```

### 建议命令

后端基线编译：

```powershell
mvn -q -pl regulation-service,regulation-operation-service,complaint-service,warning-service,query-service -am -DskipTests compile
```

前端基线构建：

```powershell
cd food-web
npm run build
```

### 验收标准

- 分支已建立
- 你已经确认 P2 只做产品、抽检、概览统计、配置治理
- 当前基线编译与构建状态已知

---

## 7. P2-1：补齐产品档案最小模块

### 目标

给平台补上“食品安全专业对象”的最小起点，让企业不是唯一监管对象，至少还有产品/食品档案。

### 当前证据

- `regulation-service/src/main/resources/sql/schema.sql`
  - 当前只有 `food_enterprise`
  - 没有产品表
- `food-web/src/views/EnterpriseProfileView.vue`
  - 当前没有产品档案入口
- `regulation-service/src/main/java/com/mortal/regulation/controller`
  - 当前没有产品控制器

### 设计收束

P2 的产品档案只做企业自维护和监管查看，不做下面这些东西：

- 产品批次
- 原料配方
- 生产记录
- 条码/二维码
- 库存与销售
- 多级审批

### 建议落点

- 主数据归属：`regulation-service`
- 前端入口：
  - 企业端新增 `产品档案`
  - 监管管理员端企业详情中可查看产品列表
- 如执行域需要产品信息，通过 `regulation-service` 内部接口读取，不允许 `regulation-operation-service` 直接碰主数据表

### 推荐表结构

建议新增表：

```text
food_product
```

建议最小字段：

- `id`
- `enterprise_id`
- `product_name`
- `category`
- `specification`
- `status`
  - `ACTIVE / INACTIVE`
- `remark`
- `create_time`
- `update_time`
- `deleted`

### 需要修改的文件

- `regulation-service/src/main/resources/sql/schema.sql`
- 新增：
  - `regulation-service/src/main/java/com/mortal/regulation/entity/FoodProduct.java`
  - `regulation-service/src/main/java/com/mortal/regulation/mapper/FoodProductMapper.java`
  - `regulation-service/src/main/java/com/mortal/regulation/dto/ProductSaveDTO.java`
  - `regulation-service/src/main/java/com/mortal/regulation/vo/ProductVO.java`
  - `regulation-service/src/main/java/com/mortal/regulation/controller/ProductController.java`
  - `regulation-service/src/main/java/com/mortal/regulation/controller/internal/InternalProductController.java`
  - `regulation-service/src/main/java/com/mortal/regulation/service/ProductService.java`
  - `regulation-service/src/main/java/com/mortal/regulation/service/impl/ProductServiceImpl.java`
- `food-web/src/api/regulation.js`
- `food-web/src/views/EnterpriseProfileView.vue`
- 必要时：
  - `food-web/src/views/EnterpriseDetailView.vue`
  - `food-web/src/views/RegulatorAdminView.vue`

### 操作清单

#### 7.1 新增企业端产品档案 CRUD

企业侧至少支持：

- 新增产品
- 编辑产品
- 停用产品
- 查看产品列表

规则：

- 企业只能操作自己的产品
- 未完成企业备案的账号不能新增产品
- 已删除产品不再显示

#### 7.2 新增监管侧只读产品列表

监管侧至少支持：

- 查看某企业的产品列表
- 在创建抽检任务时按企业查询产品

建议接口：

```text
GET /api/regulation/products/my
POST /api/regulation/products
PUT /api/regulation/products/{id}
GET /api/regulation/enterprises/{enterpriseId}/products
GET /api/internal/regulation/products/{id}
POST /api/internal/regulation/products/summaries
```

#### 7.3 企业工作台新增“产品档案”分区

当前企业工作台只有：

- 企业备案
- 检查记录
- 整改任务

P2 需要新增：

- 产品档案

位置建议：

- 放在 `企业备案` 和 `检查记录` 之间

#### 7.4 不在 P2 做公众产品展示

P2 产品档案只服务监管与抽检，不要求向公众单独开放产品详情页。  
公众只在抽检结果页或企业详情中间接看到产品名称。

### 验收标准

- 企业端可以维护自己的产品档案
- 监管端可以按企业查看产品档案
- 产品主数据归属 `regulation-service`，没有跨服务直接读表
- 抽检任务创建时可以真实选择企业产品，而不是手填自由文本

---

## 8. P2-2：补齐抽检任务最小链路

### 目标

新增最小抽检任务对象，建立“企业 + 产品”双主线，让监管任务不再只有日常检查。

### 当前证据

- `regulation-operation-service/src/main/resources/sql/schema.sql`
  - 当前只有 `inspection_*`、`rectification_*`、`warning_event_outbox`
  - 没有 `sampling_task` 或 `sampling_result`
- `regulation-operation-service/src/main/java/com/mortal/regulation/operation/controller`
  - 当前只有检查、整改控制器
- `food-web/src/views/RegulatorAdminView.vue`
  - 当前没有抽检任务入口
- `food-web/src/views/RegulatorEnforcerView.vue`
  - 当前没有抽检执行入口

### 设计收束

P2 的抽检任务只做最小流程：

1. 区域管理员创建抽检任务
2. 选择企业和产品
3. 指派执法人员
4. 执法人员录入抽检结果
5. 不合格结果进入风险联动和公示候选

P2 不做：

- 检测机构独立协同
- 样品物流状态
- 复检/复核多轮流转
- 检测报告文件流转
- 多实验室并发检测

### 推荐表结构

建议新增表：

```text
sampling_task
sampling_result
```

`sampling_task` 最小字段建议：

- `id`
- `task_no`
- `enterprise_id`
- `product_id`
- `region_id`
- `task_title`
- `status`
  - `CREATED / ASSIGNED / COMPLETED / CLOSED`
- `created_by`
- `assigned_to`
- `assigned_by`
- `assigned_time`
- `deadline`
- `create_time`
- `update_time`
- `deleted`

`sampling_result` 最小字段建议：

- `id`
- `task_id`
- `enterprise_id`
- `product_id`
- `sampled_by`
- `sampled_time`
- `result`
  - `PASS / FAIL`
- `conclusion`
- `disposal_suggestion`
- `public_status`
  - `DRAFT / PUBLISHED / OFFLINE`
- `published_time`
- `create_time`
- `update_time`
- `deleted`

### 建议落点

- 执行域归属：`regulation-operation-service`
- 主数据依赖：
  - 企业来自 `regulation-service`
  - 产品来自 `regulation-service`
- 不要把产品字段冗余复制回主数据服务

### 需要修改的文件

- `regulation-operation-service/src/main/resources/sql/schema.sql`
- 新增：
  - `entity/SamplingTask.java`
  - `entity/SamplingResult.java`
  - `mapper/SamplingTaskMapper.java`
  - `mapper/SamplingResultMapper.java`
  - `dto/SamplingTaskCreateDTO.java`
  - `dto/SamplingTaskAssignDTO.java`
  - `dto/SamplingResultSubmitDTO.java`
  - `vo/SamplingTaskVO.java`
  - `vo/SamplingResultVO.java`
  - `service/SamplingTaskService.java`
  - `service/impl/SamplingTaskServiceImpl.java`
  - `controller/SamplingTaskController.java`
  - `controller/PublicSamplingResultController.java`
- `regulation-operation-service` 内部 regulation client
  - 需要加产品内部查询接口
- `food-web/src/api/regulationOperation.js`
- `food-web/src/views/RegulatorAdminView.vue`
- `food-web/src/views/RegulatorEnforcerView.vue`

### 操作清单

#### 8.1 区域管理员新增抽检任务

任务创建时至少选择：

- 企业
- 产品
- 截止时间
- 任务说明

规则：

- 只能选择本辖区企业
- 只能选择该企业的产品

#### 8.2 区域管理员派发抽检任务

规则：

- 只能派发给辖区内执法人员
- 任务必须先创建再派发

#### 8.3 执法人员只做结果录入，不做复杂检测流程

P2 建议：

- 不要求执法人员“开始任务”
- 可直接对已派发任务录入结果

如果你想复用当前检查任务风格，也可以保留 `IN_PROGRESS`，但不是 P2 必须项。

### 验收标准

- 区域管理员可以创建并派发抽检任务
- 抽检任务必须关联真实企业和真实产品
- 执法人员可以看到自己的抽检任务
- 抽检任务不新增独立微服务，仍在 `regulation-operation-service`

---

## 9. P2-3：补齐抽检结果录入、公示和风险联动

### 目标

让抽检不只是“新增两张表”，而是真正形成“结果录入 -> 风险触发 -> 公众可见”的最小专业闭环。

### 当前证据

- P1 已有：
  - 企业公示
  - 公告发布
  - 重点监管联动
  - warning outbox
- 但当前完全没有抽检结果对象和公众抽检公示入口

### 设计收束

P2 只做以下最小链路：

1. 执法人员或区域管理员录入抽检结果
2. `FAIL` 结果进入风险联动
3. 区域管理员决定是否公示
4. 公众可以查看已公示抽检结果

P2 不做：

- 检测报告附件审核
- 多次复检
- 行政处罚流转
- 召回公告

### 推荐联动方式

#### 9.1 不合格结果的风险联动

建议最小规则：

- `sampling_result.result = FAIL`
  - 生成预警事件
  - 记录企业重点监管原因

建议做法：

- 复用 `warning_event_outbox`
- 在 `regulation-service` 重点监管写接口中增加新原因类型：
  - `SAMPLING_FAIL`

如果你想再收一点边界，也可以先不新增新枚举，临时复用 `WARNING_TRIGGERED`，但从语义上不如新增明确。

#### 9.2 抽检结果公示

建议不要把抽检结果同步复制到 `regulation-service` 存主数据。  
更合理的做法是：

- 公众抽检结果接口直接放在 `regulation-operation-service`
- 公众页面单独查询抽检结果

这样可以避免：

- 跨服务复制执行数据
- 为了公示而新建同步表

### 需要修改的文件

- `regulation-operation-service/src/main/java/com/mortal/regulation/operation/service/impl/SamplingTaskServiceImpl.java`
- `regulation-operation-service` 现有 warning outbox 相关类
- `regulation-service/src/main/java/com/mortal/regulation/common/enums/EnterpriseKeyReasonType.java`
- `regulation-service/src/main/java/com/mortal/regulation/service/impl/EnterpriseKeyReasonServiceImpl.java`
- `food-web/src/App.vue`
- 新增：
  - `food-web/src/views/PublicSamplingResultListView.vue`
  - `food-web/src/views/PublicSamplingResultDetailView.vue`
- `food-web/src/views/PublicHomeView.vue`
- 必要时：
  - `food-web/src/views/PublicEnterpriseDetailView.vue`

### 操作清单

#### 9.3 增加抽检结果录入动作

建议接口：

```text
POST /api/regulation-operation/sampling/tasks/{id}/result
GET /api/regulation-operation/sampling/results
GET /api/regulation-operation/sampling/results/{id}
```

结果录入至少包括：

- 采样时间
- 结果
- 结论
- 处置建议

#### 9.4 增加抽检结果公示状态

不要默认所有结果都对公众公开。  
建议增加：

- `public_status`
  - `DRAFT / PUBLISHED / OFFLINE`

由区域管理员控制发布与下线。

#### 9.5 公众端新增抽检结果页

当前公众端已有：

- 企业公示
- 公告
- 投诉
- 投诉跟踪

P2 建议新增：

- 抽检结果

注意：

- 这是“结果公示页”
- 不是“食品百科页”
- 不要扩成复杂门户

#### 9.6 企业公示详情只做轻量增强

如果时间充足，可在 `PublicEnterpriseDetailView.vue` 中增加：

- 最近一次已公示抽检结论

如果时间有限：

- 不要做跨服务回写
- 公众从企业公示详情跳去抽检结果列表即可

### 验收标准

- 抽检结果可以录入并落库
- `FAIL` 抽检结果能触发风险联动
- 已公示抽检结果可被公众查看
- 公众端新增抽检结果页后，平台题目和能力更匹配

---

## 10. P2-4：补齐监管概览统计

### 目标

让监管端统计页不再只有预警统计，而是至少有真实业务来源的监管概览。

### 当前证据

- `query-service/src/main/java/com/mortal/query/controller/WarningStatsController.java`
  - 当前只做预警统计
- `food-web/src/views/RegulatorAdminView.vue`
  - 当前 `stats` 分区实际只挂 `WarningStatsPanel`
- `food-web/src/views/RegulatorEnforcerView.vue`
  - 当前同样只有预警统计
- `query-service/src/main/resources/sql/schema.sql`
  - `stat_*` 表仍是预留结构，P0 已冻结为“不投入真实运行链路”

### 设计收束

P2 统计页只做“监管概览”，不做大屏和复杂趋势中心。

建议最小概览卡片：

- 企业总数
- 重点监管企业数
- 已备案通过企业数
- 检查总数 / 不合格数
- 抽检总数 / 不合格数
- 投诉总数 / 已反馈数 / 超时数
- 待处理预警数

### 推荐做法

P2 不建议直接启用 `stat_enterprise/stat_inspection/stat_complaint` 做离线汇总。  
更稳妥的做法是：

- `query-service` 增加一个最小“监管概览聚合接口”
- 直接调用各业务服务的内部统计接口进行实时聚合

好处：

- 不需要额外定时任务
- 不需要定义复杂统计口径表
- 与当前毕业设计规模更匹配

### 建议落点

- 聚合服务：`query-service`
- 不新增新服务
- 不扩 `stat_*` 表的写入链路

### 需要修改的文件

- `query-service/src/main/java/com/mortal/query/controller`
  - 新增概览控制器
- `query-service/src/main/java/com/mortal/query/service`
  - 新增概览聚合服务
- `query-service` 新增对以下服务的内部客户端：
  - `regulation-service`
  - `regulation-operation-service`
  - `complaint-service`
  - `warning-service`
- `food-web/src/components`
  - 新增 `SupervisionOverviewPanel.vue`
- `food-web/src/views/RegulatorAdminView.vue`
- `food-web/src/views/RegulatorEnforcerView.vue`

### 操作清单

#### 10.1 只做概览，不做 BI

新增接口建议：

```text
GET /api/query/dashboard/overview
```

不要在 P2 做：

- 多维钻取
- 区域热力图
- 大屏轮播
- 多维趋势分析

#### 10.2 由 query-service 做实时聚合

建议聚合来源：

- `regulation-service`
  - 企业总数、重点监管数、审批通过数
- `regulation-operation-service`
  - 检查总数、不合格数、抽检总数、抽检不合格数
- `complaint-service`
  - 投诉总数、已反馈数、超时数
- `warning-service`
  - 待处理预警数

#### 10.3 前端统计页改成“双层结构”

当前统计页可以改成：

1. 顶部：监管概览卡片
2. 下方：保留现有预警统计图表

这样能保留 P0/P1 成果，不推翻已有页面。

### 验收标准

- 监管端统计页不再只有预警统计
- 概览数据来自真实业务服务
- `query-service` 没有被扩成通用统计平台
- `stat_*` 表仍然可以继续保留为预留结构，不强制启用

---

## 11. P2-5：清理默认密钥与配置治理

### 目标

收掉当前项目中最明显的“半成品痕迹”，避免答辩时被追问为什么配置里还是默认 secret 和 `minioadmin`。

### 当前证据

- `gateway-service/src/main/resources/application.yml`
  - 当前硬编码 JWT secret
- `user-service/src/main/resources/application.yml`
  - 当前硬编码 JWT secret
- `regulation-service/src/main/resources/application.yml`
  - 当前硬编码 JWT secret
  - 当前仍有 `minioadmin` 默认凭据
  - 当前仍有内部 token 默认值
- `regulation-operation-service/src/main/resources/application.yml`
  - 当前仍有 JWT secret 和内部 token 默认值
- `complaint-service/src/main/resources/application.yml`
  - 当前仍有内部 token 默认值
- `warning-service/src/main/resources/application.yml`
  - 当前仍有 warning internal token 默认值
- `query-service/src/main/resources/application.yml`
  - 当前仍有 warning internal token 默认值

### 设计收束

P2 的配置治理不是上 Vault，不是上 KMS。  
只做以下几件事：

1. 去掉主配置里的硬编码 secret
2. 去掉主配置里的默认内部 token
3. 去掉 MinIO 默认凭据
4. 补一份本地开发环境配置模板
5. 给关键配置加启动校验

### 推荐做法

建议配置策略：

- `application.yml`
  - 只保留环境变量引用
- 本地开发模板
  - 新增 `docs/local-env-example.md` 或 `.env.example`
- 启动校验
  - 若关键配置为空或仍为 `CHANGE_ME`，启动时报错

### 需要修改的文件

- `gateway-service/src/main/resources/application.yml`
- `user-service/src/main/resources/application.yml`
- `regulation-service/src/main/resources/application.yml`
- `regulation-operation-service/src/main/resources/application.yml`
- `complaint-service/src/main/resources/application.yml`
- `warning-service/src/main/resources/application.yml`
- `query-service/src/main/resources/application.yml`
- 新增：
  - `docs/local-env-example.md`
  - 必要时新增配置校验类

### 操作清单

#### 11.1 清理 JWT secret

把下面这些硬编码改成环境变量读取：

- `gateway-service`
- `user-service`
- `regulation-service`
- `regulation-operation-service`

#### 11.2 清理内部 token 默认值

至少清理：

- `REGULATION_INTERNAL_TOKEN`
- `WARNING_INTERNAL_TOKEN`

建议不要再保留真实默认值，改成：

- 环境变量必填
- 或使用 `CHANGE_ME` 占位并做启动校验

#### 11.3 清理 MinIO 默认凭据

至少清理：

- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`

#### 11.4 增加本地运行说明

当前项目已经是多服务结构，如果不补配置模板，后续自己跑也会反复踩坑。  
建议新增一份最小本地环境说明：

- MySQL
- Nacos
- MinIO
- JWT secret
- internal token

### 验收标准

- 主配置文件中不再出现真实 secret 字面量
- 主配置文件中不再出现可直接使用的默认内部 token
- MinIO 不再默认使用 `minioadmin`
- 项目新增了一份本地运行配置模板

---

## 12. P2-6：联调与验收

### 目标

确认 P2 不是“加了几个表和几个页面”，而是真正形成“产品档案 + 抽检最小闭环 + 统计概览 + 配置治理”的完成态增强。

### 你要做什么

- 先跑后端联合测试
- 再跑前端构建
- 最后做 5 组人工验收

### 建议命令

后端：

```powershell
mvn -q -pl regulation-service,regulation-operation-service,complaint-service,warning-service,query-service -am test
```

前端：

```powershell
cd food-web
npm run build
```

### 建议补的自动化测试

- `regulation-service`
  - 产品档案企业权限测试
  - 监管侧企业产品列表测试
- `regulation-operation-service`
  - 抽检任务创建/派发测试
  - 抽检不合格触发预警/重点监管测试
  - 公共抽检结果公示接口测试
- `query-service`
  - 概览统计聚合接口测试
- 配置治理
  - 关键配置缺失时的启动校验测试

### 人工验收路径 1：产品档案

1. 企业登录
2. 进入产品档案页
3. 新增两个产品
4. 区域管理员查看该企业详情
5. 确认能看到产品列表

### 人工验收路径 2：抽检任务

1. 区域管理员登录
2. 创建一条抽检任务，选择企业和产品
3. 指派给执法人员
4. 执法人员查看自己的抽检任务

### 人工验收路径 3：抽检结果与公示

1. 执法人员录入抽检结果
2. 对 `FAIL` 结果执行发布或由管理员发布
3. 公众进入抽检结果页
4. 确认已发布结果可见

### 人工验收路径 4：风险联动

1. 对某企业录入不合格抽检结果
2. 检查是否触发预警
3. 检查企业是否进入 `KEY`
4. 查看重点监管原因是否出现抽检来源

### 人工验收路径 5：监管概览统计

1. 区域管理员进入统计页
2. 顶部看到企业、检查、投诉、抽检、预警概览
3. 下方仍可查看预警统计
4. 确认页面不是空壳

### 验收标准

- 企业端、监管端、公众端在 P2 新增能力上都能形成闭环
- 项目能够清晰讲出“企业对象 + 产品对象 + 抽检对象”三层结构
- 配置治理已经从“开发默认值”走向“可交付状态”

---

## 13. P2 期间不要改的内容

为了防止边界再次漂移，P2 明确不动这些方向：

- 不做检测机构独立工作台
- 不做实验室报告流转
- 不做批次追溯
- 不做召回管理
- 不做案件处罚
- 不做 `query-service` 离线汇总 ETL
- 不做新的大屏系统
- 不重写整个前端工作台

如果你在开发过程中又想扩功能，先问自己一句：

> 这个东西会不会直接提升“产品档案、抽检最小闭环、监管概览统计、配置治理”这四条线的完成度？

如果不会，就不要在 P2 做。

---

## 14. 建议提交顺序

推荐按下面的提交粒度推进：

1. `feat(product): add minimal enterprise product archive module`
2. `feat(sampling): add minimal sampling task flow`
3. `feat(sampling): add sampling result publication and risk linkage`
4. `feat(query): add lightweight supervision overview aggregation`
5. `chore(config): remove default secrets and internal tokens`
6. `test(p2): add smoke checks for product sampling stats and config`

---

## 15. P2 完成后的状态判断

如果 P2 做完，项目应该达到下面这个状态：

- 项目不再只有企业对象，而是有真实产品对象
- 平台不再只有日常检查，还具备最小抽检能力
- 公众端不再只有企业公示和公告，还能看到抽检结果
- 监管端统计页不再只是预警统计
- 系统配置不再像开发半成品，而更接近可交付状态

这时项目在答辩里可以更稳定地讲成四条主线：

- 企业主体纳管
- 检查整改与投诉协同
- 抽检结果与风险联动
- 公众公示与监管概览

到了这个状态，项目就已经足够支撑毕业设计答辩，不需要再继续向全链路追溯和召回系统扩张。
