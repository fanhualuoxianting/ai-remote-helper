# Phase 10：MCP Bridge 基础实现

## 目标
实现 mcp-bridge 模块基础：MCP 协议适配层，将外部 MCP 请求转换为内部任务协议。

## 技术要求
- MCP Bridge 作为独立 Spring Boot 应用运行
- 实现 MCP SSE 传输层（HTTP + SSE 双向通信）
- 实现 MCP 协议的核心方法：
  - `tools/list`：返回可用工具列表
  - `tools/call`：调用工具（映射到内部任务类型）
- 工具映射：
  - `list_directory` → `LIST_DIR`
  - `read_file` → `READ_FILE`
  - `write_file` → `WRITE_FILE`
  - `apply_patch` → `APPLY_PATCH`
  - `run_command` → `RUN_COMMAND`
- MCP Bridge 通过 WebSocket 连接到 Relay Server
- MCP 请求转换为 TaskRequest，通过 Relay 转发给 Agent
- 响应转换回 MCP 格式

## 涉及模块
- `mcp-bridge`
- `common-protocol`（TaskType、TaskRequest、TaskResultMessage）

## 文件清单
```
mcp-bridge/src/main/java/com/airh/mcp/
├── McpBridgeApplication.java
├── transport/
│   └── SseTransportHandler.java
├── protocol/
│   ├── McpRequest.java
│   ├── McpResponse.java
│   ├── McpToolDefinition.java
│   └── McpProtocolHandler.java
├── bridge/
│   ├── TaskBridge.java
│   └── ToolRegistry.java
└── config/
    └── McpBridgeConfig.java
mcp-bridge/src/main/resources/application.yml
mcp-bridge/src/test/java/com/airh/mcp/protocol/McpProtocolHandlerTest.java
```

## 提交
```bash
git commit -m "feat: implement MCP bridge with SSE transport (Phase 10)"
```

## 验证
- 测试通过
- MCP Bridge 可独立启动
- SSE 端点可访问
