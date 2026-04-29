# Phase 06：write_file / apply_patch（写入操作）

## 目标
Agent 端实现 `write_file` 和 `apply_patch` 任务，所有写入操作前自动创建备份。

## 约束
- 写入前必须在 `.ai-remote-helper/backups/` 目录创建备份
- 备份文件保留原目录结构
- 每个任务最多保留最近 3 个备份版本
- 写入操作限制在授权目录内
- 写入后返回 diff 摘要

## 任务清单

### Agent 端
- [ ] `BackupService`：备份管理
  - `backupFile(Path target)` → 备份到 `.ai-remote-helper/backups/{timestamp}/{relativePath}`
  - `listBackups(String relativePath)` → 备份列表
  - `cleanupOldBackups(String relativePath, int keepCount)` → 清理旧备份
- [ ] `FileSystemService` 扩展：
  - `writeFile(String relativePath, String content)` → 写入文件（带备份）
  - `applyPatch(String relativePath, String patch)` → 应用补丁（带备份）
  - 写入前后生成 diff 摘要
- [ ] TaskExecutor 增加 write_file / apply_patch handler
- [ ] Agent UI 显示写入操作的 diff 预览

### Relay Server 端
- [ ] 无需额外修改，复用 TaskService 存储结果

### 测试验证
- [ ] write_file 创建新文件成功
- [ ] write_file 修改已有文件前自动备份
- [ ] 备份文件可恢复
- [ ] write_file 超出授权目录返回错误
- [ ] apply_patch 正确应用补丁

## 提交信息
```
feat: implement write_file and apply_patch with backup
```
