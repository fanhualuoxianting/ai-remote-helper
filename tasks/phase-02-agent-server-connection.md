# Phase 02：实现 Agent 与 Relay Server 的基础连接

## 目标

实现 agent-client 主动连接 relay-server 的基础 WebSocket/STOMP 连接闭环。

本阶段只做连接、心跳、在线状态、连接码显示，不做远程命令执行。

## relay-server 要实现

1. WebSocket/STOMP 基础配置
   - 提供 Agent 连接端点
   - 提供 Controller/MCP 预留端点
   - 支持心跳
   - 支持连接断开处理

2. 设备在线状态
   - Agent 连接后注册 deviceId
   - 生成 sessionCode 或 connectionCode
   - 记录在线状态
   - 断开后标记离线

3. 基础 REST API
   - GET /api/devices/online 查询在线设备
   - GET /api/health 健康检查

4. 包结构建议
   - config
   - websocket
   - device
   - session
   - task
   - audit

5. 暂时可以使用内存存储
   - 本阶段可以先不用数据库
   - 但代码结构要方便后续接 PostgreSQL/Redis

## agent-client 要实现

1. JavaFX UI 增强
   - 服务器地址输入框
   - 连接按钮
   - 断开按钮
   - 当前连接状态
   - 当前 deviceId
   - 当前连接码
   - 授权目录显示
   - 实时日志区域

2. 连接逻辑
   - 点击连接后连接 relay-server
   - 连接成功后 UI 显示已连接
   - 显示服务器返回的连接码
   - 断开后 UI 显示已断开
   - 断线后可手动重连

3. 日志显示
   - 连接中
   - 连接成功
   - 连接失败
   - 断开连接
   - 收到服务端消息

## common-protocol 要补充

定义基础消息：
- AgentHelloMessage
- AgentOnlineMessage
- HeartbeatMessage
- ErrorMessage

## 安全要求

1. 本阶段仍然不实现命令执行。
2. 不实现文件读写。
3. 不隐藏运行。
4. Agent UI 必须可见。
5. 连接断开后不得继续接收任务。

## 文档要求

更新 DEVELOPMENT_REPORT.md：
- 本阶段实现了什么
- 修改了哪些文件
- 如何启动 relay-server
- 如何启动 agent-client
- 如何验证连接
- 当前问题
- 下一步计划

## 验收标准

1. relay-server 能正常启动。
2. agent-client 能输入服务器地址并连接。
3. 连接成功后 agent-client 显示连接码。
4. relay-server 能看到在线设备。
5. 调用 GET /api/devices/online 能返回在线设备。
6. 断开连接后状态能更新。
7. 没有实现远程命令执行。
