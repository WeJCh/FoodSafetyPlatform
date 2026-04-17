# 食品安全监管平台 Redis 应用实现方案

## 1. 项目现状分析

### 1.1 微服务结构与职责

基于代码审查，当前平台由以下核心服务组成：

- `gateway-service`
  - 统一入口、JWT 签名校验、调用 `user-service` 的 `/api/auth/introspect` 做登录态校验
  - 已有 Sentinel 网关限流，但属于单实例内存规则，不具备 Redis 级别的分布式共享能力
- `user-service`
  - 用户注册、登录、登出、Token 校验、角色绑定
  - 当前 JWT 黑名单在 [`user-service/src/main/java/com/mortal/user/util/TokenUtil.java`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/user-service/src/main/java/com/mortal/user/util/TokenUtil.java) 中使用 `ConcurrentHashMap` 本地保存，天然不支持多实例
- `regulation-service`
  - 企业档案、监管员档案、区域树、产品、公告、内部主数据接口
  - 对 `regulation-operation-service`、`complaint-service`、`query-service` 提供大量内部查询
- `regulation-operation-service`
  - 检查任务、抽检任务、整改任务、整改 SLA 扫描、预警 outbox 重试
  - 任务流里存在“提交即产生副作用”的操作
- `warning-service`
  - 预警事件幂等入库、预警列表/详情、统计聚合、自动升级、自动归档
  - 统计接口目前是查出记录后在 JVM 内聚合
- `query-service`
  - 监管看板聚合层
  - `/api/query/supervision/overview` 串行调用企业、检查抽检、投诉、预警 4 个下游
  - `WarningStatsScopeService` 为监管员展开区域树时会反复调用 `regulation-service`
- `complaint-service`
  - 投诉提交流转、投诉统计、投诉详情
  - 详情和列表会频繁反查企业/监管员名称

### 1.2 主要业务链路

- 登录链路：前端 -> `gateway-service` -> `user-service/login`；后续每个受保护请求在网关先验 JWT，再调 `user-service/introspect`
- 监管看板链路：前端 -> `query-service` -> `warning-service` / `regulation-service` / `regulation-operation-service` / `complaint-service`
- 任务执行链路：
  - 检查任务提交 -> 创建 `inspection_record` / `inspection_item` -> 可能创建 `rectification_task`
  - 抽检结果提交 -> 创建 `sampling_result` -> 标记重点企业 -> 写 `warning_event_outbox`
  - 整改 SLA 定时扫描 -> 写整改日志 + 推送预警事件
- 公共查询链路：
  - 公众企业公示、公众公告、公众抽检结果

### 1.3 当前真实问题

#### 1.3.1 登录态与鉴权

- 网关每次鉴权都调 [`gateway-service/src/main/java/com/mortal/gateway/filter/JwtAuthFilter.java`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/gateway-service/src/main/java/com/mortal/gateway/filter/JwtAuthFilter.java)
  - `POST http://user-service/api/auth/introspect`
- [`user-service/src/main/java/com/mortal/user/service/impl/AuthServiceImpl.java`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/user-service/src/main/java/com/mortal/user/service/impl/AuthServiceImpl.java)
  - 每次 `introspect` 都查 `user`、`user_role`、`role`
- [`user-service/src/main/java/com/mortal/user/util/TokenUtil.java`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/user-service/src/main/java/com/mortal/user/util/TokenUtil.java)
  - `invalidate` 仅写本地内存集合，多实例后登出失效

#### 1.3.2 看板与统计

- [`query-service/src/main/java/com/mortal/query/service/impl/SupervisionOverviewQueryServiceImpl.java`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/query-service/src/main/java/com/mortal/query/service/impl/SupervisionOverviewQueryServiceImpl.java)
  - 串行调用 4 个下游，单次请求放大为 4 次远程调用
- [`warning-service/src/main/java/com/mortal/warning/service/impl/WarningStatsServiceImpl.java`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/warning-service/src/main/java/com/mortal/warning/service/impl/WarningStatsServiceImpl.java)
  - 统计先 `selectList` 全量拉取满足条件的预警，再在内存做分组聚合

#### 1.3.3 主数据重复读取

- `regulation-operation-service` 的 [`OperationMasterDataSupport`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/regulation-operation-service/src/main/java/com/mortal/regulation/operation/support/OperationMasterDataSupport.java)
  - 高频调用企业详情、产品摘要、监管员身份、监管范围
- `complaint-service` 的 [`ComplaintDataSupport`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/complaint-service/src/main/java/com/mortal/complaint/application/ComplaintDataSupport.java)
  - 列表/详情重复调用企业、监管员内部接口
- `query-service` 的 [`WarningStatsScopeService`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/query-service/src/main/java/com/mortal/query/service/WarningStatsScopeService.java)
  - 每次统计都可能递归拉区域子节点

#### 1.3.4 定时任务多实例重复执行

- `regulation-operation-service`
  - [`RectificationSlaScheduler`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/regulation-operation-service/src/main/java/com/mortal/regulation/operation/service/scheduler/RectificationSlaScheduler.java)
  - [`WarningEventOutboxRetryScheduler`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/regulation-operation-service/src/main/java/com/mortal/regulation/operation/service/scheduler/WarningEventOutboxRetryScheduler.java)
- `warning-service`
  - [`WarningEscalationScheduler`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/warning-service/src/main/java/com/mortal/warning/service/scheduler/WarningEscalationScheduler.java)
  - [`WarningArchiveScheduler`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/warning-service/src/main/java/com/mortal/warning/service/scheduler/WarningArchiveScheduler.java)

这些任务当前没有分布式协调，多实例会重复扫表。

### 1.4 为什么需要 Redis

本项目需要 Redis，但只在以下高价值场景引入：

- 统一登录态黑名单与鉴权旁路缓存
- 高频只读主数据缓存
- 看板/统计短 TTL 聚合缓存
- 多实例定时任务分布式锁
- 少量高副作用提交接口的分布式幂等锁
- 分布式限流

不需要把 Redis 扩展成“第二数据库”。

## 2. Redis 在本项目中的职责定位

### 2.1 承担的职责

- 登录态共享
  - Token 黑名单
  - 在线会话索引
  - introspect 结果缓存
- 只读或低频变更主数据缓存
  - 区域树
  - 企业内部摘要/详情
  - 监管员摘要/身份/范围
  - 产品摘要/详情
  - 公众公告、公众企业、公众抽检公示
- 聚合统计缓存
  - 预警统计四类接口
  - 监管总览接口
- 分布式锁
  - 定时任务单实例执行
  - 检查/抽检/整改等高副作用提交流程
- 分布式限流
  - 登录
  - 公共投诉提交
  - 文件预签名
  - 看板高频接口

### 2.2 明确不承担的职责

- 不作为投诉、检查、抽检、整改、预警主数据存储
- 不替代当前 MySQL outbox、状态表、日志表
- 不实现整改/预警的延迟队列主通道
- 不缓存高维度、强一致要求、频繁变化的分页列表
- 不用于业务编号生成

### 2.3 总体设计原则

- 统一技术栈：
  - 缓存与计数：`StringRedisTemplate`
  - 分布式锁：`Redisson`
- 统一缓存模式：
  - 读路径采用 cache-aside
  - 写路径优先删缓存，不做复杂双写
- 统一序列化：
  - value 统一 JSON
- 统一过期策略：
  - 主数据 10 分钟到 24 小时
  - 公示数据 1 分钟到 5 分钟
  - 统计数据 20 到 30 秒
  - 鉴权缓存不超过 token 剩余寿命
- 统一隔离：
  - Key 必须带环境前缀

## 3. 最终落地清单

### 3.1 `user-service` + `gateway-service`：登录态、Token 黑名单、introspect 缓存

- 使用位置
  - `user-service` 的登录、登出、verify、introspect
  - `gateway-service` 的 `JwtAuthFilter`
- 使用原因
  - 当前登出只在单机内存失效
  - 当前每次请求都要打 `user-service` 查库
- Redis 用途
  - Token 黑名单
  - 会话索引
  - introspect 结果缓存
- 数据结构
  - `String`
  - `Set`
- Key
  - `fsp:{env}:auth:blacklist:{jti}`
  - `fsp:{env}:auth:session:{jti}`
  - `fsp:{env}:auth:user-jtis:{userId}`
  - `fsp:{env}:auth:introspect:{tokenSha256}`
- Value
  - blacklist：`1`
  - session：`{"userId":1,"username":"...","userType":"REGULATOR","roles":["REGULATOR_ADMIN"],"exp":...}`
  - user-jtis：`Set<jti>`
  - introspect：`AuthIntrospectVO` JSON
- TTL
  - 与 token 剩余过期时间保持一致，上限 7200 秒
  - introspect 缓存取 `min(300秒, token剩余TTL)`
- 更新策略
  - 登录成功后写 `session`、`user-jtis`
  - 首次 introspect 查库后写 `introspect`
- 删除/失效
  - 登出：写 blacklist，删 session/introspect，从 user-jtis 移除 jti
  - 用户禁用/删除/角色变更：遍历 `user-jtis` 将全部 jti 拉黑并删除缓存
- 一致性
  - 角色和状态以 `user-service` 为准
  - Redis 只是旁路缓存，不改变判定逻辑
- 并发控制
  - 无需分布式锁
- 是否需要分布式锁
  - 否
- 改造位置
  - `TokenUtil`
  - `AuthServiceImpl`
  - `UserServiceImpl`
  - `RoleServiceImpl`
  - `JwtAuthFilter`

### 3.2 `gateway-service` + 业务服务：分布式限流

- 使用位置
  - `/api/auth/login`
  - `/api/complaints/public`
  - `/api/files/presign`
  - `/api/query/warnings/*`
  - `/api/query/supervision/overview`
- 使用原因
  - 现有 Sentinel 为单实例规则，多实例下阈值不共享
- Redis 用途
  - 固定窗口计数限流
- 数据结构
  - `String`
- Key
  - `fsp:{env}:rl:login:ip:{ip}`
  - `fsp:{env}:rl:login:user:{username}`
  - `fsp:{env}:rl:complaint-submit:user:{userId}`
  - `fsp:{env}:rl:presign:user:{userId}:{bizType}`
  - `fsp:{env}:rl:query:user:{userId}:{api}`
- TTL 与阈值
  - 登录 IP：`10次/60秒`
  - 登录用户名：`5次/600秒`
  - 投诉提交：`5次/600秒`
  - 预签名：`20次/60秒`
  - 看板接口：`120次/60秒/用户`
- 更新策略
  - `INCR` 首次后 `EXPIRE`
- 删除/失效
  - 自动过期
- 一致性
  - 无一致性要求
- 并发控制
  - Lua 保证 `INCR + EXPIRE` 原子
- 是否需要分布式锁
  - 否
- 改造位置
  - `gateway-service` 新增 `RedisRateLimitFilter`
  - `complaint-service` 控制器前置限流拦截
  - `regulation-service` `FileController`

### 3.3 `regulation-service`：区域树缓存

- 使用位置
  - `/api/regulation/regions`
  - `/api/regulation/regions/{id}/path`
  - `query-service` 的区域递归展开
  - 企业档案、监管员档案里的区域路径
- 使用原因
  - 区域树几乎不变，但被大量重复读取
- Redis 用途
  - 区域 children/path 缓存
- 数据结构
  - `String`
- Key
  - `fsp:{env}:reg:region:children:{parentId|root}`
  - `fsp:{env}:reg:region:path:{regionId}`
- Value
  - `List<RegionVO>` JSON
- TTL
  - `24h`
- 更新策略
  - 首次查询回填
- 删除/失效
  - 新增区域后删除父节点 children key
  - 删除对应 path key
  - 如改为支持区域编辑，删除该节点与祖先 path/children key
- 一致性
  - 写后删缓存
- 并发控制
  - 单飞回填，避免同一 key 并发回源
- 是否需要分布式锁
  - 否
- 改造位置
  - `RegionServiceImpl`
  - `EnterpriseProfileServiceImpl#resolveRegionPath`
  - `RegulatorProfileServiceImpl#resolveRegionIds`
  - `InternalEnterpriseStatsController#collectRegionIds`
  - `InternalRegulatorController#collectRegionIds`

### 3.4 `regulation-service`：企业/监管员/产品内部主数据缓存

- 使用位置
  - 内部接口：
    - `/api/internal/regulation/enterprises/{id}`
    - `/api/internal/regulation/enterprises/by-user/{userId}`
    - `/api/internal/regulation/enterprises/summaries`
    - `/api/internal/regulation/regulators/by-user/{userId}`
    - `/api/internal/regulation/regulators/{id}/identity`
    - `/api/internal/regulation/regulators/{id}`
    - `/api/internal/regulation/regulators/{id}/scope-region-ids`
    - `/api/internal/regulation/regulators/{id}/scope-enterprise-ids`
    - `/api/internal/regulation/products/{id}`
    - `/api/internal/regulation/products/summaries`
- 使用原因
  - `regulation-operation-service`、`complaint-service`、`query-service` 高频依赖这些主数据
- Redis 用途
  - 主数据详情缓存
  - 监管范围缓存
- 数据结构
  - `String`
- Key
  - `fsp:{env}:reg:enterprise:detail:{enterpriseId}`
  - `fsp:{env}:reg:enterprise:by-user:{userId}`
  - `fsp:{env}:reg:enterprise:summary:{enterpriseId}`
  - `fsp:{env}:reg:regulator:identity:{regulatorId}`
  - `fsp:{env}:reg:regulator:by-user:{userId}`
  - `fsp:{env}:reg:regulator:summary:{regulatorId}`
  - `fsp:{env}:reg:regulator:scope-region:{regulatorId}`
  - `fsp:{env}:reg:regulator:scope-enterprise:{regulatorId}`
  - `fsp:{env}:reg:product:detail:{productId}`
  - `fsp:{env}:reg:product:summary:{productId}`
- Value
  - 各 VO JSON
- TTL
  - 企业/监管员/产品：`10m`
  - scope：`10m`
- 更新策略
  - 读时回填
- 删除/失效
  - 企业档案提交/审批/删除时删除企业 detail/by-user/summary 及相关 public key
  - 监管员档案变更/状态变更/删除时删除监管员 identity/by-user/summary/scope
  - 产品新增/修改时删除 product detail/summary
- 一致性
  - 写后删缓存
- 并发控制
  - 回填锁 `tryLock 1s`
- 是否需要分布式锁
  - 仅缓存回填锁，不做业务锁
- 改造位置
  - `InternalEnterpriseController`
  - `InternalRegulatorController`
  - `InternalProductController`
  - `EnterpriseProfileServiceImpl`
  - `RegulatorProfileServiceImpl`
  - `ProductServiceImpl`

### 3.5 `warning-service`：预警统计缓存

- 使用位置
  - `/api/warning/internal/stats/overview`
  - `/api/warning/internal/stats/trend`
  - `/api/warning/internal/stats/types`
  - `/api/warning/internal/stats/efficiency`
- 使用原因
  - 当前统计是全量查记录后在 JVM 聚合，计算成本高
- Redis 用途
  - 统计结果短 TTL 缓存
- 数据结构
  - `String`
- Key
  - `fsp:{env}:warn:stats:overview:{queryHash}`
  - `fsp:{env}:warn:stats:trend:{queryHash}`
  - `fsp:{env}:warn:stats:types:{queryHash}`
  - `fsp:{env}:warn:stats:efficiency:{queryHash}`
- Value
  - 对应 VO / List JSON
- TTL
  - `30s`
- 更新策略
  - 首次查询回填
- 删除/失效
  - 预警创建、合并、分派、处理、解决、自动升级、自动归档后，`INCR` 一个版本 key
  - 统计 key 带版本号，避免按前缀批量删
- 一致性
  - 采用“版本号 + 短 TTL”，保证实现简单且足够新
- 并发控制
  - 热点 key 回填锁 3 秒
- 是否需要分布式锁
  - 仅缓存回填锁
- 改造位置
  - `WarningStatsServiceImpl`
  - `WarningEventServiceImpl`
  - `WarningEscalationScheduler`
  - `WarningArchiveScheduler`

### 3.6 `query-service`：监管总览缓存与监管统计作用域缓存

- 使用位置
  - `/api/query/supervision/overview`
  - `WarningStatsScopeService`
- 使用原因
  - 总览接口串行调用 4 个下游
  - 作用域展开会递归拉区域
- Redis 用途
  - 聚合结果缓存
  - 管理员作用域区域集合缓存
  - 当前监管员 profile 缓存
- 数据结构
  - `String`
- Key
  - `fsp:{env}:query:supervision:overview:{queryHash}`
  - `fsp:{env}:query:scope:profile:{userId}`
  - `fsp:{env}:query:scope:region-set:{regulatorId}`
- Value
  - `SupervisionOverviewVO`
  - `RegulatorProfileVO`
  - `Set<Long>` JSON
- TTL
  - 总览：`20s`
  - profile：`10m`
  - region-set：`10m`
- 更新策略
  - 查询回填
- 删除/失效
  - 监管员档案变更时删除 profile/region-set
  - 总览依赖下游版本号拼接 key，不主动逐项删
- 一致性
  - 短 TTL + 下游统计版本号
- 并发控制
  - 热 key 回填锁
- 是否需要分布式锁
  - 仅缓存回填锁
- 改造位置
  - `SupervisionOverviewQueryServiceImpl`
  - `WarningStatsScopeService`

### 3.7 `regulation-service` + `regulation-operation-service`：公众公示数据缓存

- 使用位置
  - `/api/regulation/public/enterprises`
  - `/api/regulation/public/enterprises/{id}`
  - `/api/regulation/public/bulletins`
  - `/api/regulation/public/bulletins/{id}`
  - `/api/regulation-operation/public/sampling/results`
  - `/api/regulation-operation/public/sampling/results/{id}`
- 使用原因
  - 前台公示典型高读低写
- Redis 用途
  - 公众列表与详情缓存
- 数据结构
  - `String`
- Key
  - `fsp:{env}:public:enterprise:list:v{ver}:{queryHash}`
  - `fsp:{env}:public:enterprise:detail:{id}`
  - `fsp:{env}:public:bulletin:list:v{ver}:{queryHash}`
  - `fsp:{env}:public:bulletin:detail:{id}`
  - `fsp:{env}:public:sampling:list:v{ver}:{queryHash}`
  - `fsp:{env}:public:sampling:detail:{id}`
  - `fsp:{env}:public:enterprise:list:ver`
  - `fsp:{env}:public:bulletin:list:ver`
  - `fsp:{env}:public:sampling:list:ver`
- Value
  - 分页结果 JSON / detail JSON
- TTL
  - 列表：`60s`
  - 详情：`300s`
  - version key：永久
- 更新策略
  - 读时回填
- 删除/失效
  - 企业审批通过/删除：删 detail，`INCR enterprise list ver`
  - 公告发布/下线/编辑：删 detail，`INCR bulletin list ver`
  - 抽检结果发布/下线：删 detail，`INCR sampling list ver`
- 一致性
  - version key + 写后删 detail
- 并发控制
  - 热点详情回填锁
- 是否需要分布式锁
  - 否
- 改造位置
  - `EnterpriseProfileServiceImpl#listPublic/getPublicById/approve/reject/delete`
  - `BulletinServiceImpl#listPublic/getPublicDetail/create/update/publish/offline`
  - `SamplingTaskServiceImpl#listPublicResults/getPublicResultDetail/publishResult/offlineResult`

### 3.8 `regulation-operation-service`、`complaint-service`、`warning-service`：高副作用状态流转锁

- 使用位置
  - 检查任务提交
  - 抽检结果提交
  - 企业整改提交
  - 整改复核
  - 预警处理/分派/解决
  - 投诉受理/分派/处理/驳回
- 使用原因
  - 当前大量流程为“先查状态，再更新，再写副作用”
  - 多实例下双击/重复提交会导致重复写日志、重复生成整改、重复写 outbox
- Redis 用途
  - 业务幂等锁
- 数据结构
  - Redisson `RLock`
- Lock Key
  - `fsp:{env}:lock:inspection-submit:{taskId}`
  - `fsp:{env}:lock:sampling-submit:{taskId}`
  - `fsp:{env}:lock:rectification-submit:{rectificationId}`
  - `fsp:{env}:lock:rectification-review:{rectificationId}`
  - `fsp:{env}:lock:warning-action:{warningId}`
  - `fsp:{env}:lock:complaint-action:{complaintId}`
- 锁时长
  - `15s`
- 获取方式
  - `tryLock(0, 15s)`
- 删除/释放
  - `finally unlock`
- 一致性
  - 锁只做串行化，最终状态仍以数据库事务为准
- 并发控制
  - 锁内再保留原有状态检查和唯一键校验
- 是否需要分布式锁
  - 是
- 改造位置
  - `InspectionTaskServiceImpl#submitTask`
  - `SamplingTaskServiceImpl#submitResult`
  - `RectificationServiceImpl#submitMy`
  - `RectificationServiceImpl#review`
  - `WarningEventServiceImpl#processWarningAction/assignWarning`
  - `ComplaintCommandService#accept/assign/startProcess/handle/reject`

### 3.9 定时任务单实例执行锁

- 使用位置
  - `RectificationSlaScheduler`
  - `WarningEventOutboxRetryScheduler`
  - `WarningEscalationScheduler`
  - `WarningArchiveScheduler`
- 使用原因
  - 当前多实例会重复扫表
- Redis 用途
  - 调度抢占锁
- 数据结构
  - Redisson `RLock`
- Lock Key
  - `fsp:{env}:lock:scheduler:reg-op:rectification-sla`
  - `fsp:{env}:lock:scheduler:reg-op:warning-outbox-retry`
  - `fsp:{env}:lock:scheduler:warning:escalation`
  - `fsp:{env}:lock:scheduler:warning:archive`
- 锁时长
  - `scan周期 * 0.8` 与 `2 * 单次任务预估时长` 取较大值
  - 直接配置为：
    - rectification-sla：`8m`
    - outbox-retry：`60s`
    - warning-escalation：`5m`
    - warning-archive：`15m`
- 获取方式
  - `tryLock(0, leaseTime)`
  - 获取不到直接返回
- 是否需要分布式锁
  - 是
- 改造位置
  - 4 个 scheduler 开始处

## 4. 详细实现方案

### 4.1 登录鉴权 Redis 化

#### 当前现状

- JWT 本身包含 `roles`
- 网关仍然每次调用 `/api/auth/introspect`
- 登出失效只在当前 JVM 内存生效

#### 存在问题

- 多实例下登出不一致
- `introspect` 高频查库
- 用户禁用/角色调整后无法快速全局失效

#### 为什么适合 Redis

- 登录态是典型共享临时状态
- 数据量小，生命周期明确
- 访问频率极高

#### 具体改造步骤

1. 在 [`TokenUtil`](/d:/JAVA/FoodSafetyPlatform/food-safety-supervision-platform/user-service/src/main/java/com/mortal/user/util/TokenUtil.java) 增加 `jti` claim，并提供：
   - `getJti(token)`
   - `getExpireAt(token)`
   - `getRemainingSeconds(token)`
2. 新增 `AuthRedisService`
   - `saveSession(jti, session, ttl)`
   - `blacklist(jti, ttl)`
   - `isBlacklisted(jti)`
   - `cacheIntrospect(tokenHash, vo, ttl)`
   - `getCachedIntrospect(tokenHash)`
   - `bindUserJti(userId, jti, ttl)`
   - `invalidateUserAllSessions(userId)`
3. `login`
   - 生成 token 后写 `session`、`user-jtis`
4. `logout`
   - 解析 jti 和剩余 TTL
   - 写 blacklist
   - 删除 session/introspect
5. `introspect`
   - 先查 blacklist
   - 再查 introspect cache
   - 未命中才走数据库
6. 用户状态变更、删除、角色绑定后统一调用 `invalidateUserAllSessions(userId)`

#### 伪代码

```java
public AuthIntrospectVO introspect(String token) {
    String jti = tokenUtil.getJti(token);
    if (authRedisService.isBlacklisted(jti)) {
        return invalidVo();
    }
    String tokenHash = sha256(token);
    AuthIntrospectVO cached = authRedisService.getCachedIntrospect(tokenHash);
    if (cached != null) {
        return cached;
    }
    AuthIntrospectVO latest = loadFromDb(token);
    long ttl = Math.min(300, tokenUtil.getRemainingSeconds(token));
    authRedisService.cacheIntrospect(tokenHash, latest, ttl);
    return latest;
}
```

#### 调用流程

- 请求到网关
- 网关验签
- 网关调用 `user-service/introspect`
- `user-service` 优先走 Redis
- 命中直接返回，未命中再查库

#### 注意事项

- `gateway.auth.introspect-fail-open` 维持默认 `false`
- 这部分属于安全链路，Redis 异常时不要放行

### 4.2 分布式限流

#### 当前现状

- 网关已有 Sentinel 限流，但规则存在于网关实例内存
- 代码里没有验证码、短信发送、真实导出任务

#### 存在问题

- 多实例后总阈值会被实例数放大
- 登录、投诉提交、文件预签名仍缺少明确的业务级频控

#### 为什么适合 Redis

- 限流本质是全局共享计数
- Redis 实现成本低

#### 具体改造步骤

1. `gateway-service` 新增 `RedisRateLimitFilter`
2. 登录接口按 IP + 用户名双维度限流
3. 在 `complaint-service` 的 `PublicComplaintController#submit` 前加用户级限流
4. 在 `regulation-service` 的 `FileController#presign` 前加用户级限流
5. 在 `query-service` 控制器前加用户级限流

#### 伪代码

```java
boolean allowed = redisRateLimiter.allow(key, limit, windowSeconds);
if (!allowed) {
    throw new TooManyRequestsException("too many requests");
}
```

#### 注意事项

- Redis 限流与 Sentinel 并存
- Sentinel 保留为网关粗粒度保护
- Redis 限流负责多实例精确限流

### 4.3 区域树缓存

#### 当前代码现状

- 区域 children/path 查询全部直查 MySQL
- 多个类自己递归展开区域树

#### 存在问题

- 相同区域层级被重复读取
- 监管统计作用域展开反复走远程 + 数据库

#### 具体改造步骤

1. 在 `regulation-service` 新增 `RegionCacheService`
2. `listByParentId(parentId)` 先查 `children` key
3. `getPath(id)` 先查 `path` key
4. `create` 后删除父 children key

#### 注意事项

- 区域树变更极少，24h TTL 合理
- 不需要全量预热

### 4.4 主数据内部接口缓存

#### 当前代码现状

- `InternalEnterpriseController` / `InternalRegulatorController` / `InternalProductController` 大量直接查表
- 多个下游服务反复调用

#### 存在问题

- 内部接口成为所有业务链路的放大器
- 监管范围查询重复递归

#### 具体改造步骤

1. 在 `regulation-service` 新增：
   - `EnterpriseMasterCacheService`
   - `RegulatorMasterCacheService`
   - `ProductMasterCacheService`
2. internal controller 先查缓存，未命中查库并写缓存
3. 企业/监管员/产品写接口统一删除对应 key

#### 注意事项

- 不缓存“按关键字搜索的 ID 列表”，只缓存单实体和 scope
- 搜索结果受关键字影响大，收益不高

### 4.5 预警统计缓存

#### 当前代码现状

- `WarningStatsServiceImpl` 直接 `selectList` 后在内存聚合

#### 存在问题

- 数据量增长后会明显放大 GC 和 CPU
- 相同筛选条件被看板反复请求

#### 具体改造步骤

1. 将查询条件规范化为字符串
2. 计算 `queryHash`
3. 读取缓存
4. 未命中执行现有逻辑
5. 预警写路径统一 `INCR statsVersion`

#### 伪代码

```java
String version = redis.get("fsp:prod:warn:stats:ver");
String key = "fsp:prod:warn:stats:overview:v" + version + ":" + queryHash;
```

#### 注意事项

- TTL 只给 30 秒，不做长缓存
- 版本号只在预警状态真正变化时递增

### 4.6 监管总览缓存

#### 当前代码现状

- `SupervisionOverviewQueryServiceImpl#getOverview` 串行远程调用 4 个服务

#### 存在问题

- RT 叠加
- 下游任一慢查询都会放大整体延迟

#### 具体改造步骤

1. 在 `query-service` 计算作用域后的 `scopeHash`
2. 组合下游版本号：
   - enterprise stats version
   - operation stats version
   - complaint stats version
   - warning stats version
3. 使用 `overview` cache key
4. 未命中再调 4 个下游

#### 注意事项

- 这里不需要再缓存 warning trend/types/efficiency，直接复用 `warning-service` 统计缓存即可

### 4.7 公共公示缓存

#### 当前代码现状

- 公众企业、公告、抽检公示都是直接查库分页
- 写路径数量远小于读路径

#### 存在问题

- 适合缓存但当前没有任何旁路层

#### 具体改造步骤

1. 详情缓存按 ID 缓存 5 分钟
2. 列表缓存用 `version + queryHash`
3. 发布/下线/审批时只做：
   - 删除 detail
   - `INCR list version`

#### 注意事项

- 列表不要用模糊批量删除
- 一律用 version key

### 4.8 高副作用接口分布式锁

#### 当前代码现状

- 检查提交会创建检查记录、检查项、整改任务
- 抽检提交会创建结果、重点企业原因、预警 outbox
- 整改复核会写状态和日志
- 投诉/预警流程也都是“先查状态后更新”

#### 存在问题

- 多实例下重复点击会并发进入
- 当前很多流程没有乐观锁字段

#### 最终锁定点

- 必须加锁
  - `InspectionTaskServiceImpl#submitTask`
  - `SamplingTaskServiceImpl#submitResult`
  - `RectificationServiceImpl#submitMy`
  - `RectificationServiceImpl#review`
  - `WarningEventServiceImpl#assignWarning`
  - `WarningEventServiceImpl#processWarningAction`
  - `ComplaintCommandService#accept/assign/startProcess/handle/reject`

#### 不加锁但保留数据库唯一约束即可

- `RectificationServiceImpl#createFromInspection`
  - 已有 `uk_rectification_inspection`
- `WarningEventOutboxServiceImpl#ensurePendingEvent`
  - 已有 `uk_warning_outbox_event_key`

#### 伪代码

```java
RLock lock = redissonClient.getLock("fsp:prod:lock:sampling-submit:" + taskId);
if (!lock.tryLock(0, 15, TimeUnit.SECONDS)) {
    throw new IllegalStateException("request is processing");
}
try {
    return doSubmit(...);
} finally {
    lock.unlock();
}
```

### 4.9 定时任务分布式锁

#### 当前代码现状

- 4 个 scheduler 都是本地 `@Scheduled`

#### 存在问题

- 多实例重复扫描、重复写日志、重复推送

#### 具体改造步骤

1. 每个 scheduler 开始时先 `tryLock`
2. 没拿到锁直接返回
3. 保留现有事务和数据库幂等判断

#### 注意事项

- 锁时间必须大于任务平均执行时间
- 不要用永久锁

## 5. 不建议使用 Redis 的部分

以下部分明确不建议引入 Redis：

- 投诉、检查、抽检、整改、预警的分页列表缓存
  - 原因：筛选条件多、状态频繁变化、命中率低、一致性成本高
- `warning_event_outbox` 替换成 Redis Stream
  - 原因：当前 MySQL outbox + 重试调度已经完整，改造成本高于收益
- 整改/预警延迟队列改成 Redis 延迟队列
  - 原因：当前已存在 `RectificationSlaScheduler`、`WarningEscalationScheduler`、`WarningArchiveScheduler`
  - 最合适的改造是“给 scheduler 加分布式锁”，不是重写成延迟队列体系
- 验证码、短信验证码、短时票据
  - 原因：代码中不存在这类能力，不应为了 Redis 人为新增
- 导出任务状态跟踪
  - 原因：目前前后端只存在“导出名单/报表”的占位按钮，没有真实后端导出链路
- 业务编号生成
  - 原因：`complaint_no`、`task_no`、`warning_no` 目前仍是时间戳+随机数模式
  - 最终建议是补数据库唯一索引并在冲突时重试，不用 Redis 生成器
- 把 Redis 当成企业、投诉、预警主存
  - 原因：本项目是强事务业务，核心状态必须仍在 MySQL

## 6. Redis Key 设计总表

### 6.1 前缀规范

- 统一前缀：`fsp`
- 环境：`dev` / `test` / `prod`
- 结构：`fsp:{env}:{domain}:{subDomain}:{biz}`

### 6.2 环境隔离规范

- 示例
  - `fsp:dev:auth:blacklist:{jti}`
  - `fsp:prod:warn:stats:overview:{queryHash}`

### 6.3 服务隔离规范

- `auth`
- `reg`
- `reg-op`
- `warn`
- `query`
- `public`
- `lock`
- `rl`

### 6.4 典型 Key 示例

- `fsp:prod:auth:blacklist:1c3f...`
- `fsp:prod:auth:session:1c3f...`
- `fsp:prod:auth:user-jtis:1001`
- `fsp:prod:auth:introspect:5d4140...`
- `fsp:prod:rl:login:ip:10.0.0.1`
- `fsp:prod:reg:region:children:root`
- `fsp:prod:reg:region:path:330106`
- `fsp:prod:reg:enterprise:detail:2001`
- `fsp:prod:reg:regulator:scope-region:301`
- `fsp:prod:reg:product:summary:9001`
- `fsp:prod:warn:stats:overview:v12:8a2c...`
- `fsp:prod:query:supervision:overview:v5-4-7-12:913a...`
- `fsp:prod:public:bulletin:list:v18:4d2a...`
- `fsp:prod:lock:scheduler:warning:archive`
- `fsp:prod:lock:sampling-submit:501`

### 6.5 TTL 总表

| Key 类型 | TTL |
| --- | --- |
| auth blacklist | token 剩余 TTL |
| auth session | token 剩余 TTL |
| auth introspect | 300 秒以内 |
| rate limit counter | 60 秒 / 600 秒 |
| region children/path | 24 小时 |
| enterprise/regulator/product cache | 10 分钟 |
| regulator scope cache | 10 分钟 |
| warning stats cache | 30 秒 |
| supervision overview cache | 20 秒 |
| public list cache | 60 秒 |
| public detail cache | 300 秒 |
| scheduler/business lock | 15 秒到 15 分钟 |

## 7. 分布式锁与限流专项设计

### 7.1 分布式锁最终方案

- 技术选型
  - `Redisson`
- 原因
  - 与 Spring Boot 集成成熟
  - `RLock` 足够覆盖当前需求
  - 可统一用于业务锁和 scheduler 锁

### 7.2 锁的适用流程

- `InspectionTaskServiceImpl#submitTask`
- `SamplingTaskServiceImpl#submitResult`
- `RectificationServiceImpl#submitMy`
- `RectificationServiceImpl#review`
- `WarningEventServiceImpl#assignWarning`
- `WarningEventServiceImpl#processWarningAction`
- `ComplaintCommandService#accept/assign/startProcess/handle/reject`
- 4 个 scheduler

### 7.3 幂等方案

- 锁内保留原有状态校验
- 依赖数据库唯一键继续兜底
- 对无唯一键的流程，锁就是第一幂等层

### 7.4 限流最终方案

- 登录
  - 网关层
  - 维度：IP + 用户名
- 投诉提交
  - 服务层
  - 维度：submitterUserId
- 文件预签名
  - 服务层
  - 维度：userId + bizType
- 看板接口
  - `query-service` 控制器层
  - 维度：userId

## 8. 缓存一致性与异常场景处理

### 8.1 缓存穿透

- 对不存在的企业/监管员/产品/公众详情缓存空值
- 空值 TTL `60s`

### 8.2 缓存击穿

- 对热点 key 使用短锁单飞回填
- 统计和公众详情都要做

### 8.3 缓存雪崩

- TTL 加随机抖动
  - 主数据：`600 ± 60s`
  - 公众详情：`300 ± 30s`

### 8.4 热 Key

- 监管总览、预警统计、公示详情是热 key
- 采用短 TTL + 单飞回填

### 8.5 脏数据

- 统一策略：数据库写成功后删缓存
- 列表缓存统一通过 version key 失效

### 8.6 数据更新后缓存失效

- 企业、监管员、产品、区域、公告、公众企业、公众抽检结果都在对应写服务里删 key
- 不做异步消息删缓存，直接同步删

### 8.7 并发回源

- 使用 `RLock` 或轻量缓存回填锁
- 锁等待时间 `0~1s`

### 8.8 Redis 宕机降级

- 鉴权链路
  - 不降级为放行
  - 维持严格模式
- 主数据缓存
  - 直接回源数据库
- 看板缓存
  - 直接回源下游服务
- scheduler 锁
  - Redis 不可用时直接跳过定时任务，不允许多实例盲跑

## 9. 实施优先级与改造顺序

### 第一阶段

- 登录态 Redis 化
- 网关/服务限流
- scheduler 分布式锁

这一阶段收益最高，且能立刻解决多实例安全与重复执行问题。

### 第二阶段

- `warning-service` 统计缓存
- `query-service` 总览缓存
- 区域树缓存

这一阶段主要解决看板 RT 和数据库压力。

### 第三阶段

- `regulation-service` 内部主数据缓存
- 公众公示缓存
- 高副作用业务锁

这一阶段优化跨服务调用放大和重复提交问题。

## 10. 最终实施清单

- [ ] 在根 `pom` 引入 Redis 与 Redisson 依赖
- [ ] 在 `platform-common` 新增统一 Redis 配置、序列化配置、Lua 限流脚本加载器
- [ ] 在 `user-service` 改造 `TokenUtil`，增加 `jti` 与剩余 TTL 能力
- [ ] 在 `user-service` 新增 `AuthRedisService`
- [ ] 在 `user-service` 改造 `login/logout/introspect`
- [ ] 在 `user-service` 的用户更新、删除、角色绑定后统一失效用户所有会话
- [ ] 在 `gateway-service` 新增 Redis 登录限流过滤器
- [ ] 在 `complaint-service` 对公共投诉提交增加限流
- [ ] 在 `regulation-service` 对 `FileController#presign` 增加限流
- [ ] 在 `regulation-service` 为区域 children/path 增加缓存
- [ ] 在 `regulation-service` 为企业/监管员/产品内部主数据增加缓存
- [ ] 在 `warning-service` 为四个统计接口增加 30 秒缓存与版本号
- [ ] 在 `query-service` 为监管总览增加 20 秒缓存
- [ ] 在 `query-service` 为监管员作用域 profile/region-set 增加缓存
- [ ] 在 `regulation-service` 为公众企业与公告增加详情缓存和列表 version key
- [ ] 在 `regulation-operation-service` 为公众抽检公示增加详情缓存和列表 version key
- [ ] 在 `regulation-operation-service` 为检查提交、抽检提交、整改提交/复核增加 Redisson 锁
- [ ] 在 `warning-service` 为预警处理流程增加 Redisson 锁
- [ ] 在 `complaint-service` 为投诉流转动作增加 Redisson 锁
- [ ] 在 4 个 scheduler 入口增加 Redisson 单实例执行锁
- [ ] 为所有缓存 key 加统一前缀、环境隔离和 TTL 抖动
- [ ] 补充 Redis 不可用下的降级与日志告警

