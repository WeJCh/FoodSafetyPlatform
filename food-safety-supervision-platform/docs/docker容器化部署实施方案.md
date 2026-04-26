# 食品安全监管平台 Docker 容器化部署实施方案

## 1. 文档目的

本文档用于指导你将当前项目部署到 CentOS 7 虚拟机中，并为后续正式落地 Docker 容器化提供完整执行路线。

本文档有两个目标：

1. 让你先把部署环境、目录结构、镜像构建思路、编排思路和执行顺序全部理清。
2. 明确哪些步骤现在可以自己做，哪些步骤需要后续由我继续帮你补齐代码或部署文件。

注意：

- 本文档基于当前仓库实际情况编写。
- 当前仓库中还没有 `Dockerfile`、`docker-compose.yml`、Nginx 部署配置、容器化初始化脚本。
- 本文档先给出可执行的部署方案与操作步骤。
- 涉及需要改代码或新增部署文件的地方，我会统一标记为 `【后续需要改代码/补文件】`。

---

## 2. 项目实际架构梳理

根据当前仓库代码，项目由以下部分组成：

### 2.1 前端

- `food-web`
- 技术栈：`Vue 3 + Vite`
- 当前开发默认端口：`5173`
- 前端默认请求网关地址：`http://localhost:8080`

### 2.2 后端服务

- `gateway-service`：网关，端口 `8080`
- `user-service`：用户服务，端口 `8081`
- `regulation-service`：监管基础服务，端口 `8082`
- `query-service`：统计查询服务，端口 `8083`
- `warning-service`：预警服务，端口 `8084`
- `complaint-service`：投诉服务，端口 `8085`
- `regulation-operation-service`：监管业务操作服务，端口 `8086`

### 2.3 公共模块

- `platform-common`
- 不独立运行，作为后端公共依赖模块参与打包

### 2.4 外部依赖

当前代码明确依赖以下中间件：

- `MySQL`
- `Redis`
- `Nacos`
- `MinIO`

说明：

- `Nacos` 用于服务注册发现，当前不能简单去掉。
- `Redis` 不只是缓存，还用于限流和分布式锁。
- `MinIO` 是文件上传真实依赖，不是预留配置。

---

## 3. 建议的容器化部署目标

建议你本次采用如下目标：

### 3.1 部署目标

在一台 CentOS 7 虚拟机中，使用 Docker 运行以下内容：

- 1 个前端容器：`nginx + 前端静态资源`
- 1 个网关容器：`gateway-service`
- 6 个后端业务容器
- 1 个 MySQL 容器
- 1 个 Redis 容器
- 1 个 Nacos 容器
- 1 个 MinIO 容器

### 3.2 对外访问方式

建议最终只对外暴露这些入口：

- `80`：前端页面入口
- `8848`：Nacos 控制台
- `9000`：MinIO API
- `9001`：MinIO Console

说明：

- 网关 `8080` 建议只在 Docker 内部网络中使用，不直接暴露给外部。
- 前端通过 Nginx 反向代理 `/api` 到网关，这样后续部署更整洁。

### 3.3 为什么这样设计

这样设计的原因有三点：

1. 保持与当前代码结构一致，避免一上来重构服务发现。
2. 便于后续做单机演示、答辩和论文附录整理。
3. 能够在不先修改业务代码的前提下推进容器化。

---

## 4. 当前仓库与容器化之间的差距

当前仓库和目标容器化部署之间，主要还有以下差距：

### 4.1 缺少部署文件

当前仓库没有：

- `Dockerfile`
- `docker-compose.yml`
- `.dockerignore`
- Nginx 生产配置
- 容器化环境变量模板
- MySQL 初始化挂载目录整理

### 4.2 前端生产部署方式尚未定型

当前前端是 Vite 开发模式，生产部署需要：

- 先 `npm run build`
- 再放到 Nginx 静态目录中运行

### 4.3 跨域和网关访问策略需要收口

当前网关 CORS 主要放行的是本地开发地址：

- `http://localhost:5173`
- `http://localhost:5174`
- `http://127.0.0.1:5173`
- `http://127.0.0.1:5174`

如果生产环境直接通过浏览器访问虚拟机 IP，需要重新处理访问方式。

最稳妥的方式是：

- 浏览器只访问 `http://虚拟机IP/`
- 前端静态资源由 Nginx 提供
- `/api` 由 Nginx 反向代理到 `gateway-service:8080`

这样可以不依赖生产环境跨域。

### 4.4 SQL 初始化需要规范化

当前仓库中已经有各服务的：

- `schema.sql`
- `init-data.sql`

但它们还没有整理成容器初始化目录结构。

而且部分脚本带有：

- `DROP TABLE`
- `CREATE DATABASE`
- 演示数据插入

因此适合“首次初始化演示环境”，不适合作为正式生产迁移脚本反复执行。

---

## 5. 实施总路线

建议按照下面 5 个阶段推进。

### 阶段 1：准备 CentOS 7 宿主机

目标：

- 安装 Docker
- 安装 Docker Compose
- 配置系统目录
- 开放防火墙端口

### 阶段 2：整理部署目录

目标：

- 规划镜像构建目录
- 规划日志目录
- 规划数据卷目录
- 规划 MySQL 初始化目录

### 阶段 3：补齐容器化所需文件

目标：

- 编写后端 Dockerfile
- 编写前端 Dockerfile
- 编写 Nginx 配置
- 编写 `docker-compose.yml`
- 编写 `.env`

这一阶段需要我继续帮你做。

### 阶段 4：构建与启动

目标：

- 构建镜像
- 启动中间件
- 初始化数据库
- 启动业务服务
- 启动前端

### 阶段 5：验证与问题收口

目标：

- 检查服务注册
- 检查数据库连接
- 检查 Redis 连接
- 检查 MinIO 文件上传
- 检查前后端访问链路

---

## 6. CentOS 7 宿主机准备

## 6.1 重要说明

CentOS 7 已结束生命周期，后续系统包维护风险较高。

这不代表不能部署，但代表：

- Docker 版本要尽量固定
- 不建议在 CentOS 7 上无限追新
- 部署时尽量保守，优先求稳

如果这台虚拟机只是用于毕业设计、演示和论文支撑，仍然可以继续使用。

---

## 6.2 检查系统信息

登录 CentOS 7 后，先执行：

```bash
cat /etc/centos-release
uname -r
uname -m
ip addr
```
输出：
[root@localhost ~]# cat /etc/centos-release
CentOS Linux release 7.9.2009 (Core)
[root@localhost ~]# uname -r
3.10.0-1160.el7.x86_64
[root@localhost ~]# uname -m
x86_64
[root@localhost ~]# ip addr
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1000
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
    inet 127.0.0.1/8 scope host lo
       valid_lft forever preferred_lft forever
    inet6 ::1/128 scope host 
       valid_lft forever preferred_lft forever
2: ens33: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP group default qlen 1000
    link/ether 00:0c:29:70:38:a5 brd ff:ff:ff:ff:ff:ff
    inet 192.168.142.133/24 brd 192.168.142.255 scope global dynamic ens33
       valid_lft 5443334sec preferred_lft 5443334sec
    inet6 fe80::20c:29ff:fe70:38a5/64 scope link 
       valid_lft forever preferred_lft forever
3: virbr0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN group default qlen 1000
    link/ether 52:54:00:62:78:76 brd ff:ff:ff:ff:ff:ff
    inet 192.168.122.1/24 brd 192.168.122.255 scope global virbr0
       valid_lft forever preferred_lft forever
4: virbr0-nic: <BROADCAST,MULTICAST> mtu 1500 qdisc pfifo_fast master virbr0 state DOWN group default qlen 1000
    link/ether 52:54:00:62:78:76 brd ff:ff:ff:ff:ff:ff

你需要确认：

- 系统是 CentOS 7
- 架构通常为 `x86_64`
- 记录虚拟机实际 IP

---

## 6.3 更新 YUM 缓存

```bash
sudo yum clean all
sudo yum makecache
```

---

## 6.4 安装基础工具

```bash
sudo yum install -y yum-utils device-mapper-persistent-data lvm2 wget curl vim git unzip
```

---

## 6.5 添加 Docker 官方仓库

```bash
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
```

查看可安装版本：

```bash
yum list docker-ce --showduplicates | sort -r
```

如果仓库访问正常，你会看到多个 `docker-ce` 版本。

---

## 6.6 安装 Docker Engine

建议优先安装一个稳定版本，不要盲目装最新。

先尝试：

```bash
sudo yum install -y docker-ce docker-ce-cli containerd.io
```

如果你后面发现版本兼容问题，再固定版本安装，例如：

```bash
sudo yum install -y docker-ce-<版本号> docker-ce-cli-<版本号> containerd.io
```

说明：

- `<版本号>` 需要根据 `yum list docker-ce --showduplicates` 的结果替换。
- 如果后续你要我帮你定版本，我会根据你虚拟机实际情况给你精确命令。

---

## 6.7 启动 Docker

```bash
sudo systemctl daemon-reload
sudo systemctl enable docker
sudo systemctl start docker
sudo systemctl status docker
```

验证：

```bash
docker --version
sudo docker info
```

---

## 6.8 安装 Docker Compose

优先尝试安装 Compose 插件：

```bash
sudo yum install -y docker-compose-plugin
```

验证：

```bash
docker compose version
```

如果上面的方式不可用，再用独立二进制方式安装：

```bash
sudo curl -L "https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
docker-compose --version
```

说明：

- 如果 `docker compose` 可用，后续优先使用它。
- 如果插件方式不成功，使用 `docker-compose` 也可以。

---

## 6.9 配置非 root 使用 Docker

```bash
sudo usermod -aG docker $USER
```

然后重新登录终端。

验证：

```bash
docker ps
```

如果仍提示权限问题，重新登录一次再试。

---

## 6.10 配置防火墙

查看防火墙状态：

```bash
sudo systemctl status firewalld
```

如果 firewalld 正在运行，开放端口：

```bash
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=8848/tcp
sudo firewall-cmd --permanent --add-port=9000/tcp
sudo firewall-cmd --permanent --add-port=9001/tcp
sudo firewall-cmd --reload
sudo firewall-cmd --list-ports
```

说明：

- 当前建议不开放 `8080` 到 `8086` 给外部。
- 如果后续联调临时需要，也可以单独开放。

---

## 7. 宿主机目录规划

建议统一使用 `/opt/fsp` 作为部署根目录。

创建目录：

```bash
sudo mkdir -p /opt/fsp
sudo mkdir -p /opt/fsp/app
sudo mkdir -p /opt/fsp/compose
sudo mkdir -p /opt/fsp/env
sudo mkdir -p /opt/fsp/logs
sudo mkdir -p /opt/fsp/data/mysql
sudo mkdir -p /opt/fsp/data/redis
sudo mkdir -p /opt/fsp/data/minio
sudo mkdir -p /opt/fsp/data/nacos
sudo mkdir -p /opt/fsp/mysql-init
sudo mkdir -p /opt/fsp/nginx/conf.d
sudo mkdir -p /opt/fsp/nginx/html
```

设置权限：

```bash
sudo chown -R $USER:$USER /opt/fsp
```

建议用途如下：

- `/opt/fsp/app`：项目代码或构建产物
- `/opt/fsp/compose`：`docker-compose.yml`
- `/opt/fsp/env`：环境变量文件
- `/opt/fsp/logs`：服务日志挂载
- `/opt/fsp/data/mysql`：MySQL 数据目录
- `/opt/fsp/data/redis`：Redis 数据目录
- `/opt/fsp/data/minio`：MinIO 数据目录
- `/opt/fsp/data/nacos`：Nacos 数据目录
- `/opt/fsp/mysql-init`：数据库初始化 SQL
- `/opt/fsp/nginx/conf.d`：Nginx 配置

---

## 8. 项目代码上传到虚拟机

你可以任选一种方式。

### 8.1 方式一：使用 Git

如果虚拟机可直接访问 Git 仓库：

```bash
cd /opt/fsp/app
git clone <你的仓库地址> food-safety-supervision-platform
cd /opt/fsp/app/food-safety-supervision-platform
```

### 8.2 方式二：本地压缩后上传

在本地 Windows 上将项目打包后上传到虚拟机，再解压。

虚拟机中解压：

```bash
cd /opt/fsp/app
unzip food-safety-supervision-platform.zip -d .
```

或：

```bash
tar -zxvf food-safety-supervision-platform.tar.gz -C /opt/fsp/app
```

---

## 9. 容器化部署结构设计

建议的逻辑结构如下：

```text
browser
  |
  v
nginx:80
  |
  +--> /            -> 前端静态资源
  |
  +--> /api         -> gateway-service:8080

gateway-service
  |
  +--> user-service
  +--> regulation-service
  +--> query-service
  +--> warning-service
  +--> complaint-service
  +--> regulation-operation-service

所有后端服务共同依赖：
  +--> mysql
  +--> redis
  +--> nacos

regulation-service 额外依赖：
  +--> minio
```

---

## 10. 数据库初始化设计

当前项目存在以下数据库：

- `food_user_db`
- `food_regulation_db`
- `food_regulation_operation_db`
- `food_complaint_db`
- `food_query_db`
- `food_warning_db`

建议使用单个 MySQL 实例，内部创建多个 schema。

## 10.1 推荐初始化顺序

建议按这个顺序执行 SQL：

1. `user-service/schema.sql`
2. `user-service/init-data.sql`
3. `regulation-service/schema.sql`
4. `regulation-service/init-data.sql`
5. `regulation-operation-service/schema.sql`
6. `regulation-operation-service/init-data.sql`
7. `complaint-service/schema.sql`
8. `complaint-service/init-data.sql`
9. `query-service/schema.sql`
10. `warning-service/schema.sql`
11. `warning-service/init-data.sql`

## 10.2 为什么不建议直接把所有 SQL 原样扔进去

原因如下：

1. 有些 `schema.sql` 含 `DROP TABLE`
2. 有些脚本带演示数据
3. `complaint-service` 当前还配置了 `spring.sql.init.mode=always`

这意味着：

- 演示环境可以这样初始化
- 生产思路下不应长期依赖应用启动自动建表

## 10.3 当前建议

当前阶段建议：

- 由 MySQL 容器第一次启动时统一执行 SQL
- 应用层不再承担自动初始化数据库的职责

`【后续需要改代码/补文件】`

后续我需要帮你做的事情：

1. 整理一套容器专用 SQL 初始化目录
2. 决定是否移除 `complaint-service` 的自动初始化配置
3. 避免服务启动时重复执行初始化逻辑

---

## 11. 环境变量规划

建议统一规划为一个 `.env` 或多个 `env/*.env` 文件。

建议变量如下：

```env
MYSQL_ROOT_PASSWORD=123456
DB_HOST=mysql
DB_PORT=3306
DB_USER=root
DB_PASSWORD=123456

REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=123456
REDIS_DATABASE=0

NACOS_ADDR=nacos:8848

MINIO_ENDPOINT=http://minio:9000
MINIO_PUBLIC_ENDPOINT=http://你的虚拟机IP:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=complaints

REGULATION_INTERNAL_TOKEN=regulation-internal-token
WARNING_INTERNAL_TOKEN=warning-internal-token

PLATFORM_REDIS_KEY_PREFIX=fsp
PLATFORM_REDIS_ENV=prod
```

说明：

- `MINIO_PUBLIC_ENDPOINT` 不能写成 `http://minio:9000`
- 这个值必须是浏览器能访问到的地址
- 例如：`http://192.168.1.120:9000`

---

## 12. 后续需要补齐的文件清单

下面这些文件当前仓库中没有，后续需要补。

`【后续需要改代码/补文件】`

### 12.1 后端镜像相关

- 根目录或各服务目录下的 `Dockerfile`
- `.dockerignore`

### 12.2 前端镜像相关

- `food-web/Dockerfile`
- 前端生产环境 `.env.production`

### 12.3 编排相关

- `docker-compose.yml`
- `env/.env`

### 12.4 Nginx 相关

- `nginx.conf`
- `default.conf`

### 12.5 SQL 初始化相关

- 容器专用初始化脚本目录

---

## 13. 推荐的 Dockerfile 设计方案

这一节先写设计，不先落代码。

## 13.1 后端 Dockerfile 设计

建议用多阶段构建：

### 构建阶段

- 基础镜像：`maven:3.9.x-eclipse-temurin-17`
- 执行 `mvn clean package -DskipTests`

### 运行阶段

- 基础镜像：`eclipse-temurin:17-jre`
- 拷贝目标 jar
- 暴露服务端口
- 使用 `java -jar`

推荐做法：

- 尽量做一个“通用后端 Dockerfile”
- 通过构建参数指定服务名

这样可避免 7 份重复 Dockerfile。

`【后续需要改代码/补文件】`

后续我会帮你把这个 Dockerfile 实际写出来。

## 13.2 前端 Dockerfile 设计

建议也是多阶段构建：

### 构建阶段

- 基础镜像：`node:20`
- 执行 `npm ci`
- 执行 `npm run build`

### 运行阶段

- 基础镜像：`nginx:stable`
- 拷贝 `dist/` 到 Nginx 静态目录
- 拷贝 Nginx 配置

`【后续需要改代码/补文件】`

后续我会帮你把这个 Dockerfile 和 Nginx 配置一起写出来。

---

## 14. 推荐的 docker-compose 编排设计

推荐容器如下：

- `mysql`
- `redis`
- `nacos`
- `minio`
- `user-service`
- `regulation-service`
- `regulation-operation-service`
- `complaint-service`
- `warning-service`
- `query-service`
- `gateway-service`
- `food-web`

推荐关键点：

1. 所有服务都加入同一个自定义网络
2. 所有 Java 服务使用环境变量注入配置
3. 所有中间件都绑定数据卷
4. 为关键容器加 `healthcheck`
5. Java 服务通过 `depends_on` 依赖中间件

说明：

- `depends_on` 只能解决编排顺序，不能完全解决服务已就绪问题
- 最稳妥仍是给中间件配置健康检查

`【后续需要改代码/补文件】`

后续我会直接帮你生成可执行的 `docker-compose.yml`。

---

## 15. 前端访问策略

这是部署中非常关键的一点。

## 15.1 当前情况

前端当前默认请求：

```text
http://localhost:8080
```

这适合本地开发，不适合部署到虚拟机。

## 15.2 生产推荐方式

推荐最终采用：

- 前端页面访问：`http://虚拟机IP/`
- API 请求访问：`/api/...`

由 Nginx 做反向代理：

- `/` -> 前端静态资源
- `/api` -> `gateway-service:8080`

## 15.3 为什么推荐这样做

优点：

1. 浏览器和 API 同源
2. 不再依赖生产环境跨域配置
3. 前端只要知道站点根地址，不需要记多个服务地址

`【后续需要改代码/补文件】`

后续大概率需要我帮你确认或微调以下内容：

1. 前端生产环境变量
2. Nginx 反向代理配置
3. 是否保留网关现有 CORS 配置

---

## 16. Nacos、Redis、MinIO 的部署策略

## 16.1 Nacos

建议先使用单机模式。

原因：

- 当前目标是单 VM 演示和部署
- 不需要先做高可用
- 这样最容易落地

## 16.2 Redis

建议单实例：

- 负责缓存
- 负责分布式锁
- 负责限流

## 16.3 MinIO

建议单节点部署：

- 1 个数据目录
- 开放 `9000` 和 `9001`

特别注意：

- `MINIO_PUBLIC_ENDPOINT` 必须配置正确
- 否则前端上传后拿到的访问地址可能无法在浏览器打开

---

## 17. 推荐的启动顺序

容器启动顺序建议如下：

1. `mysql`
2. `redis`
3. `minio`
4. `nacos`
5. `user-service`
6. `regulation-service`
7. `warning-service`
8. `complaint-service`
9. `regulation-operation-service`
10. `query-service`
11. `gateway-service`
12. `food-web`

说明：

- `query-service` 依赖多个其他服务，建议靠后启动
- `gateway-service` 放在后面更稳妥

---

## 18. 每一步执行时的常用 Linux 命令

下面这些命令你后面会频繁用到。

## 18.1 查看目录与文件

```bash
pwd
ls
ls -al
cd /opt/fsp
cd /opt/fsp/app/food-safety-supervision-platform
```

## 18.2 查看端口监听

```bash
ss -lntp
netstat -lntp
```

如果没有 `netstat`：

```bash
sudo yum install -y net-tools
```

## 18.3 查看进程

```bash
ps -ef | grep java
ps -ef | grep docker
```

## 18.4 查看磁盘与内存

```bash
df -h
free -m
top
```

## 18.5 查看容器

```bash
docker ps
docker ps -a
docker images
docker network ls
docker volume ls
```

## 18.6 查看容器日志

```bash
docker logs mysql
docker logs nacos
docker logs gateway-service
docker logs regulation-service
docker logs food-web
```

持续跟踪日志：

```bash
docker logs -f gateway-service
docker logs -f regulation-service
```

## 18.7 进入容器

```bash
docker exec -it mysql bash
docker exec -it redis sh
docker exec -it nacos bash
docker exec -it gateway-service sh
```

## 18.8 使用 Compose 启停

如果你用的是新插件：

```bash
docker compose up -d
docker compose down
docker compose ps
docker compose logs
docker compose logs -f gateway-service
```

如果你用的是独立命令：

```bash
docker-compose up -d
docker-compose down
docker-compose ps
docker-compose logs
docker-compose logs -f gateway-service
```

## 18.9 重建镜像并启动

```bash
docker compose build
docker compose up -d
```

或：

```bash
docker-compose build
docker-compose up -d
```

## 18.10 停止并删除容器

```bash
docker compose down
```

如果还要删除挂载卷：

```bash
docker compose down -v
```

注意：

- `down -v` 会删除卷数据
- MySQL、Redis、MinIO 数据可能会丢失
- 执行前一定要确认

---

## 19. 部署完成后的验证项

建议按下面顺序验证。

## 19.1 验证 Docker 是否正常

```bash
docker ps
docker images
docker info
```

## 19.2 验证中间件是否正常

浏览器访问：

- `http://虚拟机IP:8848/nacos`
- `http://虚拟机IP:9001`

命令验证：

```bash
docker logs nacos
docker logs mysql
docker logs redis
docker logs minio
```

## 19.3 验证服务注册是否成功

登录 Nacos 控制台，检查以下服务是否注册：

- `gateway-service`
- `user-service`
- `regulation-service`
- `query-service`
- `warning-service`
- `complaint-service`
- `regulation-operation-service`

## 19.4 验证前端是否可访问

浏览器打开：

```text
http://虚拟机IP/
```

## 19.5 验证网关是否通

后续可用：

```bash
curl http://虚拟机IP/api/health
```

如果 Nginx 已经转发 `/api`，这条命令可用于快速确认链路。

## 19.6 验证数据库初始化是否成功

进入 MySQL：

```bash
docker exec -it mysql mysql -uroot -p
```

输入密码后执行：

```sql
SHOW DATABASES;
USE food_user_db;
SHOW TABLES;
SELECT COUNT(*) FROM sys_user;

USE food_regulation_db;
SHOW TABLES;
SELECT COUNT(*) FROM food_enterprise;

USE food_regulation_operation_db;
SHOW TABLES;
SELECT COUNT(*) FROM inspection_task;

USE food_complaint_db;
SHOW TABLES;
SELECT COUNT(*) FROM complaint;

USE food_warning_db;
SHOW TABLES;
SELECT COUNT(*) FROM warning_record;
```

## 19.7 验证 MinIO 文件上传链路

这一步最终要通过前端功能验证：

- 企业备案附件上传
- 投诉图片上传
- 整改附件上传

如果上传失败，优先检查：

1. `MINIO_ENDPOINT`
2. `MINIO_PUBLIC_ENDPOINT`
3. MinIO bucket 是否成功创建
4. 浏览器是否能访问返回的文件地址

---

## 20. 当前阶段你可以先做什么

在我继续帮你补 Docker 文件之前，你现在就可以先完成下面这些工作。

### 20.1 在 CentOS 7 上安装 Docker 和 Compose

按本文档第 6 章执行。

### 20.2 建好部署目录

按本文档第 7 章执行。

### 20.3 把项目代码传到虚拟机

按本文档第 8 章执行。

### 20.4 记录这些关键信息

请提前记下：

- 虚拟机 IP
- 虚拟机可用内存
- 虚拟机可用磁盘
- 是否能访问外网拉镜像
- Docker 是用 `docker compose` 还是 `docker-compose`

这些信息会影响后续我给你的具体部署文件。

---

## 21. 后续需要我继续处理的事项

下面这些事情属于下一步正式落地阶段，我建议由我继续帮你完成。

`【后续需要改代码/补文件】`

### 21.1 必做

1. 新增后端通用 `Dockerfile`
2. 新增前端 `Dockerfile`
3. 新增 Nginx 生产配置
4. 新增 `docker-compose.yml`
5. 新增环境变量模板文件
6. 整理 MySQL 初始化 SQL 目录

### 21.2 高优先级检查

1. 检查 `complaint-service` 自动建表配置是否需要调整
2. 检查前端生产环境 API 地址配置
3. 检查 MinIO 的公网访问地址策略
4. 检查服务健康检查接口是否足够使用

### 21.3 可能需要微调代码的地方

1. 前端生产环境变量文件
2. 网关生产访问策略
3. 个别服务的容器启动参数
4. 日志输出目录与 JVM 参数

---

## 22. 推荐的下一步执行方式

建议你按下面顺序继续：

1. 先在虚拟机上完成 Docker 安装
2. 把虚拟机 IP、内存、磁盘、Docker 版本告诉我
3. 我再直接帮你补齐第一版容器化文件
4. 然后你在虚拟机中执行 `docker compose up -d`
5. 我再陪你逐项排查启动和联通问题

---

## 23. 结论

当前项目非常适合做“单 VM、单机 Docker Compose”的第一版容器化部署。

原因是：

- 项目边界清晰
- 服务依赖明确
- 配置大多已预留环境变量
- 前后端拆分结构天然适合容器化

目前真正缺的不是架构基础，而是部署文件和收口工作。

换句话说，这个项目已经具备容器化基础，只差把下面几件事补齐：

- Dockerfile
- Compose 编排
- Nginx 配置
- 环境变量模板
- SQL 初始化目录

这些下一步我可以继续直接帮你落到仓库里。
