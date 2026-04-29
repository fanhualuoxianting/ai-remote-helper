# Web Console 使用说明

## 概述

Web Console 是 AI Remote Helper 的管理界面，用于查看在线设备、会话、任务日志、文件修改记录和报告。

## 启动方式

### 前置条件

- Node.js 18+
- npm 或 pnpm

### 启动步骤

```powershell
cd E:\openclaw-project\ai-remote-helper\web-console
npm install
npm run dev
```

Web Console 默认运行在端口 3000。

### 访问地址

```
http://localhost:3000
```

## 功能说明

### 在线设备页

显示所有在线的 Agent 设备：
- 设备名称
- 设备状态
- 最后在线时间

### 会话管理

查看和管理远程会话：
- 会话 ID
- 授权目录
- 会话状态
- 结束会话

### 任务日志

查看任务执行详情：
- 任务类型
- 任务状态
- 风险等级
- stdout/stderr 日志

### 文件修改记录

查看文件修改历史：
- 文件路径
- 备份路径
- 修改前后 Hash
- 修改类型

### 审计日志

查看安全审计记录：
- 操作类型
- 风险等级
- 操作结果
- 时间戳

### 报告查看

查看和生成会话报告：
- Markdown 格式展示
- 导出功能

## 技术栈

- Vue 3
- Vite
- Axios

## API 代理

开发模式下，Vite 会自动将 `/api` 请求代理到 `http://localhost:8080`。

## 安全说明

- Web Console 不直接执行命令
- Web Console 只调用 Relay Server API
- 不在前端存储敏感 token
- 日志显示有长度限制
- Markdown 渲染经过安全处理
