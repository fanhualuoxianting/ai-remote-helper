# AI Remote Helper · Codex 接力清单

> 目标：把当前“可演示作品集 MVP”继续收敛成可稳定复现、可安全解释、与简历表述完全一致的项目。
>
> 接手原则：先验证现状，再改代码；不得为了让演示“看起来成功”而绕过授权、伪造结果或吞掉异常。

## 一、审查结论

当前仓库最有价值的证据不是“模型直接控制远端电脑”，而是以下工程链路：

1. JavaFX 被协助方客户端保持可见，并由用户主动选择授权工作目录；
2. Spring Boot Relay 管理设备、会话、任务、日志、审计与 WebSocket 路由；
3. 文件和命令操作经过目录沙箱、敏感文件保护与风险检测；
4. 协助方审核自然语言需求后，启动可见 Codex / OpenClaw Runner；
5. AI 通过本地 JSON 任务队列或 MCP Bridge 提交结构化操作；
6. 执行结果、文件变化和报告回传到可见界面。

**简历风险：**当前代码没有可核验的内置 OpenAI API / Function Calling Planner。若简历继续写“接入 OpenAI 兼容接口与 Function Calling”，必须先完成下方 P0-1；否则应改成“接入 Codex / OpenClaw 与 MCP，通过审核后的结构化任务驱动受限远程执行”。

## 二、P0：必须先完成

### P0-1：在“补实现”和“改简历”之间做明确选择

#### 方案 A：补一个真实、可测试的模型 Planner

目标：用户输入自然语言后，模型只生成候选工具调用；真正授权、风险判断和执行仍由现有 Java 安全链路负责。

建议新增：

- `ai-planner` 独立模块，或放入 `relay-server` 的独立 package；
- OpenAI-compatible Chat Completions / Responses Adapter；
- 强类型工具 Schema：`list_dir`、`read_file`、`write_file`、`run_command`；
- 模型输出解析、未知工具拒绝、参数上限、一次受限修复；
- Planner 只能产生候选任务，不能直接访问文件系统、执行命令或跳过人工审核；
- 使用本地 fake HTTP server 做契约测试，不在测试中调用真实收费 API。

验收标准：

- 未配置 Key 时明确返回 `MODEL_UNAVAILABLE`，不得静默伪装成功；
- 模型返回未知工具、非法 JSON、绝对路径、目录穿越或超长参数时被拒绝；
- 高风险命令仍由现有安全模块阻断；
- README、示例配置和简历描述能对应到真实类、接口和测试。

#### 方案 B：不补模型 Planner，统一修改对外表述

需要检查并修改：简历、个人网站、项目 PPT、投递表单与 GitHub 简介。统一使用：

> 通过可见 Codex / OpenClaw Runner 与 MCP Bridge，将审核后的自然语言需求转成结构化任务，并在用户授权目录内完成受限执行、日志回传和审计。

不要再写：

- 已实现 OpenAI Function Calling；
- 模型可直接远程控制电脑；
- 已完成公网生产部署；
- 已达到生产级远控安全。

### P0-2：补 Relay WebSocket 端到端集成测试

覆盖至少以下链路：

1. Agent 上线并注册；
2. Helper 创建会话；
3. 下发 `LIST_DIR` / `READ_FILE`；
4. Agent 返回日志和结果；
5. 超时任务进入 `TIMEOUT`；
6. Agent 断线后任务失败并返回稳定错误；
7. 重连后不会重复执行已完成任务；
8. 不同会话之间不能读取对方任务或日志。

建议使用 Testcontainers PostgreSQL / Redis，避免只依赖 mock 验证路由正确性。

验收命令：

```powershell
mvn -B clean test
```

### P0-3：把共享网络认证补到可解释水平

最低要求：

- 设备注册令牌不可明文持久化；
- 连接码有过期时间、使用次数限制和重放防护；
- Helper、Agent、MCP 三类调用方使用不同权限；
- WebSocket 握手和后续消息都校验会话归属；
- 登录、连接码尝试和任务提交增加限流；
- 生产 profile 不允许使用开发数据库口令；
- 文档明确 TLS 终止位置和反向代理配置。

不要把“局域网能连通”描述成“公网安全可用”。

## 三、P1：作品集质量提升

### P1-1：前端依赖与测试

当前 `web-console` 没有提交 lockfile，也没有测试脚本。

完成项：

- 生成并提交 `package-lock.json`；
- 将 CI 中的 `npm install` 改为 `npm ci`；
- 至少增加 API client、错误态和关键页面渲染测试；
- 执行 `npm audit`，升级高危依赖并记录无法升级项。

验收命令：

```powershell
cd web-console
npm ci
npm run build
npm audit --audit-level=high
```

### P1-2：一键演示与证据

新增一个只用于本地演示的脚本，完成：

- 检查 Java、Maven、Docker 与端口；
- 启动 PostgreSQL / Redis / Relay；
- 输出 Agent 与 Helper 的启动命令；
- 跑一条不修改文件的链路自检；
- 失败时保留日志并返回非零退出码。

补充以下公开材料：

- 60～90 秒 GIF 或短视频；
- 一张授权目录界面截图；
- 一张需求审核与 AI Runner 截图；
- 一张任务日志 / 报告截图；
- 一张真实架构图，节点名称与代码模块一致。

### P1-3：Release 工程化

- GitHub Actions 构建 Windows app-image；
- 生成 SHA-256、SBOM 与版本说明；
- 产物上传 GitHub Release，不提交到仓库；
- 有条件时增加代码签名；没有证书时必须明确标注“未签名测试版”。

## 四、P2：可选增强

- 将 helper 审核与执行权限做成细粒度 capability；
- 为文件写入增加 diff 预览和二次确认；
- 为命令执行增加工作目录、环境变量和输出大小上限；
- 给审计事件增加 traceId / sessionId / taskId 关联查询；
- 增加 OpenTelemetry 指标与链路，但不要为了堆技术栈引入不必要的微服务。

## 五、禁止事项

Codex 接手时不得实现或引入：

- 隐藏窗口、静默安装、开机自启或系统服务持久化；
- 绕过授权目录或读取浏览器、SSH、云凭据；
- 默认管理员权限；
- 无人工审核的任意 Shell；
- `--dangerously-skip-permissions` 等绕过式 AI Runner 参数；
- 将 API Key、数据库密码、设备令牌写入仓库或日志；
- 通过前端假数据掩盖 Relay / Agent 未联通。

## 六、给 Codex 的首轮执行指令

```text
先完整阅读 README.md、README.zh-CN.md、SECURITY.md、docs/ARCHITECTURE.md、
docs/LAN_MODE.md 和本文件。不要直接新增功能。

第一轮只做：
1. 运行 mvn -B clean test 和 web-console 构建，记录真实结果；
2. 画出现有需求审核 -> AI Runner -> JSON 队列/MCP -> Relay -> Agent 的真实调用链；
3. 搜索所有 OpenAI、Function Calling、公网部署和生产级安全相关表述，列出“代码已支持/仅文档宣称/未实现”；
4. 为 P0-1 给出“补 Planner”与“改简历”两套最小改动方案；
5. 不改 main，创建独立分支并提交一份 REVIEW_REPORT.md 供确认。

任何无法通过代码或测试证明的能力都不得写进 README 或简历。
```

## 七、完成定义

只有同时满足以下条件，才可将本接力任务标记为完成：

- GitHub Actions 全部通过；
- 本地双机 LAN 演示可复现；
- 关键安全拒绝路径有自动化测试；
- README、简历、PPT 和实际代码没有能力错位；
- Release 产物不包含密钥、数据库文件、日志或用户目录；
- 所有“生产级”“安全”“Function Calling”等强表述都有对应实现和证据。
