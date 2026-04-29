# AI Remote Helper

AI Remote Helper 是一个面向 AI 编程工具的授权远程开发协助平台。目标场景是：被协助方主动运行可见的 JavaFX Agent 客户端，手动选择授权工作目录；协助方通过 OpenClaw / Codex / Claude 调用 MCP 工具，经 relay-server 转发任务，帮助对方排查项目问题、查看日志、修改授权目录内的项目文件并生成报告。

本项目不是隐藏远控，不做隐蔽运行，不绕过用户授权，不提供开机自启、提权、持久化后门、关闭安全软件、读取系统凭证等能力。

## 模块结构

```text
ai-remote-helper/
├── pom.xml
├── common-protocol/
├── common-safety/
├── relay-server/
├── agent-client/
├── mcp-bridge/
├── web-console/
├── docs/
└── tasks/
```

## 模块说明

- `common-protocol`：消息类型、任务类型、任务状态、权限类型、风险等级和基础 DTO。
- `common-safety`：路径限制、危险命令检测、敏感文件保护的基础框架。
- `relay-server`：Spring Boot 3 中继服务，本阶段只提供空服务和健康检查占位。
- `agent-client`：JavaFX 桌面客户端，本阶段只提供基础可见窗口。
- `mcp-bridge`：未来提供 MCP Server 能力，本阶段只保留 Java 模块结构。
- `web-console`：后台管理界面预留目录。
- `docs`：中文架构和路线图文档。

## 本地开发

环境要求：

- Java 21
- Maven 3.9 或兼容版本

构建全部模块：

```powershell
mvn clean package
```

启动 relay-server：

```powershell
mvn -pl relay-server spring-boot:run
```

启动 agent-client：

```powershell
mvn -pl agent-client javafx:run
```

## 当前阶段

当前完成 Phase 01：Java 版 Maven 多模块项目骨架。没有实现真实远程命令执行、文件修改、Agent 连接、MCP 工具调用或 Web 管理后台。
