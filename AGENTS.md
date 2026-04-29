# AI Remote Helper 项目工作协议

你正在开发 AI Remote Helper，这是一个"面向 AI 编程工具的授权远程开发协助平台"。

它不是木马，不是隐藏远控，不允许绕过用户授权。它的目标是：对方电脑安装 JavaFX Agent 客户端并主动授权工作目录后，操作者可以通过自己的 OpenClaw / Codex / Claude 调用 MCP 工具，远程协助对方修 bug、执行命令、读取/修改项目文件、查看日志、生成报告。对方电脑不需要安装 AI 工具，只需要运行 Agent 客户端，并且 Agent 客户端必须实时显示所有远程操作。

## 技术栈

- Java 21
- Maven 多模块
- Spring Boot 3
- Spring WebSocket / STOMP
- Spring Security
- JWT
- PostgreSQL 或 MySQL
- Redis
- JavaFX
- jpackage
- MCP Java SDK
- Web Console 预留 Vue / React 目录

## 模块结构

项目必须采用 Maven 多模块：

ai-remote-helper/
├── pom.xml
├── common-protocol/
├── common-safety/
├── relay-server/
├── agent-client/
├── mcp-bridge/
├── web-console/
├── docs/
├── tasks/

## 模块职责

common-protocol：
定义消息类型、任务类型、任务状态、权限类型、风险等级、DTO。

common-safety：
定义路径限制、危险命令检测、敏感文件保护、风险分级。

relay-server：
Spring Boot 中继服务器，负责 Agent 连接、会话管理、任务转发、日志持久化、权限校验和审计。

agent-client：
JavaFX 桌面客户端，安装在对方电脑，负责连接服务器、选择授权目录、执行任务、显示实时操作日志、一键断开。

mcp-bridge：
Java MCP Server，给 OpenClaw / Codex / Claude 调用，不直接执行本地命令，只调用 relay-server。

web-console：
后台管理界面，前期只预留目录和说明，后期再完善。

docs：
中文文档。

## 安全边界

严禁实现：
1. 隐藏运行
2. 绕过用户授权
3. 默认开机自启动
4. 后门持久化
5. 提权
6. 反调试
7. 反杀进程
8. 关闭防火墙
9. 关闭杀毒软件
10. 读取浏览器数据
11. 读取 SSH 私钥
12. 读取系统凭证
13. 访问授权目录外文件
14. 删除系统目录
15. 格式化磁盘
16. 修改注册表
17. 添加系统用户
18. 规避审计日志

必须实现：
1. Agent 客户端始终可见
2. 用户必须手动选择授权目录
3. 所有文件操作限制在授权目录内
4. 所有命令执行 cwd 限制在授权目录内
5. 支持一键断开
6. 支持会话有效期
7. 支持危险命令拦截
8. 支持任务超时
9. 支持文件修改前备份
10. 支持操作日志审计
11. 支持中文 report.md 生成

## 开发规则

1. 不要一次性实现全部项目。
2. 每次只完成 tasks/phase-xx.md 中指定的阶段。
3. 不要做后续阶段内容。
4. 不要把所有代码写进一个文件。
5. 每个模块都要有清晰 package 分层。
6. Spring Boot 模块按 controller、service、repository、domain、dto、config 分层。
7. Agent 模块按 ui、connection、executor、filesystem、safety、audit、report 分层。
8. 每阶段完成后必须更新 DEVELOPMENT_REPORT.md。
9. 每阶段完成后必须说明修改了哪些文件、如何运行、如何测试、当前问题、下一步建议。
10. 能运行测试就运行测试；暂时没有测试时，至少运行 Maven 构建检查。
