# 数据库关系图

## 微服务数据库架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        食品安全监管平台                              │
│                     微服务数据库架构图                               │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────┐
│  user-service    │
│  food_user_db    │
├──────────────────┤
│ • sys_user       │───┐
│ • sys_role       │   │
│ • sys_user_role  │   │
└──────────────────┘   │
                       │
                       ├─────────────────────────────────┐
                       │                                 │
                       ↓                                 ↓
            ┌──────────────────┐            ┌──────────────────┐
            │ regulation-      │            │ complaint-       │
            │ service          │            │ service          │
            │ food_regulation_ │            │ food_complaint_  │
            │ db               │            │ db               │
            ├──────────────────┤            ├──────────────────┤
            │ • addr_region    │───┐        │ • complaint      │───┐
            │ • addr_location  │   │        │ • complaint_     │   │
            │ • food_enterprise│───┼───┐    │   handle         │   │
            │ • food_regulator │   │   │    └──────────────────┘   │
            │ • food_product   │───┼───┼───┐                       │
            │ • enterprise_    │   │   │   │                       │
            │   key_reason     │   │   │   │                       │
            │ • public_        │   │   │   │                       │
            │   bulletin       │   │   │   │                       │
            └──────────────────┘   │   │   │                       │
                                   │   │   │                       │
                                   ↓   ↓   ↓                       │
                        ┌──────────────────┐                       │
                        │ regulation-      │                       │
                        │ operation-       │                       │
                        │ service          │                       │
                        │ food_regulation_ │                       │
                        │ operation_db     │                       │
                        ├──────────────────┤                       │
                        │ • inspection_    │                       │
                        │   task           │                       │
                        │ • sampling_task  │                       │
                        │ • sampling_      │───┐                   │
                        │   result         │   │                   │
                        │ • inspection_    │   │                   │
                        │   record         │   │                   │
                        │ • inspection_    │   │                   │
                        │   item           │   │                   │
                        │ • rectification_ │───┼───┐               │
                        │   task           │   │   │               │
                        │ • rectification_ │   │   │               │
                        │   action_log     │   │   │               │
                        │ • warning_event_ │   │   │               │
                        │   outbox         │   │   │               │
                        └──────────────────┘   │   │               │
                                               │   │               │
                                               ↓   ↓               ↓
                                        ┌──────────────────┐
                                        │ warning-service  │
                                        │ food_warning_db  │
                                        ├──────────────────┤
                                        │ • warning_rule   │
                                        │ • warning_record │
                                        │ • warning_       │
                                        │   process_log    │
                                        └──────────────────┘
```

## 核心数据流转

### 1. 用户注册与认证流程

```
用户注册
    ↓
sys_user (user-service)
    ↓
    ├─→ food_enterprise (regulation-service) [企业用户]
    ├─→ food_regulator (regulation-service) [监管人员]
    └─→ complaint.submitter_user_id (complaint-service) [公众用户]
```

### 2. 企业监管流程

```
企业备案
    ↓
food_enterprise (regulation-service)
    ↓
    ├─→ inspection_task (regulation-operation-service)
    │       ↓
    │   inspection_record
    │       ↓
    │   rectification_task
    │       ↓
    │   warning_record (warning-service)
    │
    ├─→ sampling_task (regulation-operation-service)
    │       ↓
    │   sampling_result
    │       ↓
    │   warning_record (warning-service)
    │
    └─→ complaint (complaint-service)
            ↓
        complaint_handle
            ↓
        warning_record (warning-service)
```

### 3. 预警触发流程

```
业务事件
    ↓
    ├─→ 投诉超标 (complaint-service)
    ├─→ 抽检不合格 (regulation-operation-service)
    ├─→ 整改逾期 (regulation-operation-service)
    └─→ 连续检查不合格 (regulation-operation-service)
            ↓
    warning_event_outbox (regulation-operation-service)
            ↓
    warning_record (warning-service)
            ↓
    warning_process_log (warning-service)
```

## 详细表关系

### user-service (food_user_db)

```
sys_user (用户表)
    ├─→ sys_user_role.user_id
    ├─→ food_enterprise.user_id (regulation-service)
    ├─→ food_regulator.user_id (regulation-service)
    └─→ complaint.submitter_user_id (complaint-service)

sys_role (角色表)
    └─→ sys_user_role.role_id

sys_user_role (用户角色关联表)
    ├─→ sys_user.id
    └─→ sys_role.id
```

### regulation-service (food_regulation_db)

```
addr_region (行政区划表)
    ├─→ addr_region.parent_id (自关联)
    ├─→ addr_location.region_id
    ├─→ food_enterprise.region_id
    ├─→ food_regulator_region.region_id
    ├─→ inspection_task.region_id (regulation-operation-service)
    ├─→ sampling_task.region_id (regulation-operation-service)
    └─→ warning_record.region_id (warning-service)

addr_location (地址信息表)
    ├─→ addr_region.id
    └─→ food_enterprise.address_id

food_enterprise (食品企业表)
    ├─→ sys_user.id (user-service)
    ├─→ addr_region.id
    ├─→ addr_location.id
    ├─→ food_product.enterprise_id
    ├─→ enterprise_key_reason.enterprise_id
    ├─→ inspection_task.enterprise_id (regulation-operation-service)
    ├─→ sampling_task.enterprise_id (regulation-operation-service)
    ├─→ complaint.enterprise_id (complaint-service)
    └─→ warning_record.biz_id (warning-service, when biz_type='ENTERPRISE')

food_regulator (监管人员表)
    ├─→ sys_user.id (user-service)
    ├─→ food_regulator_region.regulator_id
    ├─→ inspection_task.assigned_to (regulation-operation-service)
    ├─→ sampling_task.assigned_to (regulation-operation-service)
    ├─→ complaint.assigned_to (complaint-service)
    └─→ warning_record.owner_regulator_id (warning-service)

food_product (产品档案表)
    ├─→ food_enterprise.id
    ├─→ sampling_task.product_id (regulation-operation-service)
    └─→ sampling_result.product_id (regulation-operation-service)

food_regulator_region (监管人员辖区表)
    ├─→ food_regulator.id
    └─→ addr_region.id

enterprise_key_reason (重点监管原因表)
    └─→ food_enterprise.id

public_bulletin (公众公告表)
    (独立表，无外键关联)
```

### regulation-operation-service (food_regulation_operation_db)

```
inspection_task (检查任务表)
    ├─→ food_enterprise.id (regulation-service)
    ├─→ addr_region.id (regulation-service)
    ├─→ food_regulator.id (regulation-service) [assigned_to]
    └─→ inspection_record.task_id

sampling_task (抽检任务表)
    ├─→ food_enterprise.id (regulation-service)
    ├─→ food_product.id (regulation-service)
    ├─→ addr_region.id (regulation-service)
    ├─→ food_regulator.id (regulation-service) [assigned_to]
    └─→ sampling_result.task_id

sampling_result (抽检结果表)
    ├─→ sampling_task.id
    ├─→ food_enterprise.id (regulation-service)
    ├─→ food_product.id (regulation-service)
    └─→ warning_record.biz_id (warning-service, when biz_type='SAMPLING')

inspection_record (检查记录表)
    ├─→ inspection_task.id
    ├─→ food_enterprise.id (regulation-service)
    ├─→ food_regulator.id (regulation-service) [inspector_id]
    ├─→ inspection_item.inspection_id
    └─→ rectification_task.inspection_id

inspection_item (检查明细表)
    └─→ inspection_record.id

rectification_task (整改任务表)
    ├─→ inspection_record.id
    ├─→ food_enterprise.id (regulation-service)
    ├─→ rectification_action_log.rectification_id
    └─→ warning_record.biz_id (warning-service, when biz_type='RECTIFICATION')

rectification_action_log (整改动作日志表)
    └─→ rectification_task.id

warning_event_outbox (预警事件Outbox表)
    └─→ warning_record (warning-service) [通过事件传递]
```

### complaint-service (food_complaint_db)

```
complaint (投诉表)
    ├─→ sys_user.id (user-service) [submitter_user_id]
    ├─→ food_enterprise.id (regulation-service)
    ├─→ food_regulator.id (regulation-service) [assigned_to, accepted_by, processed_by]
    ├─→ complaint_handle.complaint_id
    └─→ warning_record.biz_id (warning-service, when biz_type='COMPLAINT')

complaint_handle (投诉处理记录表)
    ├─→ complaint.id
    └─→ food_regulator.id (regulation-service) [handler_id]
```

### warning-service (food_warning_db)

```
warning_rule (预警规则表)
    (独立表，无外键关联)

warning_record (预警记录表)
    ├─→ addr_region.id (regulation-service) [region_id]
    ├─→ food_regulator.id (regulation-service) [owner_regulator_id, assigned_to, resolved_by]
    ├─→ biz_id (根据biz_type关联不同的业务表)
    │   ├─→ food_enterprise.id (regulation-service, when biz_type='ENTERPRISE')
    │   ├─→ sampling_result.id (regulation-operation-service, when biz_type='SAMPLING')
    │   ├─→ rectification_task.id (regulation-operation-service, when biz_type='RECTIFICATION')
    │   └─→ complaint.id (complaint-service, when biz_type='COMPLAINT')
    └─→ warning_process_log.warning_id

warning_process_log (预警处理日志表)
    └─→ warning_record.id
```

## 数据一致性保证

### 1. 主键约束
- 所有表都有自增主键ID
- 确保数据唯一性

### 2. 外键约束（逻辑外键）
- 跨库关联使用逻辑外键
- 通过应用层保证数据一致性

### 3. 唯一约束
- `sys_user.username` - 用户名唯一
- `sys_role.role_code` - 角色编码唯一
- `food_enterprise.user_id` - 企业用户ID唯一
- `food_regulator.user_id` - 监管人员用户ID唯一
- `warning_record.dedup_key` - 预警去重键唯一

### 4. 索引优化
- 外键字段建立索引
- 查询频繁的字段建立索引
- 组合查询建立联合索引

## 数据流转示例

### 示例1：企业检查不合格触发整改和预警

```
1. 执法人员执行检查
   inspection_task (status: IN_PROGRESS)
        ↓
2. 记录检查结果（不合格）
   inspection_record (result: FAIL)
        ↓
3. 创建整改任务
   rectification_task (status: ONGOING)
        ↓
4. 触发预警事件
   warning_event_outbox (event_type: INSPECTION_FAIL)
        ↓
5. 创建预警记录
   warning_record (warning_type: INSPECTION_FAIL, status: OPEN)
        ↓
6. 分配预警处理人
   warning_record (assigned_to: regulator_id, status: PROCESSING)
        ↓
7. 企业提交整改报告
   rectification_task (status: SUBMITTED)
   rectification_action_log (action_type: ENTERPRISE_SUBMIT)
        ↓
8. 监管复核通过
   rectification_task (status: CONFIRMED)
   rectification_action_log (action_type: REVIEW_CONFIRM)
        ↓
9. 解决预警
   warning_record (status: RESOLVED)
   warning_process_log (action_type: RESOLVE)
```

### 示例2：公众投诉触发预警

```
1. 公众提交投诉
   complaint (status: SUBMITTED)
        ↓
2. 监管受理投诉
   complaint (status: PENDING)
        ↓
3. 分配处理人
   complaint (status: ASSIGNED, assigned_to: regulator_id)
        ↓
4. 投诉数量达到阈值，触发预警
   warning_record (warning_type: COMPLAINT_OVERFLOW, status: OPEN)
        ↓
5. 处理投诉
   complaint (status: PROCESSING)
   complaint_handle (handle_result: ...)
        ↓
6. 投诉处理完成
   complaint (status: FEEDBACKED)
        ↓
7. 预警处理
   warning_record (status: PROCESSING → RESOLVED)
   warning_process_log (action_type: PROCESS, RESOLVE)
```

## 总结

本数据库架构具有以下特点：

1. **微服务架构**：每个服务独立数据库，松耦合
2. **数据一致性**：通过逻辑外键和应用层保证
3. **业务完整性**：覆盖完整的业务流程
4. **可扩展性**：易于添加新的业务表和关系
5. **性能优化**：合理的索引设计
6. **数据安全**：逻辑删除，数据可追溯
