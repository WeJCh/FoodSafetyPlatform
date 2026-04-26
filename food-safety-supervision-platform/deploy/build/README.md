# 本地构建产物说明

本目录用于“方向 B”部署方式：

- 在本机先构建后端可执行 jar
- 在本机构建前端 `dist`
- 再把构建产物和部署文件上传到虚拟机

## 后端模块是否支持可执行 jar

结论：支持。

原因：

1. 七个后端服务模块都显式声明了 `spring-boot-maven-plugin`
2. 七个后端服务模块都有 `@SpringBootApplication` 启动类
3. 各服务模块没有声明 `war` 打包，默认是 `jar`
4. 根工程虽然是 `packaging=pom`，但子模块仍然是标准 Spring Boot jar 模块

对应模块：

- `user-service`
- `gateway-service`
- `regulation-service`
- `regulation-operation-service`
- `complaint-service`
- `query-service`
- `warning-service`

## 构建命令

### Windows PowerShell

```powershell
.\deploy\build\build-artifacts.ps1
```

### Linux / Git Bash

```bash
bash deploy/build/build-artifacts.sh
```

## 期望产物

后端：

- `user-service/target/user-service-0.0.1-SNAPSHOT.jar`
- `gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar`
- `regulation-service/target/regulation-service-0.0.1-SNAPSHOT.jar`
- `regulation-operation-service/target/regulation-operation-service-0.0.1-SNAPSHOT.jar`
- `complaint-service/target/complaint-service-0.0.1-SNAPSHOT.jar`
- `query-service/target/query-service-0.0.1-SNAPSHOT.jar`
- `warning-service/target/warning-service-0.0.1-SNAPSHOT.jar`

前端：

- `food-web/dist/`

## 上传到虚拟机时至少要包含的内容

- 七个后端模块的 `target/*.jar`
- `food-web/dist/`
- `docker-compose.yml`
- `.env`
- `Dockerfile.backend`
- `food-web/Dockerfile`
- `deploy/nginx/`
- `deploy/mysql/init/`
