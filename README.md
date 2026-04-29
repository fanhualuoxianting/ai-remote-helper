# AI Remote Helper

授权远程开发协助工具 — 允许 AI 通过 MCP 协议远程操作已授权的开发工作站。

## 项目概述

AI Remote Helper 是一个安全的远程开发协助平台，通过授权目录限制、路径沙箱、敏感文件保护和危险命令拦截，确保远程操作的安全性。

## 核心特性

- 🔒 **授权目录限制**：所有操作必须在用户选择的授权目录内
- 🛡️ **路径沙箱**：防止路径穿越攻击
- 🔐 **敏感文件保护**：阻止访问 SSH 密钥、云凭证等
- ⚠️ **危险命令拦截**：四级风险评估
- 📝 **操作审计**：所有操作都有审计日志
- 🔄 **自动备份**：文件修改前自动备份

## 模块说明

| 模块 | 说明 |
|------|------|
| `common-protocol` | 通用协议定义（DTO、枚举） |
| `common-safety` | 安全模块（路径沙箱、敏感文件保护、危险命令检测） |
| `relay-server` | 中继服务器（设备管理、任务调度、日志存储） |
| `agent-client` | Agent 客户端（JavaFX UI、文件操作、命令执行） |
| `mcp-bridge` | MCP 协议桥接（SSE 传输、工具注册） |
| `web-console` | Web 控制台前端（Vue 3） |

## 快速开始

### 环境要求

- Java 21+
- Maven 3.9.9（项目内置）
- PostgreSQL 17
- Redis
- Node.js 18+（Web Console）

### 构建项目

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package
```

### 启动服务

```powershell
# 1. 启动 Relay Server
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f relay-server\pom.xml spring-boot:run

# 2. 启动 Agent Client
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f agent-client\pom.xml javafx:run

# 3. 启动 MCP Bridge
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f mcp-bridge\pom.xml spring-boot:run

# 4. 启动 Web Console
cd web-console
npm install
npm run dev
```

## 使用流程

1. 启动 Relay Server（端口 8080）
2. 启动 Agent Client，选择授权目录
3. Agent 连接到 Relay Server
4. 通过 MCP Bridge 或 Web Console 执行操作
5. 所有操作在授权目录内执行

## MCP 工具

| 工具 | 说明 |
|------|------|
| `remote_list_devices` | 列出在线设备 |
| `remote_connect_session` | 连接会话 |
| `remote_list_dir` | 列出目录内容 |
| `remote_read_file` | 读取文件 |
| `remote_write_file` | 写入文件 |
| `remote_apply_patch` | 应用补丁 |
| `remote_run_command` | 执行命令 |
| `remote_get_task_logs` | 获取任务日志 |
| `remote_kill_task` | 终止任务 |
| `remote_generate_report` | 生成报告 |

## 安全说明

详见 [SECURITY.md](SECURITY.md)

## 部署说明

详见 [DEPLOY.md](DEPLOY.md)

## 架构说明

详见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## MCP 使用说明

详见 [docs/MCP_USAGE.md](docs/MCP_USAGE.md)

## 打包说明

详见 [docs/PACKAGING.md](docs/PACKAGING.md)

## Web Console 说明

详见 [docs/WEB_CONSOLE.md](docs/WEB_CONSOLE.md)

## 开发报告

详见 [DEVELOPMENT_REPORT.md](DEVELOPMENT_REPORT.md)

## 路线图

- [x] Phase 01-03：项目骨架、连接、任务路由
- [x] Phase 04-07：安全边界、文件操作、命令执行
- [x] Phase 08-10：持久化、MCP Bridge、报告生成
- [x] Phase 11-13：打包、Web Console、最终集成

## 许可证

私有项目
