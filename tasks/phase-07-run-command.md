# Phase 07：实现真实命令执行和流式日志

## 目标

实现 Agent 端授权目录内的 run_command 能力。

本阶段允许执行命令，但必须受授权目录、危险命令拦截、超时、kill、实时日志、审计记录限制。

## 前置条件

Phase 04-06 已完成：
- 授权目录
- 路径沙箱
- 敏感文件保护
- 只读文件能力
- 文件写入和备份能力

## agent-client 要实现

1. run_command

要求：
- 接收 RUN_COMMAND 任务
- 校验 cwd 必须在授权目录内
- 使用 CommandRiskDetector 检查命令风险
- HIGH / BLOCKED 风险命令默认拒绝
- MEDIUM 风险命令记录明显警告
- LOW 风险命令可执行
- 使用 ProcessBuilder 执行命令
- 捕获 stdout
- 捕获 stderr
- 实时流式返回 TASK_LOG
- 命令结束返回 exitCode
- 支持 timeoutSeconds
- 支持 kill_task
- 记录 startTime、endTime、duration

2. 命令执行模式

优先支持：
- 直接进程执行
- Windows 下必要时使用 cmd /c
- 暂时不要默认使用 powershell
- 如果使用 powershell，必须阻止 -enc / -encodedcommand

3. 并发限制

第一版只允许每个 Agent 同时执行一个命令任务。
如果已有命令运行，新任务返回 FAILED 或 QUEUED，按现有协议设计处理。

4. UI 显示

Agent UI 必须显示：
- AI 请求执行命令：xxx
- cwd：xxx
- riskLevel：xxx
- stdout 实时输出
- stderr 实时输出
- exitCode
- 是否超时
- 是否被终止

5. kill_task

实现：
- 当前运行任务可被终止
- 终止后返回 CANCELLED
- UI 显示任务已终止

## common-safety 要实现

CommandRiskDetector：

LOW：
- pwd
- ls
- dir
- git status
- npm run dev
- npm test
- pnpm dev
- mvn test
- gradle test
- python app.py
- python train.py
- nvidia-smi

MEDIUM：
- npm install
- pnpm install
- pip install
- mvn package
- gradle build
- docker compose up
- git pull

BLOCKED：
- rm -rf
- del /s
- rd /s
- format
- reg delete
- net user
- powershell -enc
- powershell -encodedcommand
- curl xxx | sh
- wget xxx | sh
- chmod 777 /
- shutdown
- taskkill 杀安全软件
- 修改系统 PATH
- 修改注册表
- 访问系统目录
- 访问用户隐私目录
- 静默运行未知 exe/msi

## relay-server 要实现

1. 流式 TASK_LOG 存储
2. TASK_RESULT 存储
3. kill_task 转发给 Agent
4. 审计日志记录：
- command 摘要
- cwd
- riskLevel
- exitCode
- duration
- blockedReason

## common-protocol 要补充

DTO：
- RunCommandRequest
- RunCommandResult
- KillTaskRequest
- CommandLogChunk

字段：
- command
- cwd
- timeoutSeconds
- exitCode
- stdoutPreview
- stderrPreview
- durationMs
- killed
- timedOut

## 安全要求

1. cwd 必须在授权目录内。
2. BLOCKED 命令不执行。
3. HIGH 风险命令默认不执行。
4. 禁止默认管理员权限。
5. 禁止隐藏执行。
6. 禁止 powershell -enc。
7. 禁止 curl/wget 管道执行 shell。
8. 禁止系统级破坏命令。
9. 所有命令必须显示在 Agent UI。
10. 所有输出必须记录审计。

## 文档要求

更新：
- DEVELOPMENT_REPORT.md
- SECURITY.md
- README.md

说明：
- run_command 用法
- 风险等级
- 被阻止命令
- timeout 和 kill
- stdout/stderr 日志

## 验收标准

1. 能执行授权目录内的低风险命令。
2. stdout/stderr 能实时显示在 Agent UI。
3. stdout/stderr 能回传 relay-server。
4. 命令结束返回 exitCode。
5. timeout 能终止长时间任务。
6. kill_task 能终止正在运行的命令。
7. cwd 在授权目录外会 BLOCKED。
8. BLOCKED 命令不会执行。
9. powershell -enc 被阻止。
10. mvn clean package 通过。
