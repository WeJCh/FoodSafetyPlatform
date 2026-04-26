# MySQL 初始化目录说明

本目录现在采用“部署包自包含 SQL 文件”的方式初始化数据库。

## 设计目标

- 不依赖源码目录
- 不依赖 `src/main/resources/sql` 挂载
- 只要把 `deploy/mysql/init/` 传到虚拟机，就能执行初始化

## 当前文件

1. `01-user-schema.sql`
2. `02-user-init.sql`
3. `03-regulation-schema.sql`
4. `04-regulation-init.sql`
5. `05-operation-schema.sql`
6. `06-operation-init.sql`
7. `07-complaint-schema.sql`
8. `08-complaint-init.sql`
9. `09-query-schema.sql`
10. `10-warning-schema.sql`
11. `11-warning-init.sql`

MySQL 官方镜像会在数据目录为空时，按文件名顺序自动执行这些脚本。

## 注意事项

- 该方案适合首次部署、演示环境、开发环境。
- 如果 `mysql_data` 已存在，MySQL 官方镜像不会再次自动执行本目录中的脚本。
- 若要重新初始化，必须先删除 MySQL 数据卷，再重新启动容器。
- 当前没有单独执行 `addr_region_init.sql`，因为 `regulation-service` 的 `init-data.sql` 已包含区域初始化数据。
