# 架构说明

AI Remote Helper 采用多模块结构，核心链路规划如下：

```text
OpenClaw / Codex / Claude
        |
        v
    mcp-bridge
        |
        v
    relay-server
        |
        v
    agent-client
        |
        v
  用户授权工作目录
```

## 设计原则

- Agent 端必须由用户主动运行，并且界面始终可见。
- Agent 端必须由用户手动选择授权目录。
- relay-server 只负责鉴权、会话、任务转发和审计，不直接操作被协助方文件系统。
- mcp-bridge 不直接执行本地命令，只调用 relay-server。
- 所有危险能力必须经过安全模块检查。

## Phase 01 已建模块

- `common-protocol`：定义协议枚举和基础 DTO。
- `common-safety`：定义基础安全规则框架。
- `relay-server`：空 Spring Boot 服务。
- `agent-client`：基础 JavaFX 可见窗口。
- `mcp-bridge`：MCP Bridge Java 模块占位。

## Phase 01 未实现内容

- Agent 与 relay-server 的真实连接。
- MCP 工具注册和调用。
- 任务路由、命令执行、文件读取写入。
- 数据库、Redis、JWT。
- Web Console 管理后台。
