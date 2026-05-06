# 部署说明

## 环境要求

- Java 21+ (推荐 Temurin)
- Maven 3.9.9 (项目内置)
- PostgreSQL 17
- Redis
- Node.js 18+ (Web Console)

## 数据库准备

### PostgreSQL

```powershell
# 使用 Docker 启动 PostgreSQL
docker run -d --name my-postgres `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -e POSTGRES_DB=testdb `
  -p 15432:5432 `
  postgres:17
```

上面的 `postgres/postgres` 只适合本地开发示例，生产或共享网络部署必须改成独立强密码并通过安全配置注入。

### Redis

```powershell
# 使用 Docker 启动 Redis
docker run -d --name my-redis `
  -p 16379:6379 `
  redis
```

## 构建项目

```powershell
git clone https://github.com/<your-name>/ai-remote-helper.git
cd ai-remote-helper
mvn clean package
```

## 启动服务

### 1. 启动 Relay Server

```powershell
mvn -f relay-server/pom.xml spring-boot:run
```

Relay Server 默认运行在端口 8080。

### 2. 启动 Agent Client

```powershell
mvn -f agent-client/pom.xml javafx:run
```

Agent Client 启动后需要：
1. 选择授权工作目录
2. 输入 Relay Server 地址（默认 http://localhost:8080）
3. 点击连接

### 3. 启动 MCP Bridge

```powershell
mvn -f mcp-bridge/pom.xml spring-boot:run
```

MCP Bridge 默认运行在端口 9090。

### 4. 启动 Web Console

```powershell
cd web-console
npm install
npm run dev
```

Web Console 默认运行在端口 3000。

## 配置文件

### application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${AIRH_DATASOURCE_URL:jdbc:postgresql://localhost:15432/testdb}
    username: ${AIRH_DATASOURCE_USERNAME:postgres}
    password: ${AIRH_DATASOURCE_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

  data:
    redis:
      host: ${AIRH_REDIS_HOST:localhost}
      port: ${AIRH_REDIS_PORT:16379}
```

生产或共享网络部署建议通过环境变量覆盖：

```powershell
$env:AIRH_DATASOURCE_URL='jdbc:postgresql://db-host:5432/airh'
$env:AIRH_DATASOURCE_USERNAME='airh'
$env:AIRH_DATASOURCE_PASSWORD='<strong-password>'
$env:AIRH_REDIS_HOST='redis-host'
$env:AIRH_REDIS_PORT='6379'
```

## 打包 Agent 客户端

```powershell
.\scripts\package-agent.ps1
```

或

```powershell
.\scripts\package-agent.bat
```

打包产物在 `dist/AI-Remote-Helper-Agent` 目录下。

## 验证部署

1. 访问 http://localhost:8080/console/health 检查 Relay Server 状态
2. 访问 http://localhost:3000 查看 Web Console
3. 在 Agent Client 中选择授权目录并连接
4. 通过 MCP Bridge 或 Web Console 执行操作

## 常见问题

### PostgreSQL 连接失败

检查 PostgreSQL 是否正常运行：
```powershell
docker ps | Select-String "my-postgres"
```

### Redis 连接失败

检查 Redis 是否正常运行：
```powershell
docker ps | Select-String "my-redis"
```

### Agent Client 无法启动

确保 JAVA_HOME 指向 JDK 21+，并包含 JavaFX 模块。

### MCP Bridge 无法连接

确保 Relay Server 已启动，并检查 MCP Bridge 配置中的 relay-url 是否正确。
