# MCP 使用说明

## 概述

MCP Bridge 是 AI Remote Helper 的协议桥接层，允许 OpenClaw、Codex、Claude 等 AI 工具通过标准 MCP 协议远程操作已授权的 Agent。

## 启动方式

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f mcp-bridge\pom.xml spring-boot:run
```

MCP Bridge 默认运行在端口 9090。

## MCP 端点

- SSE 端点：`http://localhost:9090/mcp/sse`
- 消息端点：`http://localhost:9090/mcp/messages`

## 可用工具

### 设备和会话管理

| 工具 | 说明 | 参数 |
|------|------|------|
| `remote_list_devices` | 列出所有在线设备 | 无 |
| `remote_connect_session` | 连接到远程会话 | `sessionCode` |

### 文件操作

| 工具 | 说明 | 参数 |
|------|------|------|
| `remote_list_dir` | 列出远程目录内容 | `path` |
| `remote_read_file` | 读取远程文件 | `path` |
| `remote_write_file` | 写入远程文件 | `path`, `content` |
| `remote_apply_patch` | 应用补丁 | `path`, `patch` |

### 命令执行

| 工具 | 说明 | 参数 |
|------|------|------|
| `remote_run_command` | 执行远程命令 | `command`, `cwd`, `timeoutSeconds` |
| `remote_get_task_logs` | 获取任务日志 | `taskId` |
| `remote_kill_task` | 终止任务 | `taskId` |

### 报告

| 工具 | 说明 | 参数 |
|------|------|------|
| `remote_generate_report` | 生成会话报告 | `sessionId` |

## OpenClaw 接入

在 OpenClaw 配置中添加 MCP 服务器：

```toml
[mcp_servers.remote_helper]
url = "http://localhost:9090/mcp/sse"
```

## Claude / Codex 接入

待验证具体配置方式。

## 安全说明

- MCP Bridge 不直接执行本地命令
- MCP Bridge 不直接读写本地文件
- 所有操作通过 Relay Server 转发
- 所有操作受授权目录限制
- 所有操作有审计日志

## 示例提示词

```
请帮我查看远程机器上的项目文件结构，然后读取 README.md 文件内容。
```

```
请在远程机器上执行 `npm install` 命令安装依赖。
```

```
请帮我修改远程机器上的 package.json 文件，添加一个新的依赖。
```
