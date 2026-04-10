# P2 后续整改项收口文档

版本：v0.1  
日期：2026-04-07  
适用阶段：阶段 C / 阶段 D  
文档性质：基于当前项目代码、P2 验收结论和最新范围决策形成的后续整改项收口说明

---

## 1. 当前理解结论

### 1.1 当前事实

- P0 已完成：前端空入口、网关 fail-open、文件上传权限、`query-service` 职责收束均已有验收记录。
- P1 已完成：企业公示、公告发布与展示、企业检查记录、投诉反馈、重点监管联动均已有验收记录。
- P2 业务增强主线已完成：产品档案、抽检任务与结果、公示与风险联动、监管概览统计均已有代码和测试证据。
- P2-5 配置治理未执行，且当前决策为不再推进。
- 原纠偏方案中的 P3 低优先级/可放弃项，当前决策为整体放弃。

### 1.2 当前阶段定位

当前不再进入新的功能建设阶段，而是进入“交付收口与演示稳定”阶段。

阶段 C 的核心不是继续找新功能，而是明确：

- 本轮收口目标是什么
- 哪些历史待办被正式放弃或降级
- 后续只允许做哪些低风险收尾项
- 当前系统如何被验收和讲述

阶段 D 的核心不是继续改业务代码，而是把上述边界落成文档和验收口径。

---

## 2. 证据依据

### 2.1 文档依据

- `docs/food-safety-platform-rectification-plan.md`
  - P0/P1/P2/P3 优先级路线图
  - 开发落地任务清单
  - “不要再把项目做大”的最终建议
- `docs/p0-acceptance-report.md`
  - P0 已完成验收
- `docs/p1-acceptance-report.md`
  - P1 五条主线已完成验收
- `docs/p2-acceptance-report.md`
  - P2-1 到 P2-4 已具备验收证据
  - P2-5 配置治理未纳入通过范围
- `docs/p2-execution-checklist.md`
  - 原始 P2 范围包含 P2-5 配置治理
  - 原始 P2 边界明确不做检测机构、实验室、追溯、召回、案件处罚、大屏等扩展

### 2.2 代码依据

- `regulation-service`
  - 产品档案：`ProductController`、`ProductServiceImpl`、`food_product`
  - 公告与公示：`BulletinManageController`、`PublicBulletinController`、`PublicEnterpriseController`
  - 重点监管：`EnterpriseKeyReasonServiceImpl`、`enterprise_key_reason`
- `regulation-operation-service`
  - 检查整改：`InspectionTaskController`、`InspectionRecordController`、`RectificationController`
  - 抽检闭环：`SamplingTaskController`、`PublicSamplingResultController`、`SamplingTaskServiceImpl`
  - 预警事件：`WarningEventOutboxServiceImpl`
- `complaint-service`
  - 投诉提交、受理、派发、处理、驳回、反馈字段
- `warning-service`
  - 预警事件、处理、升级、归档、内部统计接口
- `query-service`
  - `SupervisionOverviewController`、`SupervisionOverviewQueryServiceImpl`
- `food-web`
  - 公众企业公示、公告、抽检结果
  - 企业产品档案、检查记录、整改任务
  - 监管抽检、投诉、预警、概览统计

---

## 3. 差距与问题

### 3.1 已接受的差距

当前明确接受以下差距，不再作为后续整改开发项：

| 差距 | 当前处理方式 | 原因 |
| --- | --- | --- |
| P2-5 配置治理未完成 | 放弃推进，仅保留风险说明 | 当前目标转为交付收口，不再扩大修改面 |
| P3 通用日志、复杂角色、区域交接等 | 整体放弃 | 偏离当前主业务闭环，收益低 |
| 全链路追溯、召回、检测机构、案件处罚 | 整体放弃 | 超出毕业设计最小可交付范围 |
| 完整在线录屏式联调 | 降级为建议项 | 当前已有自动化测试、构建与代码链路证据 |
| 生产级配置安全 | 不宣称已完成 | 默认 secret/token 仍存在，不能按生产级交付表述 |

### 3.2 仍需注意的问题

- 后续答辩或交付材料中不能宣称“P2 全量完成”。
- 后续答辩或交付材料中不能宣称“生产级安全配置已治理完成”。
- 后续如果继续运行演示环境，应将默认 secret/token 视为演示环境风险，而不是已解决问题。
- 后续所有改动必须避免重新打开 P3 或 P2-5 范围。

---

## 4. 建议推进项

当前建议推进项只有一类：交付收口项。

| 整改项 | 状态 | 优先级 | 是否开发代码 | 说明 |
| --- | --- | --- | --- | --- |
| 固化 P2 后续边界 | 本轮执行 | S0 | 否 | 新增本文档，避免后续继续把 P2-5/P3 当待办 |
| 更新 P2 验收口径 | 本轮执行 | S0 | 否 | 在 P2 验收报告中补充“P2-5/P3 不再推进”的最新决策 |
| 演示脚本整理 | 建议后续 | S1 | 否 | 围绕企业纳管、检查整改、投诉、抽检、公示、概览组织 |
| 演示数据准备 | 建议后续 | S1 | 可选 | 只准备必要演示数据，不新增功能 |
| 全链路手工验收 | 建议后续 | S1 | 否 | 拉起服务后按已有路径点击验证 |
| 缺陷修复 | 按需 | S2 | 仅限 bugfix | 只修阻断演示或测试的缺陷，不扩业务能力 |

说明：

- S0：本轮必须完成的文档收口项。
- S1：建议在答辩或交付前完成的演示准备项。
- S2：仅在发现真实缺陷时处理，不预设开发范围。

---

## 5. 实施方案

### 5.1 本次整改目标

本次整改目标是完成“范围决策与后续整改项文档化”：

- 明确 P2-5 不再推进。
- 明确 P3 整体放弃。
- 明确后续不再新增功能线。
- 明确后续只允许做文档、演示、验收、阻断性 bugfix。

### 5.2 本次不处理内容

本次不处理：

- 不清理默认 JWT secret。
- 不清理内部 token。
- 不清理 MinIO 默认凭据。
- 不新增配置校验类。
- 不新增审计日志。
- 不做追溯、召回、处罚、检测机构、复杂大屏。
- 不重构前端路由或微服务边界。

### 5.3 技术实施方式

本次以 Markdown 文档落地，不改业务代码。

改动点：

- 新增 `docs/post-p2-follow-up-rectification-items.md`
- 在 `docs/p2-acceptance-report.md` 末尾追加“最新范围决策附录”

### 5.4 兼容性处理

- 不修改 Java、Vue、SQL、配置文件。
- 不影响现有测试与构建。
- 不改变 P0/P1/P2-1 到 P2-4 的历史验收结论。
- 保留 `docs/p2-execution-checklist.md` 作为原始执行清单，不做回写，避免混淆“原计划”和“最新决策”。

---

## 6. 落地改动清单

| 文件 | 类型 | 改动内容 |
| --- | --- | --- |
| `docs/post-p2-follow-up-rectification-items.md` | 新增 | 固化阶段 C/D 后续整改项、范围边界、风险与验收口径 |
| `docs/p2-acceptance-report.md` | 修改 | 追加最新范围决策附录，说明 P2-5 不再推进、P3 放弃 |

---

## 7. 验证方案

### 7.1 文档验证

- 检查新增文档是否存在。
- 检查 P2 验收报告是否追加最新范围决策。
- 检查文档中是否明确区分：
  - 已完成
  - 放弃
  - 建议后续演示准备
  - 风险接受

### 7.2 代码验证

本次不改业务代码，因此不强制跑全量测试。

如需做保守验证，可继续执行：

```powershell
mvn -q -pl regulation-service,regulation-operation-service,complaint-service,warning-service,query-service -am test
cd food-web
npm run build
```

---

## 8. 风险与注意事项

### 8.1 已接受风险

- 默认 JWT secret、内部 token、MinIO 默认凭据仍存在。
- 当前项目不能按生产安全配置完成态表述。
- P2 只能表述为“业务增强主线完成”，不能表述为“P2 原始清单全量完成”。

### 8.2 回滚点

本次仅改文档，回滚方式为：

- 删除 `docs/post-p2-follow-up-rectification-items.md`
- 移除 `docs/p2-acceptance-report.md` 末尾新增附录

### 8.3 后续约束

后续只允许进入以下工作：

- 答辩演示脚本
- 演示数据准备
- 手工联调记录
- 阻断性 bugfix
- 文档图表补充

不允许再进入：

- P2-5 配置治理
- P3 功能
- 追溯/召回/处罚/检测机构/大屏
- 前端路由重写
- 新增微服务拆分

---

## 9. 结论

当前项目后续整改路线正式收敛为：

> 保留 P0、P1、P2-1 到 P2-4 的业务整改成果；P2-5 不再推进；P3 整体放弃；后续只做交付、演示、验收和阻断性 bugfix。
