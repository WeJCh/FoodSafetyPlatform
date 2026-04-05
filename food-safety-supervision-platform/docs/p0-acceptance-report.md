# P0 整体验收报告

版本：V0.1  
日期：2026-03-19  
对应阶段：P0-5 联调与验收  

---

## 1. 验收目标

本次验收目标不是继续开发新功能，而是确认 P0 四项整改已经真实生效：

1. 前端空入口和占位入口已清理
2. 网关鉴权默认改为 fail-closed
3. 文件上传权限边界已收紧
4. `query-service` 职责已冻结，不再继续漂移

---

## 2. 本次实际验证过程

本次并未跳过项目分析，而是先确认当前项目具备哪些可直接验证条件，再决定采用什么验收方式。

### 2.1 现状复核结果

- `gateway-service`、`regulation-service` 原本没有现成测试覆盖本次 P0 修改点
- `query-service` 原本已有预警统计集成测试
- 前端可以直接通过源码静态检查 + `npm run build` 验证
- 当前环境下未启动完整的 Nacos / MySQL / MinIO / user-service / gateway / regulation 全套服务集群，因此：
  - 页面入口可做静态验收
  - 网关 fail-closed、文件上传权限矩阵更适合补成自动化测试
  - `query-service` 可继续使用现有集成测试验证

因此本次验收采用以下策略：

- 页面类问题：静态检查 + 前端构建
- 鉴权/上传权限：补自动化测试
- `query-service`：复用并修正现有集成测试

---

## 3. 验收项与结果

### 3.1 页面验收

#### 验收方法

对以下页面执行静态模式检查：

- `food-web/src/views/AdminView.vue`
- `food-web/src/views/EnterpriseProfileView.vue`
- `food-web/src/views/PublicHomeView.vue`

检查关键字：

- `企业审批`
- `用户管理`
- `操作日志`
- `角色调整`
- `区域交接`
- `功能占位`
- `open-enterprise`
- `open-bulletin`

#### 验收结果

- 已通过
- 结果：未发现 P0 阶段禁止保留的占位入口或未接线入口

结论：

- 管理员页已收缩为监管人员管理
- 企业页已移除“检查结果”占位入口
- 公众首页已移除未开放的公示/公告跳转

### 3.2 网关鉴权验收

#### 验收方法

新增自动化测试：

- `gateway-service/src/test/java/com/mortal/gateway/filter/JwtAuthFilterTest.java`

覆盖两类行为：

1. `failOpen=false` 且 `introspect` 异常时，受保护请求应返回 `401`
2. `failOpen=true` 时，仍允许保留临时放开能力

#### 验收结果

- 已通过

结论：

- P0 要求的 fail-closed 默认行为已经成立
- 同时保留了受配置控制的临时 fail-open 分支，便于后续特殊联调场景使用

### 3.3 文件上传权限验收

#### 验收方法

新增自动化测试：

- `regulation-service/src/test/java/com/mortal/regulation/controller/FileControllerTest.java`

覆盖权限矩阵：

1. `PUBLIC + COMPLAINT` 成功
2. `PUBLIC + RECTIFICATION` 拒绝
3. `ENTERPRISE + RECTIFICATION` 成功
4. `ENTERPRISE + INSPECTION` 拒绝

说明：

- P0 阶段 `INSPECTION` 统一拒绝
- 当前前端真实调用的两种上传场景与权限矩阵一致

#### 验收结果

- 已通过

结论：

- 文件预签名接口已经不再是“只要登录就能申请任意业务类型”
- 当前权限边界符合 P0 收敛要求

### 3.4 `query-service` 验收

#### 验收方法

1. 复核当前服务职责
2. 检查 P0 边界说明文档
3. 运行现有集成测试：
   - `query-service/src/test/java/com/mortal/query/controller/WarningStatsControllerIntegrationTest.java`

#### 验收过程中发现的问题

发现一处历史测试漂移问题：

- 测试代码仍引用旧的 `com.mortal.query.common.ApiResponse`
- 当前客户端接口已使用 `com.mortal.platform.common.ApiResponse`

这不是 P0 本次改动引入的问题，而是原测试代码与现有客户端签名不一致。

#### 已执行修复

- 已修正测试导入，恢复与当前主代码一致

#### 验收结果

- 已通过

结论：

- `query-service` 现有 warning stats 集成测试可运行
- P0 阶段职责冻结说明已落地
- 当前未继续向 `query-service` 扩展新业务面

### 3.5 前端构建验收

#### 验收命令

```powershell
cd food-web
npm run build
```

#### 验收结果

- 已通过

结论：

- 前端收缩改动后，构建产物正常生成

### 3.6 后端测试验收

#### 验收命令

```powershell
mvn -q -pl gateway-service,regulation-service,query-service -am test
```

#### 验收结果

- 已通过

说明：

- 验收过程中先暴露出 `query-service` 历史测试漂移问题
- 该问题已修复后重新执行通过

---

## 4. 本次新增/修复的验收资产

本次 P0-5 除了执行验收，还补齐了后续可重复使用的测试资产：

- 新增：
  - `gateway-service/src/test/java/com/mortal/gateway/filter/JwtAuthFilterTest.java`
  - `regulation-service/src/test/java/com/mortal/regulation/controller/FileControllerTest.java`
- 修复：
  - `query-service/src/test/java/com/mortal/query/controller/WarningStatsControllerIntegrationTest.java`

这些测试后续仍可用于回归验证，不属于一次性人工验收。

---

## 5. 当前无法在本轮完全手工联调的部分

由于本轮未拉起完整运行环境，以下事项未采用“真实服务进程 + 手工接口请求”方式验证：

- 暂停 `user-service` 后通过真实网关发请求的人工联调
- 真实 MinIO 预签名上传全链路验证
- 真实前端登录后点击操作的人工录屏式验收

但这些关键点已由自动化测试和静态验证补位：

- 网关 fail-closed：由 `JwtAuthFilterTest` 验证
- 文件上传权限矩阵：由 `FileControllerTest` 验证
- 页面主入口清理：由静态检查 + 前端构建验证

因此当前结论仍可作为 P0 阶段的有效交付结果。

---

## 6. P0 最终判断

根据本次验收结果：

- 页面主入口没有明显空壳
- 网关鉴权默认已切换为 fail-closed
- 文件上传权限边界已收紧
- `query-service` 职责已冻结
- 后端测试通过
- 前端构建通过

结论：

> P0 阶段已经完成，可以进入 P1。

---

## 7. 下一步建议

P0 完成后，建议不要继续做基础设施或泛后台能力，直接进入 P1：

1. 企业公示详情
2. 公告发布与展示
3. 企业检查记录页
4. 重点监管联动
5. 投诉反馈增强

一句话结论：

> P0 已经把“界面空壳、权限边界、服务职责漂移”这三类最危险的问题压住了，项目可以开始进入真正补业务闭环的阶段。
