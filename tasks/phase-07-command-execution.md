# Phase 07：run_command（命令执行）

## 目标
Agent 端实现 `run_command` 任务，支持 stdout/stderr 实时流式输出、超时控制、进程强杀。

## 约束
- 命令执行限制工作目录在授权目录内
- 默认超时 30 秒，可自定义（最大 300 秒）
- 超时后必须强杀进程及其子进程
- stdout/stderr 通过 STOMP 日志消息实时流式转发
- 不支持交互式命令（stdin 关闭）
- 命令通过 `ProcessBuilder` 执行，使用系统 shell

## 任务清单

### Agent 端
- [ ] `CommandExecutor`：命令执行服务
  - `execute(String command, String workDir, int timeoutSeconds)` → 异步执行
  - 实时读取 stdout/stderr 并通过 callback 发送日志
  - 超时强杀（含子进程树）
  - 返回 exit code + 完整输出
- [ ] `TaskExecutor` 增加 run_command handler
- [ ] AgentConnectionClient 中发送日志通过 STOMP `/app/agent/task-log`
- [ ] Agent UI 实时显示命令输出（追加模式）

### Relay Server 端
- [ ] TaskService 接收并存储流式日志
- [ ] TaskController 查询接口支持分页日志

### 测试验证
- [ ] 简单命令执行（`echo hello`）返回正确输出
- [ ] 长时间命令超时被强杀
- [ ] 工作目录限制在授权目录内
- [ ] exit code 正确传递
- [ ] stdout 和 stderr 分别正确传递

## 提交信息
```
feat: implement run_command with streaming output and timeout
```
