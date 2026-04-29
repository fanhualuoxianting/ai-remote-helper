# 部署说明

本文档记录 AI Remote Helper 后续部署方向。Phase 01 只创建项目骨架，不包含生产部署能力。

## relay-server 后续部署规划

relay-server 计划作为 Spring Boot 服务部署，后续会接入：

- PostgreSQL 或 MySQL：保存用户、设备、会话、任务和审计日志。
- Redis：保存在线状态、会话状态和短期任务状态。
- WebSocket / STOMP：维护 Agent 长连接。
- JWT：保护操作者、Agent 和 MCP Bridge 的访问。

后续构建命令：

```powershell
mvn -pl relay-server clean package
```

后续运行命令：

```powershell
java -jar relay-server\target\relay-server-0.1.0-SNAPSHOT.jar
```

## agent-client 后续打包规划

agent-client 计划使用 JavaFX + jpackage 打包为桌面客户端。客户端必须可见运行，用户手动选择授权目录，并提供一键断开入口。

后续开发运行命令：

```powershell
mvn -pl agent-client javafx:run
```

后续 jpackage 打包命令会在实现客户端功能后补充。

## 当前阶段限制

Phase 01 没有数据库、Redis、JWT、WebSocket 真实连接、Agent 安装包或 MCP 工具能力。
