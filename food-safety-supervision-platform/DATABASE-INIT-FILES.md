# 数据库初始化文件清单

## 📋 文件列表

### 1. SQL初始化脚本（5个微服务）

#### user-service
- ✅ `user-service/src/main/resources/sql/init-data.sql`
  - 16个用户账号（系统管理员、区域管理员、执法人员、企业用户、公众用户）
  - 5个角色（已在schema.sql中定义）
  - 16条用户角色关联

#### regulation-service
- ✅ `regulation-service/src/main/resources/sql/init-data.sql`
  - 14个行政区划（省、市、区、街道）
  - 10个地址信息
  - 6个监管人员档案
  - 10条监管人员辖区分配
  - 6个食品企业（3个正常、2个重点监管、1个待审核）
  - 17个企业产品档案
  - 2条重点监管原因记录
  - 4条公众公告

#### regulation-operation-service
- ✅ `regulation-operation-service/src/main/resources/sql/init-data.sql`
  - 6个检查任务（4个已完成、1个进行中、1个待分配）
  - 4个抽检任务（3个已完成、1个待分配）
  - 3个抽检结果（2个合格、1个不合格）
  - 5个监督检查记录
  - 20条检查明细
  - 2个整改任务（1个已提交待复核、1个返工中）
  - 7条整改动作日志
  - 3个预警事件Outbox记录

#### complaint-service
- ✅ `complaint-service/src/main/resources/sql/init-data.sql`
  - 9条投诉记录（5条已处理、1条处理中、1条已分配、1条待分配、1条已驳回）
  - 5条投诉处理记录

#### warning-service
- ✅ `warning-service/src/main/resources/sql/init-data.sql`
  - 5条预警规则配置
  - 7条预警记录（3条已解决、2条处理中、1条待分配、1条已关闭）
  - 30条预警处理日志

### 2. 自动化脚本

- ✅ `init-all-databases.sh` - Linux/Mac自动化初始化脚本
  - 一键初始化所有数据库
  - 自动验证数据
  - 显示统计信息

- ✅ `init-all-databases.bat` - Windows自动化初始化脚本
  - 功能同上
  - 适配Windows命令行

### 3. 文档

- ✅ `README-DATABASE-INIT.md` - 快速开始指南
  - 快速使用方法
  - 测试账号列表
  - 验证命令

- ✅ `docs/database-init-guide.md` - 详细初始化指南
  - 完整的初始化步骤
  - 数据库结构说明
  - 执行顺序说明
  - 数据关联关系
  - 业务流程完整性
  - 测试场景建议

- ✅ `docs/database-init-summary.md` - 初始化工作总结
  - 已完成工作清单
  - 数据统计
  - 数据关联关系图
  - 业务场景覆盖
  - 使用建议

- ✅ `docs/database-relationship-diagram.md` - 数据库关系图
  - 微服务数据库架构图
  - 核心数据流转图
  - 详细表关系说明
  - 数据流转示例

## 📊 数据统计总览

| 微服务 | 表数量 | 数据记录数 | 主要内容 |
|-------|--------|-----------|---------|
| user-service | 3 | 37条 | 用户、角色、关联 |
| regulation-service | 9 | 63条 | 区划、地址、监管人员、企业、产品、公告 |
| regulation-operation-service | 8 | 50条 | 检查、抽检、整改、预警事件 |
| complaint-service | 2 | 14条 | 投诉、处理记录 |
| warning-service | 3 | 42条 | 预警规则、记录、日志 |
| **总计** | **25** | **206条** | - |

## 🎯 核心功能覆盖

### ✅ 用户管理
- [x] 系统管理员
- [x] 区域管理员
- [x] 执法人员
- [x] 企业用户
- [x] 公众用户
- [x] 角色权限

### ✅ 企业管理
- [x] 企业备案
- [x] 企业审核
- [x] 正常企业
- [x] 重点监管企业
- [x] 产品档案
- [x] 重点监管原因

### ✅ 监管操作
- [x] 检查任务创建
- [x] 检查任务分配
- [x] 检查执行
- [x] 检查记录
- [x] 抽检任务
- [x] 抽检结果
- [x] 整改任务
- [x] 整改复核

### ✅ 投诉处理
- [x] 投诉提交
- [x] 投诉受理
- [x] 投诉分配
- [x] 投诉处理
- [x] 投诉反馈
- [x] 投诉驳回

### ✅ 预警管理
- [x] 预警规则
- [x] 预警触发
- [x] 预警分配
- [x] 预警处理
- [x] 预警升级
- [x] 预警解决
- [x] 预警归档

## 🔗 数据关联关系

### 跨库关联
```
user-service
    ↓
    ├─→ regulation-service (用户→企业/监管人员)
    └─→ complaint-service (用户→投诉人)

regulation-service
    ↓
    ├─→ regulation-operation-service (企业/产品→检查/抽检)
    ├─→ complaint-service (企业→投诉对象)
    └─→ warning-service (企业/区域→预警)

regulation-operation-service
    ↓
    └─→ warning-service (检查/抽检/整改→预警)

complaint-service
    ↓
    └─→ warning-service (投诉→预警)
```

## 🚀 快速开始

### Linux/Mac
```bash
# 设置数据库密码（如果需要）
export DB_PASSWORD=your_password

# 执行初始化
bash init-all-databases.sh
```

### Windows
```cmd
# 设置数据库密码（如果需要）
set DB_PASSWORD=your_password

# 执行初始化
init-all-databases.bat
```

## 🔐 测试账号

| 用户类型 | 用户名 | 密码 | 说明 |
|---------|--------|------|------|
| 系统管理员 | admin | admin123 | 系统管理员 |
| 区域管理员 | regulator_admin_1 | regulator123 | 负责东湖区、西湖区 |
| 执法人员 | enforcer_1 | enforcer123 | 负责东湖区 |
| 企业用户 | enterprise_1 | enterprise123 | 美味餐饮 |
| 公众用户 | public_1 | public123 | 市民张三 |

更多账号详见：`docs/database-init-guide.md`

## ✅ 验证数据

初始化完成后，执行以下SQL验证：

```sql
-- 验证用户数量
SELECT user_type, COUNT(*) FROM food_user_db.sys_user GROUP BY user_type;

-- 验证企业数量
SELECT status, approval_status, COUNT(*) 
FROM food_regulation_db.food_enterprise 
GROUP BY status, approval_status;

-- 验证检查任务
SELECT status, COUNT(*) 
FROM food_regulation_operation_db.inspection_task 
GROUP BY status;

-- 验证投诉
SELECT status, COUNT(*) 
FROM food_complaint_db.complaint 
GROUP BY status;

-- 验证预警
SELECT level, status, COUNT(*) 
FROM food_warning_db.warning_record 
GROUP BY level, status;
```

## 📚 相关文档

1. **快速开始**: `README-DATABASE-INIT.md`
2. **详细指南**: `docs/database-init-guide.md`
3. **工作总结**: `docs/database-init-summary.md`
4. **关系图**: `docs/database-relationship-diagram.md`

## 💡 使用场景

### 开发环境
- 快速搭建开发环境
- 提供测试数据
- 便于功能开发和调试

### 测试环境
- 功能测试
- 集成测试
- 性能测试

### 演示环境
- 系统演示
- 功能展示
- 业务流程演示

## ⚠️ 注意事项

1. **执行顺序**：必须按照user-service → regulation-service → regulation-operation-service → complaint-service → warning-service的顺序执行
2. **密码安全**：示例密码仅用于测试，生产环境请使用强密码
3. **数据清理**：重新初始化前需要先清空所有数据库
4. **ID自增**：所有表的自增ID都从100开始，避免冲突

## 🎉 总结

本次数据库初始化工作提供了：
- ✅ 5个微服务的完整初始化SQL脚本
- ✅ 206条测试数据，覆盖所有主要业务场景
- ✅ 跨库数据关联完整且一致
- ✅ 自动化初始化脚本（Linux/Mac和Windows）
- ✅ 详细的使用文档和关系图

可以直接用于开发、测试和演示，大大提高项目的可用性！
