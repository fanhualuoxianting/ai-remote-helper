# 路线图

## Phase 01：项目骨架

- Maven 多模块结构。
- 基础协议枚举和 DTO。
- 基础安全规则框架。
- 空 Spring Boot relay-server。
- 基础 JavaFX agent-client 窗口。
- MCP Bridge 模块占位。
- 中文文档。

## Phase 02：Agent 与 Server 连接

- Agent 配置 relay-server 地址。
- Agent 主动连接 relay-server。
- relay-server 维护在线 Agent 状态。
- 基础断开和重连流程。

## Phase 03：任务路由

- mcp-bridge 提交任务到 relay-server。
- relay-server 将任务转发给对应 Agent。
- Agent 返回任务状态和日志。

## 后续阶段

- 授权目录内文件操作。
- 命令执行和危险命令拦截。
- 审计日志持久化。
- report.md 生成。
- Web Console 管理后台。
- Agent 桌面安装包。
