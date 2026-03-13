# 预警统计 P0 联调冒烟清单

## 1. 启动前置

- 启动网关、`regulation-service`、`warning-service`、`query-service`
- 确认 `warning-service` 与 `query-service` 使用同一个环境变量 `WARNING_INTERNAL_TOKEN`

示例（PowerShell）：

```powershell
$env:WARNING_INTERNAL_TOKEN="warning-internal-token"
```

## 2. 健康检查

```text
GET http://localhost:8083/api/health
GET http://localhost:8084/api/health
```

## 3. 统计接口验证（query-service）

时间参数格式统一：

`yyyy-MM-dd'T'HH:mm:ss`（UTC+8 本地时间）

```text
GET /api/query/warnings/overview?startTime=2026-03-10T00:00:00&endTime=2026-03-13T23:59:59
GET /api/query/warnings/trend?trendDays=7
GET /api/query/warnings/types?topN=5
GET /api/query/warnings/efficiency?overdueHours=24
```

## 4. 数据口径（本期固定）

- 时间锚点：`firstOccurTime`
- 已处理完成：`RESOLVED + CLOSED`
- 超时待处理：`OPEN/PROCESSING` 且 `firstOccurTime <= now - overdueHours`
- 类型统计：按 `warningType` 原值分组，不做中文 label 映射

## 5. 与预警列表口径校对

使用同一筛选参数对比：

- 列表：`/api/regulation/warnings`
- 统计：`/api/query/warnings/*`

重点校对：

- `overview.totalCount` 是否等于列表总数
- `overview.completedCount` 是否等于 `overview.resolvedCount + overview.closedCount`
- `overview.statusDistribution` 各状态汇总是否等于列表分组数
- `types` 是否与列表 `warningType` 分组一致
- `efficiency.pendingCount` 是否等于 `OPEN + PROCESSING`

## 6. P1 权限范围校验（新增）

- 区域管理员：
  - 不传 `regionId/regionIds` 时，系统自动按“我的辖区”统计。
- 执法员：
  - 不传 `ownerRegulatorId` 时，系统自动按“我本人”统计。
  - 传 `regionId/regionIds` 时应返回 `403`（防止越权）。

## 7. P2 可运维（轻量）

- 关键日志（`query-service`）：
  - 接口耗时 `elapsedMs`
  - 筛选参数摘要 `filters`
  - 返回条数 `resultSize`
- 预留缓存开关（当前默认关闭）：

```yaml
query:
  warning-stats:
    cache:
      enabled: false
      ttl-seconds: 30
```
