# Phase 06：实现文件写入、补丁应用和自动备份

## 目标

实现 Agent 端授权目录内的 write_file 和 apply_patch 能力。

本阶段允许修改授权目录内的项目文件，但必须有路径沙箱、安全检查、修改前备份、文件修改记录和 Agent UI 显示。

本阶段仍然不实现命令执行。

## 前置条件

Phase 05 已经完成：
- list_dir
- read_file
- PathGuard
- SensitiveFileGuard
- Agent UI 操作日志

## agent-client 要实现

1. write_file

要求：
- 接收 WRITE_FILE 任务
- 使用 PathGuard 校验目标路径
- 使用 SensitiveFileGuard 判断是否允许写入
- 写入前自动备份原文件
- 如果目标文件不存在，校验父目录在授权目录内
- 默认只允许写文本文件
- 写入后计算 beforeHash 和 afterHash
- 返回写入结果

2. apply_patch

要求：
- 接收 APPLY_PATCH 任务
- 支持标准 unified diff patch 或项目内定义的简单 patch 格式
- 应用 patch 前备份原文件
- patch 失败不能破坏原文件
- 返回 patch 成功/失败原因
- 优先实现单文件 patch
- 多文件 patch 后续扩展

3. 自动备份

备份目录：
- 授权目录/.ai-remote-helper/backups

备份文件命名建议：
- 原文件名 + 时间戳 + hash

备份记录包含：
- taskId
- originalPath
- backupPath
- beforeHash
- afterHash
- timestamp

4. UI 显示

Agent UI 必须显示：
- AI 准备修改文件：xxx
- 已创建备份：xxx
- AI 写入文件：xxx
- AI 应用补丁：xxx
- 修改成功/失败

5. 文件修改记录

在本地和 relay-server 都要记录 file_changes。

## relay-server 要实现

1. 接收 file_change 记录
2. 保存或内存记录：
- sessionId
- taskId
- filePath
- backupPath
- beforeHash
- afterHash
- changeType
- createdAt

3. API：
- GET /api/sessions/{sessionId}/file-changes

## common-protocol 要补充

DTO：
- WriteFileRequest
- WriteFileResult
- ApplyPatchRequest
- ApplyPatchResult
- FileChangeRecord

## 安全要求

1. 不允许写授权目录外文件。
2. 不允许写敏感文件。
3. 不允许删除文件。
4. 不允许修改系统目录。
5. 不允许修改浏览器、SSH、凭证相关目录。
6. 修改前必须备份。
7. patch 失败必须保持原文件可恢复。
8. 本阶段不实现命令执行。

## 文档要求

更新：
- DEVELOPMENT_REPORT.md
- SECURITY.md
- README.md

说明：
- write_file 用法
- apply_patch 用法
- 备份目录
- 回滚规划
- 文件修改审计

## 验收标准

1. write_file 能修改授权目录内文本文件。
2. write_file 修改前会备份。
3. apply_patch 能修改授权目录内单文件。
4. patch 失败不会破坏原文件。
5. 修改授权目录外文件会 BLOCKED。
6. 修改敏感文件会 BLOCKED。
7. Agent UI 显示文件修改行为。
8. relay-server 能查询 file_changes。
9. mvn clean package 通过。
10. 本阶段没有实现命令执行。
