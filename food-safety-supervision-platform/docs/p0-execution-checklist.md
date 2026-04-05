# P0 整改执行清单

版本：V0.1  
日期：2026-03-19  
适用阶段：纠偏方案落地的第一阶段  

---

## 1. P0 的目标

P0 不是补新功能，而是先把项目从“继续跑偏”拉回“可控、可信、可演示”的状态。

本阶段只做四件事：

1. 清掉前端空入口和假入口，避免演示穿帮
2. 让网关鉴权变成默认拒绝，而不是默认放行
3. 堵上文件上传的越权口子
4. 冻结 `query-service` 的职责，停止继续漂移

---

## 2. P0 完成标准（Definition of Done）

P0 完成时，至少要满足下面 6 条：

- 管理员端、企业端、公众端主导航中，不再出现当前无法落地的入口
- 页面主流程中，用户点击后不再进入“功能占位”页
- 网关在 `user-service` 不可用或 introspect 超时时，不再放行受保护接口
- 文件预签名接口只允许当前真实开放的业务组合
- `query-service` 的 P0 职责被明确写死为“预警统计聚合”，不再扩展空壳统计
- P0 改动完成后，后端相关模块能编译，前端能构建

---

## 3. P0 实施顺序

严格按这个顺序做，不要乱穿插：

1. P0-0：建立基线和回滚点
2. P0-1：清理前端空入口和占位入口
3. P0-2：修复网关 `fail-open`
4. P0-3：修复文件上传越权
5. P0-4：冻结 `query-service` 职责
6. P0-5：做联调与验收

---

## 4. P0-0：建立基线和回滚点

### 目标

在开始修改之前，把当前状态固定住，方便中途回退和对比。

### 你要做什么

- 新建一个专门的整改分支
- 记录当前 P0 涉及页面和接口的现状
- 先做一次后端编译和前端构建，确认问题不是历史脏状态导致的

### 建议动作

- 分支名建议：`rectify/p0-stabilization`
- 记录现状页面：
  - 管理员页
  - 企业页
  - 公众首页
- 记录现状接口：
  - `/api/files/presign`
  - 任意一个受保护的 `/api/regulation/**`
  - `/api/query/warnings/**`

### 建议命令

```powershell
git checkout -b rectify/p0-stabilization
```

后端基线编译：

```powershell
mvn -q -pl gateway-service,regulation-service,query-service -am -DskipTests compile
```

前端基线构建：

```powershell
cd food-web
npm run build
```

### 验收标准

- 分支已建立
- 当前系统构建状态已知
- 你知道现在哪些入口是假的、哪些接口有风险

---

## 5. P0-1：清理前端空入口和占位入口

### 目标

把当前最容易暴露“半成品”状态的入口先收掉，保证用户从主导航进入的页面都是真实可用的。

### 当前证据

- `food-web/src/views/AdminView.vue`
  - `企业审批`
  - `用户管理`
  - `操作日志`
  - `角色调整`
  - `区域交接`
  - `启用/停用`
  这些入口当前不是完整能力，且多数落到占位区
- `food-web/src/views/EnterpriseProfileView.vue`
  - `检查结果` 当前是占位
- `food-web/src/views/PublicHomeView.vue`
  - `企业公示`
  - `预警与公告`
  当前按钮已渲染，但 `App.vue` 没有接线

### 需要修改的文件

- `food-web/src/views/AdminView.vue`
- `food-web/src/views/EnterpriseProfileView.vue`
- `food-web/src/views/PublicHomeView.vue`
- 可选：`food-web/src/App.vue`

### 操作清单

#### 5.1 管理员页

只保留当前真实有价值的入口：

- 保留：
  - `监管人员管理`
  - 如果你确认是实装的，再保留对应子功能
- 删除或隐藏：
  - `企业审批`
  - `用户管理`
  - `操作日志`
  - `角色调整`
  - `区域交接`
  - `启用/停用` 独立标签

注意：

- 如果“启用/停用”已经在监管人员列表页内通过按钮完成，就不要保留独立标签
- 同时要清理对应的 `sectionLabel`、`subSectionLabel` 等计算逻辑，避免死代码残留

#### 5.2 企业页

- 隐藏 `检查结果` 导航入口
- 保留：
  - `企业备案`
  - `整改任务`

注意：

- 当前 P0 不补检查页，所以不要保留这个入口
- 后续 P1 再补“企业检查记录查询页”

#### 5.3 公众首页

- 隐藏：
  - `企业公示`
  - `预警与公告`
  两个按钮或整卡片
- 保留：
  - `我要投诉`
  - `我的投诉`

同时修改文案：

- 不要再出现“抽检结果与召回信息集中展示”这种当前做不到的强承诺
- 可以改成“投诉提交与进度查询为当前已开放功能”

#### 5.4 可选清理

如果你想把 P0 做得更干净：

- 删除 `PublicHomeView.vue` 中暂时无用的 `open-enterprise`、`open-bulletin` emit
- 保持 `App.vue` 无需接线这些事件

### 验收标准

- 从管理员页主导航进入，不会看到占位页
- 从企业页主导航进入，不会看到占位页
- 从公众首页点击按钮，不会点击到未接线功能
- 页面中不再把“未开放能力”包装成已开放能力

### 建议提交

```text
chore(frontend): remove P0 placeholder and dead entry points
```

---

## 6. P0-2：修复网关 `fail-open`

### 目标

把当前网关的默认行为从“鉴权服务挂了也放行”改成“鉴权服务挂了就拒绝”。

### 当前证据

- `gateway-service/src/main/resources/application.yml`
  - `gateway.auth.introspect-fail-open: true`
- `gateway-service/src/main/java/com/mortal/gateway/filter/JwtAuthFilter.java`
  - `@Value("${gateway.auth.introspect-fail-open:true}")`
  - 当 introspect 不可用时，存在放行逻辑

### 需要修改的文件

- `gateway-service/src/main/resources/application.yml`
- `gateway-service/src/main/java/com/mortal/gateway/filter/JwtAuthFilter.java`

### 操作清单

#### 6.1 改配置默认值

把：

```yaml
introspect-fail-open: true
```

改成：

```yaml
introspect-fail-open: ${GATEWAY_AUTH_INTROSPECT_FAIL_OPEN:false}
```

这样做的目的：

- 默认严格
- 本地特殊场景如果一定要放开，仍可用环境变量临时控制

#### 6.2 改代码默认值

把：

```java
@Value("${gateway.auth.introspect-fail-open:true}")
```

改成：

```java
@Value("${gateway.auth.introspect-fail-open:false}")
```

#### 6.3 保留 fail-open 分支，但不再默认开启

不要把逻辑全部删掉，保留分支即可，原因：

- 未来联调阶段如果真需要临时放开，还能用配置控制
- 但 P0 默认行为必须是严格模式

### 验收标准

- 正常登录时，原有受保护接口仍能访问
- 当 `user-service` 不可用或 introspect 超时时，受保护接口返回 `401` 或鉴权失败，不再继续向下游放行
- 白名单接口仍可访问

### 建议验证方式

验证 1：正常情况

- 正常启动 `user-service`
- 带 token 调用一个受保护接口，例如：
  - `/api/regulation/enterprise/me`

验证 2：异常情况

- 暂停 `user-service`
- 再调用相同接口
- 期望结果：返回鉴权失败，而不是继续放行

### 建议提交

```text
fix(gateway): default to fail-closed when auth introspection fails
```

---

## 7. P0-3：修复文件上传越权

### 目标

把当前“谁都能拿着登录态申请不同业务类型上传地址”的问题堵上。

### 当前证据

- `regulation-service/src/main/java/com/mortal/regulation/controller/FileController.java`
  - 当前只校验 token 是否能解析出 `userId`
- `regulation-service/src/main/java/com/mortal/regulation/service/MinioFileService.java`
  - 当前只校验大小、类型、`bizType` 枚举
- `regulation-service/src/main/java/com/mortal/regulation/common/enums/FileBizType.java`
  - 当前支持：
    - `COMPLAINT`
    - `RECTIFICATION`
    - `INSPECTION`
- `food-web/src/views/PublicComplaintView.vue`
  - 当前使用 `COMPLAINT`
- `food-web/src/views/EnterpriseProfileView.vue`
  - 当前使用 `RECTIFICATION`
- 网关当前只允许 `PUBLIC`、`ENTERPRISE` 访问 `/api/files/**`

结论：

- 公众可以尝试申请 `RECTIFICATION`
- 企业可以尝试申请 `COMPLAINT`
- `INSPECTION` 在 P0 并未真实开放，但枚举存在

### 需要修改的文件

- `regulation-service/src/main/java/com/mortal/regulation/controller/FileController.java`
- `regulation-service/src/main/java/com/mortal/regulation/service/MinioFileService.java`
- 可选：`regulation-service/src/main/java/com/mortal/regulation/util/JwtUserResolver.java`

### 推荐实现方式

不要继续只靠解析 `Authorization` 里的 `userId`。

优先使用网关透传头：

- `X-User-Type`
- `X-User-Roles`

因为这些头已经由网关统一注入，适合做授权判断。

### 权限矩阵（P0 版本）

P0 只允许这两种真实业务组合：

| 用户类型 / 角色 | 允许的 `bizType` |
| --- | --- |
| `PUBLIC` | `COMPLAINT` |
| `ENTERPRISE` | `RECTIFICATION` |

P0 明确禁止：

| 用户类型 / 角色 | 禁止的 `bizType` |
| --- | --- |
| `PUBLIC` | `RECTIFICATION`、`INSPECTION` |
| `ENTERPRISE` | `COMPLAINT`、`INSPECTION` |
| 其他 | 全部禁止 |

注意：

- `INSPECTION` 在 P0 先一律拒绝
- 后续如果真的开放监管端上传检查附件，再在 P1/P2 打开，并同步修改网关角色规则

### 操作清单

#### 7.1 在 `FileController` 增加角色读取

新增读取：

- `X-User-Type`
- `X-User-Roles`

#### 7.2 增加文件业务授权校验

建议新增一个明确的方法，例如：

- `validatePresignPermission(userType, userRoles, bizType)`

逻辑要求：

- `PUBLIC` 只能 `COMPLAINT`
- `ENTERPRISE` 只能 `RECTIFICATION`
- 其他一律拒绝

#### 7.3 拒绝时返回明确错误

错误信息不要模糊成 `invalid biz type`，而要区分：

- 枚举非法：`invalid biz type`
- 权限不足：`forbidden biz type`

#### 7.4 保留原有文件校验

下面这些校验仍然保留：

- 文件大小
- 文件 MIME 类型
- 文件名清洗

### 验收标准

- 公众提交投诉图片仍可正常上传
- 企业提交整改图片仍可正常上传
- 公众申请 `RECTIFICATION` 返回拒绝
- 企业申请 `COMPLAINT` 返回拒绝
- 任何用户申请 `INSPECTION` 在 P0 都返回拒绝

### 建议验证用例

至少验证 4 组：

1. `PUBLIC + COMPLAINT`：成功
2. `PUBLIC + RECTIFICATION`：失败
3. `ENTERPRISE + RECTIFICATION`：成功
4. `ENTERPRISE + COMPLAINT`：失败

### 建议提交

```text
fix(file): restrict presign upload by active business role
```

---

## 8. P0-4：冻结 `query-service` 职责

### 目标

停止 `query-service` 在 P0 阶段继续漂移，把它明确成“预警统计聚合服务”，不要一边有 `stat_*` 表，一边实际只做 warning 代理，还继续往里堆需求。

### 当前证据

- `query-service/src/main/resources/sql/schema.sql`
  - 有：
    - `stat_enterprise`
    - `stat_inspection`
    - `stat_complaint`
- `query-service/src/main/java/com/mortal/query/service/impl/WarningStatsQueryServiceImpl.java`
  - 当前实际只代理 `warning-service` 内部统计
- `query-service/docs/warning-stats-p0-smoke-test.md`
  - 当前文档也集中在 warning stats

### 这一步不要做什么

P0 不要做下面这些事情：

- 不要新增新的企业统计接口
- 不要新增新的投诉统计接口
- 不要补假的离线汇总逻辑
- 不要为了“看起来完整”去硬做大屏

### 这一步要做什么

#### 8.1 写清楚边界

建议新增一份简短说明文档，或者补在现有文档里，明确：

- 当前 `query-service` 的 P0 职责只有：
  - 预警统计聚合
  - 作用域收口
  - 轻量查询聚合

#### 8.2 标明 `stat_*` 表状态

建议说明：

- 当前三张 `stat_*` 表为预留结构
- P0 不作为实际展示和答辩的数据来源

#### 8.3 冻结开发边界

把下面这条原则写进你自己的实施说明里：

> P0 阶段，`query-service` 不扩业务面，只收敛已有 warning stats 能力。

### 建议修改文件

- 可新建：
  - `query-service/docs/service-scope-p0.md`
- 或者追加到：
  - `docs/food-safety-platform-rectification-plan.md`

### 验收标准

- 你能一句话解释 `query-service` 当前做什么
- 你能一句话解释 `stat_*` 表为什么现在不用
- 后续 P0 改动中不再向 `query-service` 继续塞新功能

### 建议提交

```text
docs(query): freeze query service scope for P0
```

---

## 9. P0-5：联调与验收

### 目标

确认前面 4 项整改是真的生效了，而不是只改了代码没改结果。

### 后端编译

```powershell
mvn -q -pl gateway-service,regulation-service,query-service -am -DskipTests compile
```

### 前端构建

```powershell
cd food-web
npm run build
```

### 手工验收清单

#### 9.1 页面验收

- 管理员登录后：
  - 不再看到 `企业审批`
  - 不再看到 `用户管理`
  - 不再看到 `操作日志`
  - 不再看到 `角色调整`
  - 不再看到 `区域交接`
  - 不再看到 `启用/停用` 独立标签
- 企业登录后：
  - 不再看到 `检查结果` 占位入口
- 公众登录后：
  - 首页只保留当前真实开放能力
  - 不再能点到未接线入口

#### 9.2 鉴权限验收

- 正常启动 `user-service` 时，受保护接口可访问
- 关闭 `user-service` 后，受保护接口返回鉴权失败

#### 9.3 文件上传验收

- 公众投诉上传成功
- 企业整改上传成功
- 公众整改上传失败
- 企业投诉上传失败
- `INSPECTION` 上传失败

#### 9.4 `query-service` 验收

- 现有 warning stats 接口仍可用
- P0 新改动没有往 `query-service` 添加新业务面

### 最终交付判断

只有当下面四条都满足时，P0 才算完成：

- 页面主入口没有明显空壳
- 鉴权默认是 fail-closed
- 文件上传权限边界已收紧
- `query-service` 职责已冻结

---

## 10. 推荐提交顺序

建议拆成 4 次提交，不要一把梭：

1. `chore(frontend): remove P0 placeholder and dead entry points`
2. `fix(gateway): default to fail-closed when auth introspection fails`
3. `fix(file): restrict presign upload by active business role`
4. `docs(query): freeze query service scope for P0`

---

## 11. P0 完成后，你应该进入什么阶段

P0 完成后，不要立刻去做抽检、产品档案、追溯。

下一阶段应该直接进入 P1，优先顺序建议是：

1. 企业公示详情
2. 公告发布与展示
3. 企业检查记录页
4. 重点监管联动
5. 投诉反馈增强

---

## 12. 一句话执行原则

> P0 的任务不是“让系统更丰富”，而是“让系统不再继续跑偏，并且主界面、权限边界、服务职责先站稳”。
