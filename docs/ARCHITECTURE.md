# 架构说明

## 系统架构

```text
┌─────────────────────────────────────────────────────────────┐
│                    AI Remote Helper                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │  MCP Bridge  │───▶│ Relay Server │◀───│ Agent Client │  │
│  │  (Port 9090) │    │ (Port 8080)  │    │  (JavaFX)    │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│         │                   │                   │          │
│         ▼                   ▼                   ▼          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │  AI Tools    │    │  PostgreSQL  │    │  File System │  │
│  │  (OpenClaw/  │    │  (Port 15432)│    │  (Authorized)│  │
│  │   Codex)     │    └──────────────┘    └──────────────┘  │
│  └──────────────┘            │                             │
│         │                    ▼                             │
│         │            ┌──────────────┐                      │
│         │            │    Redis     │                      │
│         │            │ (Port 16379) │                      │
│         │            └──────────────┘                      │
│         │                                                  │
│         ▼                                                  │
│  ┌──────────────┐                                          │
│  │ Web Console  │                                          │
│  │ (Port 3000)  │                                          │
│  └──────────────┘                                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 模块依赖关系

```text
common-protocol
       │
       ▼
common-safety
       │
       ▼
┌──────────────┬──────────────┬──────────────┐
│ relay-server │ agent-client │  mcp-bridge  │
└──────────────┴──────────────┴──────────────┘
```

## 核心流程

### 1. Agent 连接流程

```text
Agent Client                    Relay Server
    │                               │
    │──── WebSocket Connect ───────▶│
    │                               │
    │◀─── Session Created ─────────│
    │                               │
    │──── Heartbeat ──────────────▶│
    │                               │
```

### 2. 任务执行流程

```text
MCP Bridge / Web Console        Relay Server                    Agent Client
    │                               │                               │
    │──── Create Task ─────────────▶│                               │
    │                               │──── Forward Task ────────────▶│
    │                               │                               │
    │                               │◀─── Task Log (stream) ───────│
    │                               │                               │
    │                               │◀─── Task Result ─────────────│
    │                               │                               │
    │◀─── Return Result ──────────│                               │
```

### 3. 安全检查流程

```text
Agent Client
    │
    ├─▶ PathGuard.resolveSafePath()
    │       │
    │       ├─▶ 检查路径穿越
    │       ├─▶ 检查绝对路径
    │       └─▶ 检查是否在授权目录内
    │
    ├─▶ SensitiveFileGuard.checkFileAccess()
    │       │
    │       ├─▶ 检查敏感文件
    │       ├─▶ 检查敏感目录
    │       └─▶ 检查敏感扩展名
    │
    └─▶ CommandRiskDetector.detect()
            │
            ├─▶ 检查 BLOCKED 模式
            ├─▶ 检查 HIGH 模式
            ├─▶ 检查 MEDIUM 模式
            └─▶ 返回风险等级
```

## 数据库设计

### 核心表

- `devices`：设备信息
- `sessions`：会话信息
- `tasks`：任务记录
- `task_logs`：任务日志
- `file_changes`：文件修改记录
- `audit_events`：审计事件

### Redis 用途

- 在线设备状态
- Session Code 到 Session ID 映射
- 运行中任务状态
- WebSocket 连接状态

## 安全边界

### 授权目录限制

所有文件操作必须在用户选择的授权目录内。

### 路径穿越防护

使用 Java NIO Path 进行路径解析和标准化。

### 敏感文件保护

阻止访问 SSH 密钥、云凭证、浏览器数据等。

### 危险命令拦截

四级风险评估：LOW、MEDIUM、HIGH、BLOCKED。

## 扩展点

### 添加新任务类型

1. 在 `TaskType` 枚举中添加新类型
2. 在 `TaskExecutor` 中添加执行逻辑
3. 在 `McpProtocolHandler` 中添加工具映射

### 添加新安全规则

1. 在 `CommandRiskDetector` 中添加新模式
2. 在 `SensitiveFileGuard` 中添加新规则

### 添加新 API

1. 在 `relay-server` 中创建新的 Controller
2. 在 `web-console` 中添加对应的前端页面
