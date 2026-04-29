# AI Remote Helper 开发报告

## 项目概述

AI Remote Helper 是一个授权远程开发协助工具，允许 AI 通过 MCP 协议远程操作已授权的开发工作站。

## 技术栈

- **后端**：Java 21 + Spring Boot 3.3.5
- **前端**：Vue 3 + Vite
- **数据库**：PostgreSQL 17
- **缓存**：Redis
- **协议**：WebSocket/STOMP + MCP SSE
- **打包**：jpackage

## 模块说明

### common-protocol

通用协议定义，包含：
- DTO：任务请求、结果、文件项、日志等
- 枚举：任务类型、状态、风险等级、消息类型

### common-safety

安全模块，包含：
- `PathGuard`：路径沙箱，防止路径穿越
- `SensitiveFileGuard`：敏感文件保护
- `CommandRiskDetector`：危险命令检测
- `SecurityDecision`：安全检查结果

### relay-server

中继服务器，包含：
- 设备管理
- 会话管理
- 任务调度
- 日志存储
- 审计记录
- REST API
- WebSocket 支持

### agent-client

Agent 客户端，包含：
- JavaFX UI
- 文件系统操作
- 命令执行
- 报告生成
- 安全检查

### mcp-bridge

MCP 协议桥接，包含：
- MCP SSE 传输层
- 工具注册表
- 任务桥接

### web-console

Web 控制台前端，包含：
- 设备管理
- 会话查看
- 任务日志
- 文件修改记录

## 核心功能

### 1. 授权目录管理

- 用户选择授权工作目录
- 路径沙箱限制所有操作
- 防止路径穿越攻击

### 2. 文件操作

- `list_dir`：列出目录内容
- `read_file`：读取文本文件
- `write_file`：写入文件（自动备份）
- `apply_patch`：应用补丁（自动备份）

### 3. 命令执行

- `run_command`：执行 shell 命令
- stdout/stderr 实时流式输出
- 超时控制
- 任务终止

### 4. 安全保护

- 危险命令拦截
- 敏感文件保护
- 操作审计日志

### 5. MCP 协议支持

- MCP SSE 传输
- 10 个远程工具
- 参数校验

## 运行命令

### 构建项目

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package
```

### 启动 Relay Server

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f relay-server\pom.xml spring-boot:run
```

### 启动 Agent Client

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f agent-client\pom.xml javafx:run
```

### 启动 MCP Bridge

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f mcp-bridge\pom.xml spring-boot:run
```

### 启动 Web Console

```powershell
cd web-console
npm install
npm run dev
```

## 测试结果

```
common-safety: Tests run: 17, Failures: 0, Errors: 0
relay-server: Tests run: 2, Failures: 0, Errors: 0
agent-client: Tests run: 19, Failures: 0, Errors: 0
mcp-bridge: Tests run: 6, Failures: 0, Errors: 0
BUILD SUCCESS
```

## 已知问题

1. MCP Bridge 需要手动连接到 Relay Server
2. Web Console 前端需要独立启动
3. jpackage 打包需要 Java 21+ 环境

## 后续计划

1. 完善 Web Console 功能
2. 添加用户认证
3. 支持多 Agent 协作
4. 添加文件同步功能
5. 支持 macOS/Linux 打包

## 安全说明

详见 [SECURITY.md](SECURITY.md)

## 打包说明

详见 [docs/PACKAGING.md](docs/PACKAGING.md)
