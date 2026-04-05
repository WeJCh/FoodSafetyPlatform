# query-service P0 职责边界说明

版本：V0.1  
日期：2026-03-19  
适用阶段：P0 收敛阶段  

---

## 1. 文档目的

本说明用于在 P0 阶段正式冻结 `query-service` 的职责边界，避免继续把该服务扩展成“大而全统计服务”或“为了微服务而微服务”的空壳模块。

本说明是当前代码现状的归纳，不是未来完整规划。

---

## 2. 当前项目事实

以下结论可直接由当前代码与资源证明：

### 2.1 当前对外接口只有预警统计

`query-service` 当前只有一个核心业务控制器：

- `src/main/java/com/mortal/query/controller/WarningStatsController.java`

当前对外开放的接口仅包括：

- `/api/query/warnings/overview`
- `/api/query/warnings/trend`
- `/api/query/warnings/types`
- `/api/query/warnings/efficiency`

除此之外，仅存在健康检查接口：

- `src/main/java/com/mortal/query/controller/HealthController.java`

### 2.2 当前核心实现是“代理 warning-service + 作用域收口”

当前服务主逻辑由两部分组成：

1. `WarningStatsQueryServiceImpl`
   - 调用 `warning-service` 的内部统计接口
   - 做参数校验
   - 统一返回结果

2. `WarningStatsScopeService`
   - 根据当前用户身份补全统计范围
   - 区域管理员按辖区展开区域范围
   - 执法人员强制限定为“本人范围”

也就是说，当前 `query-service` 的真实定位不是“统计数据生产服务”，而是：

> 预警统计聚合与权限收口服务

### 2.3 当前三张 `stat_*` 表没有落入实际运行链路

`src/main/resources/sql/schema.sql` 中定义了：

- `stat_enterprise`
- `stat_inspection`
- `stat_complaint`

但当前 `src/main/java` 下没有对应的：

- Entity
- Mapper
- Service
- Controller
- 定时汇总任务
- 离线计算任务

结论：

- 这三张表当前属于预留结构
- 它们不是 P0 阶段页面展示和答辩口径的真实数据来源

### 2.4 当前测试也聚焦预警统计

现有测试：

- `src/test/java/com/mortal/query/controller/WarningStatsControllerIntegrationTest.java`

测试覆盖内容主要是：

- warning internal token 透传
- 监管辖区范围展开
- 参数校验
- warning 统计接口调用链

这进一步证明：当前 `query-service` 的有效业务面就是预警统计，不是综合统计中心。

---

## 3. P0 阶段正式职责定义

P0 阶段，`query-service` 的职责严格限定为：

1. 预警统计查询聚合
2. 预警统计权限范围收口
3. 预警统计接口日志记录
4. 预警统计接口的轻量扩展点预留
   - 例如本地缓存开关

P0 阶段，`query-service` 明确不负责：

1. 企业统计数据生产
2. 检查统计数据生产
3. 投诉统计数据生产
4. 通用 BI 报表
5. 大屏指标中心
6. 跨服务离线汇总任务
7. 综合查询平台

---

## 4. P0 不做清单

为了防止继续漂移，P0 阶段禁止在 `query-service` 中新增以下内容：

- 新的企业统计接口
- 新的检查统计接口
- 新的投诉统计接口
- 读取 `stat_enterprise/stat_inspection/stat_complaint` 的业务代码
- 定时汇总任务
- 本地假数据统计
- 只有页面展示意义、没有业务链路支撑的指标接口

一句话原则：

> P0 阶段，`query-service` 不扩业务面，只收敛已有 warning stats 能力。

---

## 5. `stat_*` 表状态说明

当前三张 `stat_*` 表处理策略如下：

| 表名 | 当前状态 | P0 处理方式 |
| --- | --- | --- |
| `stat_enterprise` | 预留表 | 保留结构，不作为运行数据来源 |
| `stat_inspection` | 预留表 | 保留结构，不作为运行数据来源 |
| `stat_complaint` | 预留表 | 保留结构，不作为运行数据来源 |

保留原因：

- 可以作为后续 P2 以后统计汇总的落点
- 当前不需要在 P0 阶段删除，避免无谓改动

不使用原因：

- 没有汇总写入链路
- 没有读取链路
- 没有口径定义的闭环

---

## 6. 为什么 P0 必须收敛到这个程度

原因不是技术能力不够，而是当前项目更需要“主线清晰”。

如果 P0 阶段继续扩展 `query-service`，会产生三个问题：

1. 统计接口越来越多，但数据基础依然不完整
2. `query-service` 会进一步变成“为了微服务而微服务”的示意模块
3. 毕业设计答辩时很难解释为什么有多张统计表，但真正可验证的只有预警统计

所以 P0 阶段的正确做法不是“补更多接口”，而是：

- 把现有 warning 统计讲清楚
- 把作用域控制讲清楚
- 把服务定位讲清楚

---

## 7. 当前答辩口径建议

如果在答辩中被问到 `query-service` 的定位，建议统一表述为：

> `query-service` 在当前阶段承担监管看板的查询聚合职责，P0 版本聚焦预警统计数据，不直接生产企业、检查、投诉三类统计数据；预留统计表仅作为后续离线汇总扩展结构，当前并未投入运行链路。

---

## 8. P1/P2 之后才能考虑的扩展

只有在以下条件都满足后，才建议扩展 `query-service`：

1. 企业、检查、投诉主业务闭环稳定
2. 统计口径已定义
3. 汇总写入链路已实现
4. 页面确实需要这些统计，而不是为了展示而展示

在这些条件未满足前，不建议继续扩展 `query-service`。

---

## 9. 本文档的执行结论

P0 阶段对 `query-service` 的执行结论只有一句话：

> 保留现有预警统计聚合能力，不新增综合统计能力，不启用 `stat_*` 表作为运行数据来源。
