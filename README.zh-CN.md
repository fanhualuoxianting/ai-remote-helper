# AI Remote Helper

[English](README.md) · [下载最新版 Windows LAN 包](https://github.com/fanhualuoxianting/ai-remote-helper/releases/latest)

[![CI](https://github.com/fanhualuoxianting/ai-remote-helper/actions/workflows/ci.yml/badge.svg)](https://github.com/fanhualuoxianting/ai-remote-helper/actions/workflows/ci.yml)

AI Remote Helper 是一个面向 AI Coding 场景的**可见、授权式远程开发协助平台**。它通过 JavaFX 桌面端、Spring Boot Relay Server 与 MCP 协议桥，组织需求审核、任务转发、受限执行、日志回传和审计记录；所有文件与命令操作都必须落在被协助方主动选择的授权目录内。

> **能力边界**
> 当前仓库定位为本地 / 局域网作品集 MVP，不是隐蔽远控工具。AI 层通过可见的 Codex 或 OpenClaw 会话生成结构化任务，再由协助方客户端或 MCP Bridge 提交；仓库目前**没有内置 OpenAI Function Calling 实现**，简历与面试表述应以实际代码为准。

<p align="center">
  <img src="docs/assets/airh-demo.png" width="880" alt="AI Remote Helper 桌面端演示" />
</p>

## 项目能证明什么

- **完整任务链路：**需求提交与审核、会话建立、Relay 转发、远端执行、结果回传和报告生成。
- **用户明确授权：**被协助方选择工作目录、保持客户端可见、实时查看日志并可随时断开。
- **目录安全沙箱：**路径标准化、路径穿越拦截、敏感文件保护与命令风险分级。
- **操作可审计：**任务状态、命令输出、文件变化、异常与审计事件均有记录。
- **AI 工具协作：**可见 Codex / OpenClaw Runner 与 MCP 客户端都不能绕过服务端和客户端安全检查。
- **桌面端交付：**JavaFX 客户端可打包为 Windows app-image，用于双机局域网演示。

## 架构

<p align="center">
  <img src="docs/assets/architecture.png" width="880" alt="AI Remote Helper 架构图" />
</p>

```text
Codex / OpenClaw / MCP Client
              |
              v
         MCP Bridge
              |
              v
         Relay Server  <---->  PostgreSQL / Redis
              |
              v
       JavaFX Agent Client
              |
              v
        用户授权工作目录
```

| 模块 | 作用 |
| --- | --- |
| `common-protocol` | 共享 DTO、任务状态、消息类型与枚举。 |
| `common-safety` | 路径沙箱、敏感文件保护和命令风险检测。 |
| `relay-server` | 设备、会话、任务、日志、审计与 WebSocket 路由。 |
| `agent-client` | 可见 JavaFX 客户端，负责授权、执行、日志、审核与断开控制。 |
| `mcp-bridge` | 将允许的 MCP 工具请求转发给 Relay。 |
| `web-console` | Vue 监控控制台原型，不是桌面演示的必需组件。 |

## 可演示流程

1. 被协助方启动 Agent Client，选择授权项目目录。
2. 协助方使用连接码建立远程会话。
3. 被协助方提交自然语言需求。
4. 协助方审核并批准需求。
5. 可见 Codex / OpenClaw 会话向本地队列写入结构化任务，或 MCP 客户端提交允许的工具请求。
6. 协助方客户端经 Relay 下发任务，远端 Agent 只在授权目录中执行。
7. 日志、任务结果、文件变化记录和会话报告返回可见界面。

## 快速启动

环境要求：Java 21+、Maven 3.9+、Docker Compose；JavaFX 打包建议使用 Windows 10/11。

```powershell
git clone https://github.com/fanhualuoxianting/ai-remote-helper.git
cd ai-remote-helper

docker compose up -d
mvn clean package
scripts\start-relay-lan.bat
mvn -f agent-client/pom.xml javafx:run
```

可选组件：

```powershell
# MCP Bridge
mvn -f mcp-bridge/pom.xml spring-boot:run

# Vue Console 原型
cd web-console
npm install
npm run dev
```

默认端口和双机局域网排障见 [docs/LAN_MODE.md](docs/LAN_MODE.md)。

## 安全边界

AI Remote Helper 明确不实现：

- 隐藏运行、静默安装或开机自启；
- 默认提权或安装系统服务；
- 接管鼠标键盘；
- 绕过授权目录；
- 读取浏览器数据、SSH 私钥或系统凭据；
- 关闭防火墙、安全软件或审计记录。

完整规则见 [SECURITY.md](SECURITY.md)。

## 验证

仓库包含路径沙箱、敏感文件保护、命令风险检测、任务持久化、报告生成、需求审核和 MCP 协议处理等测试。

```powershell
mvn clean package
```

GitHub Actions 会构建并测试 Java 模块、构建 Vue Console，并进行基础的已跟踪密钥扫描。

## 当前限制

- 认证与授权目前面向可信本地 / 局域网演示，进入共享网络或公网前仍需进一步加固。
- AI Runner 依赖外部可见 Codex / OpenClaw 进程，仓库尚未实现直接模型 API Planner。
- Vue Console 仍是原型，自动化测试覆盖弱于 Java 模块。
- Windows 产物签名、SBOM 与自动 Release 发布尚未纳入交付流水线。

剩余任务与验收标准见 [docs/CODEX_HANDOFF.md](docs/CODEX_HANDOFF.md)。

## 文档

- [安全模型](SECURITY.md)
- [系统架构](docs/ARCHITECTURE.md)
- [局域网模式](docs/LAN_MODE.md)
- [Windows 打包](docs/LAN_PACKAGING.md)
- [MCP 使用](docs/MCP_USAGE.md)
- [Web Console](docs/WEB_CONSOLE.md)
- [Roadmap](docs/ROADMAP.md)

## 许可证

MIT，见 [LICENSE](LICENSE)。
