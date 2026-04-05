# P1 整改执行清单

版本：V0.1  
日期：2026-03-19  
适用阶段：P0 完成后的第二阶段整改落地

---

## 1. P1 的目标

P1 不是继续堆功能，而是把项目从“内部流程能跑”推进到“外部看起来就是食品安全监管平台”。

本阶段只做五件事：

1. 把公众侧的企业公示链路补成真实可用能力
2. 把公告发布与展示链路补齐
3. 把企业端检查记录查询链路补齐
4. 把投诉反馈从“只有状态”补成“有时限、有反馈、有驳回原因”
5. 把企业重点监管状态真正和检查、投诉联动起来

---

## 2. P1 完成标准（Definition of Done）

P1 完成时，至少要满足下面 8 条：

- 公众首页重新开放 `企业公示` 和 `公告信息` 入口，且点击后进入真实页面
- 公众可以查看已公示企业列表和企业详情，不再只有投诉表单里的企业选择器
- 区域管理员可以发布、下线公告，公众端只能看到已发布公告
- 企业账号可以在企业工作台查看自己的检查记录和检查详情
- 投诉处理链路中，公众可以看到明确的办理时限、反馈摘要或驳回原因
- 企业在“连续不合格”或“投诉过多”时，系统会把企业标记为 `KEY`，并记录原因
- 监管端企业列表/详情中的 `重点监管` 状态来自真实联动，而不是空标签
- P1 涉及模块改动完成后，后端相关模块能编译，前端能构建

---

## 3. P1 范围边界

P1 要收住，不做下面这些事：

- 不启动 `抽检/检测机构/实验室回传` 整体建设
- 不启动 `食品追溯/召回` 全链路建设
- 不新增新的微服务来承载公告、公示或风险联动
- 不把投诉改造成复杂流程引擎或 BPM 系统
- 不在企业端新增“申诉”“复议”“在线沟通”等延伸功能
- 不在检查记录里补附件上传、复杂分类、图片比对等扩展能力

P1 的核心是把当前最能体现“监管平台”的五条线做实，而不是把项目继续做大。

---

## 4. P1 实施顺序

严格按这个顺序做，不要乱穿插：

1. P1-0：建立 P1 基线和数据库改动清单
2. P1-1：补齐公众企业公示链路
3. P1-2：补齐公告发布与展示链路
4. P1-3：补齐企业检查记录链路
5. P1-4：增强投诉反馈链路
6. P1-5：打通重点监管联动
7. P1-6：联调与验收

---

## 5. P1-0：建立 P1 基线和数据库改动清单

### 目标

在开始 P1 之前，先把本阶段会动到的模块、表和页面固定住，避免中途边做边漂。

### 你要做什么

- 新建一个 P1 专用整改分支
- 先整理本阶段会改的数据库对象
- 先做一次后端编译和前端构建，确保当前是干净基线

### P1 预计会动到的数据库对象

- `food_regulation_db.food_enterprise`
- `food_regulation_db.enterprise_key_reason`
- `food_regulation_db` 新增公告表
- `food_complaint_db.complaint`
- 如你决定保留投诉处理明细，也会涉及 `food_complaint_db.complaint_handle`

### 建议动作

- 分支名建议：`rectify/p1-public-risk-loop`
- 先记录当前这几个页面状态：
  - 公众首页
  - 企业工作台
  - 区域管理员工作台
  - 执法人员工作台
- 先记录当前这几类接口状态：
  - `/api/regulation/public/enterprises`
  - 公众公告接口目前不存在
  - 企业检查记录接口目前不存在
  - 投诉详情接口已有，但结构偏粗

### 建议命令

```powershell
git checkout -b rectify/p1-public-risk-loop
```

后端基线编译：

```powershell
mvn -q -pl regulation-service,regulation-operation-service,complaint-service,warning-service -am -DskipTests compile
```

前端基线构建：

```powershell
cd food-web
npm run build
```

### 验收标准

- 分支已建立
- 你已经明确 P1 会改哪些表、哪些服务、哪些页面
- 当前系统基线编译和构建状态已知

---

## 6. P1-1：补齐公众企业公示链路

### 目标

把“公众企业信息”从投诉表单里的下拉选项，补成真正的公众公示能力。

### 当前证据

- `regulation-service/src/main/java/com/mortal/regulation/controller/PublicEnterpriseController.java`
  - 当前只有分页列表接口，没有详情接口
- `regulation-service/src/main/java/com/mortal/regulation/vo/PublicEnterpriseVO.java`
  - 当前只够支撑投诉表单选择，字段过少
- `food-web/src/api/regulation.js`
  - 当前只有 `fetchPublicEnterprises`
- `food-web/src/views/PublicHomeView.vue`
  - 页面明确写着企业公示能力“后续补齐后再开放”
- `food-web/src/App.vue`
  - 当前没有 `public-enterprises` 或 `public-enterprise-detail` 视图切换
- `food-web/src/views/PublicComplaintView.vue`
  - 当前 `fetchPublicEnterprises` 只被用于投诉时选择企业

### 需要修改的文件

- `regulation-service/src/main/java/com/mortal/regulation/controller/PublicEnterpriseController.java`
- `regulation-service/src/main/java/com/mortal/regulation/service/EnterpriseProfileService.java`
- `regulation-service/src/main/java/com/mortal/regulation/service/impl/EnterpriseProfileServiceImpl.java`
- `regulation-service/src/main/java/com/mortal/regulation/vo/PublicEnterpriseVO.java`
- 新增 `regulation-service/src/main/java/com/mortal/regulation/vo/PublicEnterpriseDetailVO.java`
- `food-web/src/api/regulation.js`
- `food-web/src/App.vue`
- `food-web/src/views/PublicHomeView.vue`
- 新增 `food-web/src/views/PublicEnterpriseListView.vue`
- 新增 `food-web/src/views/PublicEnterpriseDetailView.vue`

### 操作清单

#### 6.1 新增公众企业详情接口

新增接口建议：

```text
GET /api/regulation/public/enterprises/{id}
```

规则：

- 只能查询 `approval_status = APPROVED` 的企业
- 已删除企业不可见
- 不暴露 `userId`
- 不暴露负责人完整手机号

#### 6.2 扩展公众企业展示字段

公众详情页至少展示：

- 企业名称
- 许可证编号
- 所属区域
- 详细地址
- 负责人姓名
- 企业状态 `NORMAL / KEY`
- 审核通过时间
- 最近更新时间
- 包保责任人或监管联系人名称

注意：

- P1 不要求补 `license_expire_time`
- P1 也不要求单独做 `public_status` 字段
- 当前先以 `approval_status = APPROVED` 作为公示可见条件

#### 6.3 增加公众侧页面

前端至少补两页：

- `企业公示列表页`
- `企业公示详情页`

并在 `App.vue` 中补对应 view 切换：

- `public-enterprises`
- `public-enterprise-detail`

#### 6.4 恢复公众首页入口

`PublicHomeView.vue` 中重新开放：

- `企业公示`

要求：

- 点击后进入真实列表页
- 不是再次跳回投诉页
- 不是只弹提示文案

#### 6.5 复用现有监管端详情展示思路

`food-web/src/views/EnterpriseDetailView.vue` 现在已经有一套只读详情样式，可以参考，但不能直接把监管端详情原样搬到公众端。

公众端要注意：

- 去掉内部审核备注类信息
- 去掉企业账号内字段
- 只展示对外可解释的信息

### 验收标准

- 公众首页可以进入企业公示列表
- 公众可以查看已通过企业的详情
- 未通过审核企业不能通过公众详情接口访问
- 原有投诉页的企业选择能力不受影响
- `KEY` 企业在公示详情中能看到“重点监管”状态

### 建议提交

```text
feat(public): add public enterprise list and detail flow
```

---

## 7. P1-2：补齐公告发布与展示链路

### 目标

让公众端除了投诉之外，真正具备“信息公示”能力。

### 当前证据

- 当前项目中基本没有 `bulletin / 公告` 相关后端代码
- `food-web/src/views/PublicHomeView.vue`
  - 只有“公告发布后续补齐”的说明文案
- 当前前端没有公告列表页、公告详情页
- 当前也没有哪个角色在系统内真正承担公告发布职责

### 设计收束

P1 不新建公告微服务，直接放到 `regulation-service` 中实现。

公告发布角色只给：

- `REGULATOR_ADMIN`

不要给：

- 平台管理员
- 企业用户
- 公众用户

### 需要修改的文件

- `regulation-service/src/main/resources/sql/schema.sql`
- 新增公告实体/Mapper/DTO/VO/Service/Controller
- `food-web/src/api/regulation.js`
- `food-web/src/views/RegulatorAdminView.vue`
- `food-web/src/App.vue`
- `food-web/src/views/PublicHomeView.vue`
- 新增 `food-web/src/views/PublicBulletinListView.vue`
- 新增 `food-web/src/views/PublicBulletinDetailView.vue`

### 操作清单

#### 7.1 新增公告表

建议新增表：`public_bulletin`

字段建议至少包含：

- `id`
- `title`
- `summary`
- `content`
- `status`：`DRAFT / PUBLISHED / OFFLINE`
- `published_by`
- `published_time`
- `create_time`
- `update_time`
- `deleted`

P1 不需要：

- 富文本编辑器
- 多附件
- 复杂栏目分类

#### 7.2 新增监管端公告接口

建议至少补下面几类接口：

- 新建公告
- 编辑公告
- 查询公告列表
- 查询公告详情
- 发布公告
- 下线公告

这些接口全部只允许区域管理员使用。

#### 7.3 新增公众公告接口

建议至少补下面两类接口：

- `GET /api/regulation/public/bulletins`
- `GET /api/regulation/public/bulletins/{id}`

公众端只能看到：

- `status = PUBLISHED`
- 且未删除的公告

#### 7.4 在监管端补“公告发布”入口

推荐直接放在：

- `food-web/src/views/RegulatorAdminView.vue`

原因：

- 当前项目真正承担业务主线的是监管管理员，而不是平台管理员
- 如果把公告发布放到 `AdminView.vue`，只会进一步制造角色混乱

#### 7.5 在公众首页补“公告信息”入口

恢复公众首页中的：

- `公告信息`

要求：

- 进入真实的公告列表页
- 点击列表后进入公告详情页

### 验收标准

- 区域管理员可以创建并发布公告
- 已发布公告能在公众端看到
- 下线公告后公众端不可见
- 公众首页存在真实可用的公告入口
- 整个公告能力仍属于 `regulation-service`，没有新增服务漂移

### 建议提交

```text
feat(public): add bulletin publish and public display flow
```

---

## 8. P1-3：补齐企业检查记录链路

### 目标

让企业用户除了看整改任务，还能看到“为什么被整改”，形成最基本的监管可解释链路。

### 当前证据

- `food-web/src/views/EnterpriseProfileView.vue`
  - 当前只有 `企业备案` 和 `整改任务`
- `regulation-operation-service/src/main/java/com/mortal/regulation/operation/controller/InspectionRecordController.java`
  - 当前所有检查记录接口都限定为监管角色
- `regulation-operation-service/src/main/java/com/mortal/regulation/operation/service/impl/InspectionRecordServiceImpl.java`
  - 当前只有监管员视角的 `listMy / listForAdmin / getDetail`
- `regulation-operation-service/src/main/java/com/mortal/regulation/operation/support/OperationMasterDataSupport.java`
  - 实际已经有 `requireEnterpriseByUserId`，说明企业端查询基础能力已存在

### 需要修改的文件

- `regulation-operation-service/src/main/java/com/mortal/regulation/operation/controller/InspectionRecordController.java`
- `regulation-operation-service/src/main/java/com/mortal/regulation/operation/service/InspectionRecordService.java`
- `regulation-operation-service/src/main/java/com/mortal/regulation/operation/service/impl/InspectionRecordServiceImpl.java`
- 可选扩展 `InspectionRecordVO / InspectionRecordDetailVO`
- `food-web/src/api/regulationOperation.js`
- `food-web/src/views/EnterpriseProfileView.vue`

### 操作清单

#### 8.1 新增企业端只读检查接口

建议新增接口：

- `GET /api/regulation-operation/inspections/enterprise`
- `GET /api/regulation-operation/inspections/enterprise/{id}`

规则：

- 只允许 `ENTERPRISE`
- 只能看当前账号对应企业自己的记录
- 不允许企业修改或删除检查记录

#### 8.2 在服务层新增企业视角查询方法

建议新增：

- `listForEnterprise(Long userId, ...)`
- `getDetailForEnterprise(Long userId, Long recordId)`

实现方式：

- 直接复用 `OperationMasterDataSupport.requireEnterpriseByUserId(userId)`
- 用企业 `id` 过滤 `inspection_record.enterprise_id`

#### 8.3 企业前端增加“检查记录”栏目

在 `EnterpriseProfileView.vue` 中新增导航：

- `检查记录`

页面能力只做三件事：

- 分页列表
- 条件筛选
- 查看详情

P1 不做：

- 企业对检查结果申诉
- 企业上传检查佐证
- 企业修改检查记录

#### 8.4 详情页展示重点信息

详情至少展示：

- 检查日期
- 检查结果
- 总体问题描述
- 检查项明细

如果工作量允许，可以补：

- 检查人员姓名

但这不是 P1 必过项。

### 验收标准

- 企业登录后能进入 `检查记录` 页面
- 企业只能看到自己的记录
- 检查详情能解释清楚“检查结论”和“问题项”
- 现有监管端检查记录页面不受影响

### 建议提交

```text
feat(enterprise): add read-only inspection record flow
```

---

## 9. P1-4：增强投诉反馈链路

### 目标

把当前投诉链路从“有状态流转”提升为“公众能看到办理结果和时限”的闭环。

### 当前证据

- `complaint-service/src/main/resources/sql/schema.sql`
  - `complaint` 表没有 `deadline_time`
  - 没有 `feedback_summary`
  - 没有 `reject_reason`
- `complaint-service/src/main/java/com/mortal/complaint/application/ComplaintCommandService.java`
  - 当前 `handle` 和 `reject` 都把结果写进单一的 `handleResult`
- `food-web/src/views/PublicComplaintTrackView.vue`
  - 当前已经能显示 `handleResult`
  - 说明“反馈展示”不是完全没有，而是结构过粗
- `complaint-service/src/main/java/com/mortal/complaint/domain/entity/Complaint.java`
  - 当前主表没有能支撑投诉 SLA 和结构化反馈的字段

### 设计判断

这一项不要误做成“重写投诉系统”。

P1 只做两件事：

- 把办理时限补进去
- 把“反馈摘要”和“驳回原因”从混合文本里拆出来

### 需要修改的文件

- `complaint-service/src/main/resources/sql/schema.sql`
- `complaint-service/src/main/java/com/mortal/complaint/domain/entity/Complaint.java`
- `complaint-service/src/main/java/com/mortal/complaint/vo/ComplaintVO.java`
- `complaint-service/src/main/java/com/mortal/complaint/dto/ComplaintAssignDTO.java`
- `complaint-service/src/main/java/com/mortal/complaint/application/ComplaintCommandService.java`
- `complaint-service/src/main/java/com/mortal/complaint/application/ComplaintDataSupport.java`
- `food-web/src/views/PublicComplaintTrackView.vue`
- `food-web/src/views/RegulatorAdminComplaintDetailView.vue`
- `food-web/src/views/RegulatorEnforcerComplaintDetailView.vue`

### 操作清单

#### 9.1 给投诉主表补结构化字段

建议给 `complaint` 表增加：

- `deadline_time`
- `feedback_summary`
- `reject_reason`

同时把状态注释补完整，加入：

- `REJECTED`

#### 9.2 在派单时写入处理时限

改 `ComplaintAssignDTO`，允许管理员派单时传 `deadlineTime`。

建议规则：

- 前端可填
- 不填时系统自动给默认值

默认值建议：

- `当前时间 + 3 天`

这样后续前端和统计都能讲清楚“投诉有办理时限”。

#### 9.3 在处理完成时写反馈摘要

当前 `handleResult` 不要直接废弃。

P1 推荐做法：

- `complaint.feedback_summary` 存结构化摘要
- `complaint_handle.handle_result` 继续保留，作为处理留痕

这样兼顾：

- 公众端直观展示
- 处理过程留痕

#### 9.4 在驳回时写驳回原因

当前 `reject` 也是把原因塞到 `handleResult`。

P1 要改成：

- 主表写 `reject_reason`
- 明细表仍保留一条处理记录

#### 9.5 优化公众投诉详情展示

`PublicComplaintTrackView.vue` 需要补充展示：

- 当前处理时限
- 已反馈时的反馈摘要
- 已驳回时的驳回原因

注意：

- 当前已有 `handleResult` 展示逻辑，不要直接删掉，先兼容
- 兼容期可以优先读 `feedbackSummary / rejectReason`
- 为空时再回退读 `handleResult`

### 验收标准

- 投诉被派单后，公众能看到处理时限
- 投诉办结后，公众能看到明确反馈摘要
- 投诉驳回后，公众能看到明确驳回原因
- 监管端详情页能看到时限与结果字段
- 原有投诉状态流转不被破坏

### 建议提交

```text
feat(complaint): add deadline and structured feedback fields
```

---

## 10. P1-5：打通重点监管联动

### 目标

让 `food_enterprise.status = KEY` 真正变成有来源、有原因、有业务含义的状态。

### 当前证据

- `regulation-service/src/main/resources/sql/schema.sql`
  - 已有 `food_enterprise.status`
  - 已有 `enterprise_key_reason`
- `regulation-service/src/main/java/com/mortal/regulation/entity/EnterpriseKeyReason.java`
  - 实体已存在
- `regulation-service/src/main/java/com/mortal/regulation/mapper/EnterpriseKeyReasonMapper.java`
  - Mapper 已存在
- 当前项目里几乎没有真正写入 `enterprise_key_reason` 的业务逻辑
- `regulation-operation-service`
  - 已有完整的预警 outbox/upsert 模式，可用于检查类事件
- `complaint-service`
  - 当前没有重点监管联动逻辑
- `food-web/src/views/RegulatorAdminView.vue` 与 `RegulatorEnforcerView.vue`
  - 企业列表已经有 `KEY` 筛选，但当前缺少真实触发来源

### 设计收束

P1 只做两个触发来源：

1. 连续不合格
2. 投诉过多

P1 不做：

- 自动降级回 `NORMAL`
- 多级风险评分模型
- 复杂风险画像

### 需要修改的文件

- `regulation-service` 内部企业接口及服务
- `regulation-service/src/main/java/com/mortal/regulation/entity/EnterpriseKeyReason.java`
- `regulation-service/src/main/java/com/mortal/regulation/mapper/EnterpriseKeyReasonMapper.java`
- `regulation-operation-service/src/main/java/com/mortal/regulation/operation/service/impl/InspectionTaskServiceImpl.java`
- `regulation-operation-service` 相关 warning outbox 逻辑
- `complaint-service/src/main/java/com/mortal/complaint/application/ComplaintCommandService.java`
- 必要时新增 complaint -> regulation 的内部写接口客户端
- `food-web/src/views/RegulatorAdminView.vue`
- `food-web/src/views/RegulatorEnforcerView.vue`
- `food-web/src/views/EnterpriseDetailView.vue`
- `food-web/src/views/PublicEnterpriseDetailView.vue`

### 操作清单

#### 10.1 在 regulation-service 内新增“重点监管写接口”

不要让其他服务直接操作 `food_enterprise` 表。

推荐做法：

- 在 `regulation-service` 内部控制器中新增内部写接口
- 由 `regulation-service` 统一完成：
  - 写 `enterprise_key_reason`
  - 把 `food_enterprise.status` 改为 `KEY`

建议接口能力至少包含：

- 根据企业 ID 写入重点监管原因
- 避免同一来源重复插入相同原因

#### 10.2 打通“连续不合格 -> 重点监管”

触发点建议放在：

- `InspectionTaskServiceImpl.submitTask(...)`

实现方式：

- 每次提交 `FAIL` 检查结果后
- 查询该企业最近 N 次检查记录
- 如果达到连续不合格阈值，则调用 `regulation-service` 内部重点监管接口

建议阈值：

- 默认 `2` 次连续不合格

并建议加配置项，不要写死在代码里。

#### 10.3 连续不合格同时写预警事件

这部分优先复用现有模式：

- `WarningEventUpsertDTO`
- `WarningEventOutboxService`
- `warning-service` 的 internal upsert 接口

不要在 P1 为了这件事再造一套新预警同步方案。

#### 10.4 打通“投诉过多 -> 重点监管”

触发点建议放在：

- `ComplaintCommandService.accept(...)`

原因：

- `accept` 代表投诉被监管端确认进入处理，不容易被无效投诉直接污染

建议规则：

- 统计某企业最近 30 天有效投诉量
- 达到阈值后写入 `COMPLAINT_OVERFLOW`

建议阈值：

- 默认 `3` 条

P1 这里先保证：

- 写重点监管原因
- 把企业状态改成 `KEY`

如果你时间足够，再补投诉类 warning upsert；如果时间不够，这一部分不作为 P1 必过项。

#### 10.5 把重点监管原因展示到前端

至少要让下面几个地方显示“为什么是重点监管”：

- 监管端企业详情
- 企业公示详情

如果工作量允许，再补：

- 监管端企业列表中显示“最近一次重点监管原因”

### 验收标准

- 连续不合格达到阈值后，企业状态自动变为 `KEY`
- 投诉过多达到阈值后，企业状态自动变为 `KEY`
- `enterprise_key_reason` 中有真实记录，不再是空表
- 监管端能看到重点监管原因
- 公众企业详情能看到企业当前是重点监管状态

### 建议提交

```text
feat(risk): link inspections and complaints to key-supervision status
```

---

## 11. P1-6：联调与验收

### 目标

确认 P1 五条主线不是各做各的，而是形成可演示的业务闭环。

### 你要做什么

- 先跑后端编译和测试
- 再跑前端构建
- 最后做 5 组人工验收

### 建议命令

后端：

```powershell
mvn -q -pl regulation-service,regulation-operation-service,complaint-service,warning-service -am test
```

前端：

```powershell
cd food-web
npm run build
```

### 建议补的自动化测试

- `regulation-service`
  - 公众企业详情接口测试
  - 公告发布/下线权限测试
- `regulation-operation-service`
  - 企业检查记录只读权限测试
  - 连续不合格触发重点监管测试
- `complaint-service`
  - 投诉派单时限写入测试
  - 投诉反馈摘要/驳回原因返回测试

### 人工验收路径 1：企业公示

1. 公众登录
2. 进入企业公示列表
3. 打开某个已审核通过企业详情
4. 确认能看到企业状态和基础备案信息

### 人工验收路径 2：公告发布

1. 区域管理员登录
2. 新建一条公告并发布
3. 公众端刷新并查看公告列表
4. 下线公告后确认公众端消失

### 人工验收路径 3：企业检查记录

1. 执法员完成检查并生成检查记录
2. 企业登录
3. 进入检查记录页
4. 查看检查详情与问题项

### 人工验收路径 4：投诉反馈

1. 公众提交投诉
2. 区域管理员受理并派单
3. 执法员处理完成
4. 公众进入“我的投诉”查看反馈摘要或驳回原因

### 人工验收路径 5：重点监管联动

1. 对同一企业制造连续不合格记录
2. 检查企业是否自动变为 `KEY`
3. 查看 `enterprise_key_reason`
4. 监管端/公众端详情确认状态同步

### 验收标准

- 五条主线都能独立演示
- 监管端、企业端、公众端三端逻辑能串起来
- 当前系统更像“监管平台”，不再只是“表单录入系统”

### 建议提交

```text
test(p1): add smoke checks for public, complaint and risk linkage flows
```

---

## 12. P1 期间不要改的内容

为了防止边界再次漂移，P1 期间明确不动这些方向：

- 不新增 `食品档案/产品批次` 模块
- 不新增 `检测机构管理` 模块
- 不新增 `追溯二维码` 模块
- 不新增 `商城式企业展示页`
- 不新增 `系统管理员总控台` 的复杂能力
- 不新增 `query-service` 的综合大屏统计范围

如果你在开发过程中想继续加功能，先问自己一句：

> 这个东西会不会直接提升“公众公示、公告、企业检查记录、投诉反馈、重点监管联动”这五条线的完成度？

如果不会，就不要在 P1 做。

---

## 13. 建议提交顺序

推荐按下面的提交粒度推进：

1. `feat(public): add public enterprise list and detail flow`
2. `feat(public): add bulletin publish and public display flow`
3. `feat(enterprise): add read-only inspection record flow`
4. `feat(complaint): add deadline and structured feedback fields`
5. `feat(risk): link inspections and complaints to key-supervision status`
6. `test(p1): add smoke checks for public, complaint and risk linkage flows`

---

## 14. P1 完成后的状态判断

如果 P1 做完，项目应该达到下面这个状态：

- 公众端不再只有投诉入口，而是具备基础公示能力
- 企业端不再只是被动整改，而能看到监管检查结果
- 投诉链路不再只有状态变化，而有结果反馈
- 企业重点监管状态不再是空标签，而有真实业务触发来源
- 答辩时可以清晰讲出三条主线：
  - `公众投诉与信息公示`
  - `监管检查与企业整改`
  - `风险联动与重点监管`

这时项目就已经具备进入 P2 的基础。
