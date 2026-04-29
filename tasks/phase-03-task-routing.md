# Phase 03：实现任务转发协议，不执行真实命令

## 目标

实现 Controller/MCP 侧向 relay-server 创建任务，relay-server 将任务转发给指定 agent-client，agent-client 接收任务并返回模拟结果。

本阶段只做任务转发闭环，不执行真实命令，不读写真实文件。

## relay-server 要实现

1. 任务创建 API
   - POST /api/sessions/{sessionId}/tasks
   - 请求体包含 taskType、payload、timeoutSeconds
   - 返回 taskId

2. 任务转发
   - 根据 sessionId 找到对应 Agent WebSocket 连接
   - 将任务消息发送给 agent-client
   - 保存任务状态
   - 接收 agent-client 返回的 TASK_RESULT
   - 支持查询任务结果

3. 任务查询 API
   - GET /api/tasks/{taskId}
   - GET /api/tasks/{taskId}/logs

4. 任务状态
   - PENDING
   - RUNNING
   - SUCCESS
   - FAILED
   - TIMEOUT
   - CANCELLED
   - BLOCKED

5. 日志
   - 支持 TASK_LOG 消息
   - relay-server 暂时可以内存保存日志

## agent-client 要实现

1. 接收任务消息
   - 在 UI 日志区显示收到任务
   - 显示 taskId、taskType、payload 摘要

2. 返回模拟结果
   - LIST_DIR 返回模拟目录
   - READ_FILE 返回模拟内容
   - RUN_COMMAND 返回模拟 stdout/stderr
   - WRITE_FILE 返回模拟成功
   - APPLY_PATCH 返回模拟成功

3. UI 显示
   - 当前任务
   - 任务开始
   - 任务日志
   - 任务结束

## common-protocol 要补充

DTO：
- CreateTaskRequest
- CreateTaskResponse
- TaskLogMessage
- TaskResultMessage
- TaskPayload

## 安全要求

1. 本阶段仍然不执行真实命令。
2. 不读取真实文件。
3. 不写入真实文件。
4. 只做协议和任务流转。
5. 所有任务都必须在 Agent UI 显示。

## 文档要求

更新 DEVELOPMENT_REPORT.md：
- 本阶段实现的任务转发流程
- API 使用说明
- 如何测试
- 当前仍未实现真实执行的说明
- 下一阶段计划

## 验收标准

1. Agent 在线后，relay-server 能向指定 Agent 下发任务。
2. Agent UI 能显示收到任务。
3. Agent 能返回模拟结果。
4. relay-server 能查询任务结果。
5. relay-server 能查询任务日志。
6. 仍然没有真实执行命令或真实读写文件。
