# Phase 08：危险命令拦截 + 敏感文件保护

## 目标
Agent 端实现危险命令拦截和敏感文件保护机制，防止执行破坏性操作。

## 约束
- 黑名单模式拦截已知危险命令
- 敏感文件路径保护（SSH 密钥、浏览器数据、系统文件等）
- 拦截必须在 Agent 端本地完成，不依赖 Server
- 拦截操作必须记录审计日志
- 拦截规则可配置（`safety-rules.json`）

## 任务清单

### Agent 端
- [ ] `SafetyChecker`：安全检查服务
  - `checkCommand(String command)` → 命令风险检查
  - `checkFilePath(String path)` → 文件路径检查
  - 返回检查结果（ALLOW / WARN / DENY）及原因
- [ ] 危险命令黑名单（默认规则）：
  - `rm -rf /`、`rm -rf /*`、`format`、`del /s /q`
  - `shutdown`、`reboot`、`init 0`
  - `reg delete`、`bcdedit`
  - 权限提升：`sudo`、`runas`、`net user`
  - 远程下载执行：`curl | sh`、`wget | sh`、`Invoke-Expression`
- [ ] 敏感文件保护（默认规则）：
  - `~/.ssh/*`、`~/.gnupg/*`
  - 浏览器数据目录（Chrome、Firefox、Edge）
  - 系统文件（`/etc/passwd`、`C:\Windows\System32`）
  - Git 凭据（`.gitconfig`、`.git-credentials`）
- [ ] `SafetyConfig`：安全规则配置类
  - 从 `safety-rules.json` 加载自定义规则
  - 支持按 taskType 配置不同规则
- [ ] 拦截结果展示在 Agent UI
- [ ] 拦截事件发送审计日志

### Relay Server 端
- [ ] `AuditEvent` DTO
- [ ] TaskService 记录拦截事件

### 测试验证
- [ ] 危险命令 `rm -rf /` 被拦截
- [ ] 访问 `~/.ssh/id_rsa` 被拦截
- [ ] 正常命令不受影响
- [ ] 拦截事件出现在审计日志

## 提交信息
```
feat: implement dangerous command interception and sensitive file protection
```
