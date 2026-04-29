# Phase 09: 数据库集成（PostgreSQL + Redis）

## 目标

将 Relay Server 中的任务记录和审计日志持久化到 PostgreSQL，并使用 Redis 做会话状态缓存。

## 前提

- Docker PostgreSQL 已运行：`localhost:15432`，用户 `postgres`，密码 `postgres`，数据库 `testdb`
- Docker Redis 已运行：`localhost:16379`（如未运行，需要启动）

## 任务

### 1. 在 relay-server 引入 Spring Data JPA + PostgreSQL 驱动

在 `relay-server/pom.xml` 添加：
- `spring-boot-starter-data-jpa`
- `postgresql` 驱动

### 2. 创建实体类

在 `com.airh.relay.domain` 包下创建：

- `TaskRecordEntity`（映射 `task_records` 表）：
  - `taskId`（UUID，主键）
  - `sessionId`
  - `taskType`（枚举字符串）
  - `status`（枚举字符串）
  - `payload`（JSON 字符串）
  - `summary`
  - `output`
  - `error`
  - `createdAt`
  - `updatedAt`
  - `completedAt`

- `AuditEventEntity`（映射 `audit_events` 表）：
  - `eventId`（UUID，主键）
  - `sessionId`
  - `eventType`（枚举字符串）
  - `detail`（JSON 字符串）
  - `createdAt`

### 3. 创建 Repository 接口

- `TaskRecordRepository extends JpaRepository<TaskRecordEntity, String>`
- `AuditEventRepository extends JpaRepository<AuditEventEntity, String>`

### 4. 创建数据库配置

- `DatabaseConfig`：启用 JPA 审计（`@EnableJpaAuditing`）
- `application.yml` 配置数据源：
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://localhost:15432/testdb
      username: postgres
      password: postgres
    jpa:
      hibernate:
        ddl-auto: update
      show-sql: false
  ```

### 5. 在 TaskService 中集成数据库

- 任务创建时写入 `task_records`
- 任务状态更新时更新 `task_records`
- 查询任务从数据库读取

### 6. 创建 Redis 配置

- `RedisConfig`：配置 RedisTemplate
- `SessionStateCache`：用 Redis 缓存在线会话状态（TTL 5 分钟）

### 7. 创建数据库迁移脚本

在 `relay-server/src/main/resources/db/migration/` 创建 `V1__init.sql`：
- `CREATE TABLE IF NOT EXISTS task_records`
- `CREATE TABLE IF NOT EXISTS audit_events`

### 8. 添加依赖

在 relay-server/pom.xml 添加：
- `spring-boot-starter-data-redis`（如需 Redis）

### 9. 编写测试

- `TaskRecordRepositoryTest`：测试 CRUD
- `AuditEventRepositoryTest`：测试 CRUD

### 10. 构建验证

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package
```

### 11. 更新文档

- `DEVELOPMENT_REPORT.md`
- `复现记录.md`

## 安全提醒

- 数据库连接信息只存在于 application.yml，不硬编码到代码
- 审计日志不可删除
- 不得将数据库凭据提交到公开仓库
