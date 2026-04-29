# Phase 04：授权目录选择与路径沙箱

## 目标
Agent 端实现授权目录选择机制，所有后续文件操作必须限制在授权目录内（路径沙箱）。

## 约束
- Agent 启动后必须先选择授权目录才能连接
- 所有文件路径必须校验是否在授权目录内（防止路径穿越）
- 授权目录信息通过连接协议传递给 Relay Server
- Relay Server 记录每个 Agent 的授权目录

## 任务清单

### Agent 端
- [ ] `AuthorizedDirectoryChooser`：JavaFX 目录选择器对话框
- [ ] `PathSandbox`：路径校验工具类
  - `isUnderAuthorizedDir(Path target)` — 检查目标路径是否在授权目录内
  - `resolveSecurely(String relativePath)` — 安全解析相对路径，防止 `../` 穿越
  - `normalize(String path)` — 路径标准化
- [ ] AgentConnectionClient 连接时发送 `authorizedDirectory` 字段
- [ ] 未选择授权目录时禁止连接

### Relay Server 端
- [ ] DeviceConnection record 增加 `authorizedDirectory` 字段
- [ ] DeviceRegistry 记录并暴露 `getAuthorizedDirectory(deviceId)`
- [ ] 后续任务下发时携带 `authorizedDirectory` 信息

### 测试验证
- [ ] 路径穿越测试：`../../../etc/passwd` 应被拒绝
- [ ] 正常相对路径测试：`src/main.java` 应被允许
- [ ] 连接后通过 `/api/devices/online` 可见授权目录

## 提交信息
```
feat: implement authorized directory selection and path sandbox
```
