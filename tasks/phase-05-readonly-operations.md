# Phase 05：list_dir / read_file（只读操作）

## 目标
Agent 端实现 `list_dir` 和 `read_file` 任务的真实执行（只读），所有操作限制在授权目录内。

## 约束
- 只允许读取授权目录内的文件
- 大文件限制读取大小（默认最大 1MB）
- 二进制文件不读取内容，仅返回文件信息
- 结果通过 STOMP `/app/agent/task-result` 返回
- 任务执行必须异步，不阻塞 UI 线程

## 任务清单

### Agent 端
- [ ] `FileSystemService`：文件系统操作服务
  - `listDirectory(String relativePath)` → 文件/子目录列表（名称、大小、修改时间、是否目录）
  - `readFile(String relativePath)` → 文件内容（文本）或文件信息（二进制）
  - 所有路径通过 PathSandbox 校验
- [ ] `TaskExecutor`：任务执行调度器
  - 根据 taskType 分发到对应 handler
  - 异步执行，结果包装为 TaskResultMessage
  - 捕获异常并返回错误结果
- [ ] AgentConnectionClient 中替换模拟执行为真实执行
- [ ] Agent UI 显示文件列表和文件内容（可折叠）

### Relay Server 端
- [ ] TaskService 接收并存储真实任务结果
- [ ] TaskController 查询接口返回真实 output/summary

### 测试验证
- [ ] list_dir 返回授权目录下真实文件列表
- [ ] read_file 返回真实文件内容
- [ ] read_file 超出授权目录返回错误
- [ ] read_file 二进制文件返回文件信息而非内容

## 提交信息
```
feat: implement list_dir and read_file read-only operations
```
