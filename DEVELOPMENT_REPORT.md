# DEVELOPMENT_REPORT

## Phase 01：创建 Java 版项目骨架

### 本次任务目标

根据 `tasks/phase-01-project-skeleton.md` 创建 AI Remote Helper 的 Java 版 Maven 多模块项目骨架，只完成 Phase 01，不实现后续阶段能力。

### 实际完成内容

- 创建父级 `pom.xml`，配置 Java 21、Maven 多模块、统一 dependencyManagement 和 pluginManagement。
- 创建 `common-protocol` 模块，定义消息类型、任务类型、任务状态、权限类型、风险等级和基础 DTO。
- 创建 `common-safety` 模块，定义命令风险检测、路径限制、敏感文件保护的基础框架。
- 创建 `relay-server` 模块，提供 Spring Boot 3 空服务和 `/health` 占位接口，并预留 controller、service、repository、domain、dto、config、websocket、session、device、task、audit 包。
- 为 `relay-server` 添加最小 Spring Security 配置，放行 `/health`，其它请求保持认证要求。
- 创建 `agent-client` 模块，提供 JavaFX 基础窗口，显示项目名称、当前状态、授权目录、日志区域、连接和断开按钮占位。
- 创建 `mcp-bridge` 模块，预留 tools、client、config 包。
- 创建 `web-console` 预留目录和 README。
- 创建 `docs/ARCHITECTURE.md`、`docs/ROADMAP.md`。
- 创建中文 `README.md`、`SECURITY.md`、`DEPLOY.md`。
- 未实现真实远程命令执行、文件读写、Agent 连接、MCP 工具调用、隐藏运行、开机自启、提权等后续能力。

### 修改/新增文件

- `pom.xml`
- `README.md`
- `SECURITY.md`
- `DEPLOY.md`
- `DEVELOPMENT_REPORT.md`
- `common-protocol/pom.xml`
- `common-protocol/src/main/java/com/airh/protocol/enums/*.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/*.java`
- `common-safety/pom.xml`
- `common-safety/src/main/java/com/airh/safety/*.java`
- `relay-server/pom.xml`
- `relay-server/src/main/java/com/airh/relay/**/*.java`
- `relay-server/src/main/resources/application.yml`
- `agent-client/pom.xml`
- `agent-client/src/main/java/com/airh/agent/**/*.java`
- `mcp-bridge/pom.xml`
- `mcp-bridge/src/main/java/com/airh/mcp/**/*.java`
- `web-console/README.md`
- `docs/ARCHITECTURE.md`
- `docs/ROADMAP.md`

### 运行命令

已执行：

```powershell
java -version
where.exe mvn
where.exe java
Get-ChildItem -Path C:\,D:\,E:\ -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue | Select-Object -First 10 -ExpandProperty FullName
Get-ChildItem -Recurse -Filter pom.xml | ForEach-Object { [xml](Get-Content -Raw -LiteralPath $_.FullName) | Out-Null; $_.FullName }
javac -encoding UTF-8 -d .verify\classes <common-protocol/common-safety/mcp-bridge Java 源文件>
```

已尝试执行 Maven 构建检查：

```powershell
mvn clean package
```

### 测试结果

- Java 已确认存在：OpenJDK 21.0.9。
- `javac` 已确认存在：21.0.9。
- `common-protocol`、`common-safety`、`mcp-bridge` 中不依赖外部库的 Java 源码已通过 `javac` 编译检查。
- 所有 `pom.xml` 已通过 XML 解析检查。
- 已尝试执行 `mvn clean package`，但当前 PATH 未找到 `mvn`，常见路径检查也未发现 Maven，全盘 `mvn.cmd` 查找 30 秒超时。因此 Maven 构建检查本轮无法完成，需要在安装 Maven 或配置 PATH 后重新执行。

### 当前问题

- 本机当前 PowerShell 环境中 `mvn` 不可用，`where.exe mvn` 未找到 Maven 可执行文件。

### 下一阶段计划

下一阶段应严格按 `tasks/phase-02-agent-server-connection.md` 执行，开始实现 Agent 与 relay-server 的基础连接能力。不要在 Phase 01 中提前实现任务执行、文件操作或 MCP 工具能力。

## Phase 02：实现 Agent 与 Relay Server 的基础连接

### 本次任务目标

根据 `tasks/phase-02-agent-server-connection.md`，只实现 Agent 与 Relay Server 的基础 WebSocket/STOMP 连接闭环，包括连接、断开、心跳、在线状态、连接码显示和在线设备查询。不实现远程命令执行、文件读写、MCP 工具和任务转发。

### 实际完成内容

- 在 `common-protocol` 中补充基础连接消息：
  - `AgentHelloMessage`
  - `AgentOnlineMessage`
  - `HeartbeatMessage`
  - `ErrorMessage`
- 在 `relay-server` 中新增 WebSocket/STOMP 配置：
  - Agent 连接端点：`/ws/agent`
  - Controller/MCP 预留端点：`/ws/controller`
  - 应用消息前缀：`/app`
  - 简单 broker：`/topic`
  - STOMP 心跳配置
- 在 `relay-server` 中新增内存设备注册表：
  - Agent hello 后注册 `deviceId`
  - 生成 `sessionId`
  - 生成 6 位 `connectionCode`
  - 记录在线状态和最近心跳时间
  - WebSocket 断开后按 STOMP session 标记离线
- 在 `relay-server` 中新增 REST API：
  - `GET /api/health`
  - `GET /api/devices/online`
- 在 `agent-client` 中增强 JavaFX UI：
  - 服务器地址输入框
  - 手动选择授权目录按钮
  - 连接/断开按钮
  - 当前连接状态
  - 当前 `deviceId`
  - 当前连接码
  - 授权目录显示
  - 实时日志区域
- 在 `agent-client` 中新增 STOMP 客户端连接逻辑：
  - 点击连接后连接 relay-server
  - 连接成功后发送 Agent hello
  - 收到服务端在线确认后显示连接码
  - 定时发送 heartbeat
  - 断开后停止 heartbeat，并允许手动重连
- 保持 Phase 01 安全边界：
  - 未实现命令执行
  - 未实现文件读写
  - 未实现任务转发
  - 未实现 MCP 工具
  - Agent UI 必须可见
  - 连接前要求用户手动选择授权目录

### 修改/新增文件

- `common-protocol/src/main/java/com/airh/protocol/enums/MessageType.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/AgentHelloMessage.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/AgentOnlineMessage.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/HeartbeatMessage.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/ErrorMessage.java`
- `relay-server/src/main/java/com/airh/relay/config/SecurityConfig.java`
- `relay-server/src/main/java/com/airh/relay/config/WebSocketConfig.java`
- `relay-server/src/main/java/com/airh/relay/controller/HealthController.java`
- `relay-server/src/main/java/com/airh/relay/controller/DeviceController.java`
- `relay-server/src/main/java/com/airh/relay/device/DeviceConnection.java`
- `relay-server/src/main/java/com/airh/relay/device/DeviceRegistry.java`
- `relay-server/src/main/java/com/airh/relay/websocket/AgentConnectionController.java`
- `relay-server/src/main/java/com/airh/relay/websocket/AgentWebSocketEventListener.java`
- `agent-client/pom.xml`
- `agent-client/src/main/java/com/airh/agent/connection/AgentConnectionClient.java`
- `agent-client/src/main/java/com/airh/agent/connection/AgentConnectionListener.java`
- `agent-client/src/main/java/com/airh/agent/ui/AgentClientApplication.java`
- `DEVELOPMENT_REPORT.md`
- `复现记录.md`

### 如何启动 relay-server

如果系统 PATH 中已有 Maven：

```powershell
cd E:\openclaw-project\ai-remote-helper
mvn -f relay-server\pom.xml spring-boot:run
```

如果本机没有 `mvn`，可使用本次验证下载的项目局部 Maven：

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f relay-server\pom.xml spring-boot:run
```

如果 8080 端口被占用，可临时切换端口：

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f relay-server\pom.xml spring-boot:run -Dspring-boot.run.arguments=--server.port=18081
```

### 如何启动 agent-client

未在本轮自动启动 JavaFX GUI，以避免在后台打开不可控窗口；已通过 Maven 编译检查。可手动执行：

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f agent-client\pom.xml javafx:run
```

启动后：

1. 服务器地址填写 `http://localhost:8080`，如果 relay-server 使用临时端口则填写 `http://localhost:18081`。
2. 点击“选择授权目录”，手动选择工作目录。
3. 点击“连接”。
4. UI 显示“已连接”和 6 位连接码。
5. 点击“断开”后，UI 回到未连接状态。

### 如何验证连接

健康检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

查询在线设备：

```powershell
Invoke-RestMethod http://localhost:8080/api/devices/online
```

本轮实际在 18081 端口完成最小启动验证：

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -f relay-server\pom.xml spring-boot:run -Dspring-boot.run.arguments=--server.port=18081
Invoke-RestMethod http://localhost:18081/api/health
Invoke-RestMethod http://localhost:18081/api/devices/online
```

实际结果：

```text
GET /api/health -> {"status":"UP"}
GET /api/devices/online -> []
```

### 构建检查

本机 PowerShell 中 `mvn` 不在 PATH，因此先下载项目局部 Maven 3.9.9 到 `.tools`：

```powershell
New-Item -ItemType Directory -Force -Path .tools | Out-Null
$zip = Join-Path (Resolve-Path .tools) 'apache-maven-3.9.9-bin.zip'
Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip' -OutFile $zip
Expand-Archive -Path $zip -DestinationPath .tools -Force
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -version
```

已执行 Maven 构建检查：

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package
```

结果：`BUILD SUCCESS`。所有模块编译通过；当前项目没有测试源码，Maven 输出 `No tests to run`。

### 当前问题

- 系统 PATH 中仍未找到 `mvn`，本轮使用 `.tools\apache-maven-3.9.9\bin\mvn.cmd` 完成构建。
- 本机 8080 端口已被 PID 8116 占用，因此最小启动验证使用临时端口 18081。
- 本轮未自动启动 JavaFX 图形界面进行人工点击验证；连接逻辑已通过编译，启动和操作步骤已补充。

### 下一步计划

下一阶段应严格按 `tasks/phase-03-task-routing.md` 执行，开始任务转发前仍要保持授权目录、可见 UI、断开后不接收任务等安全边界。本阶段不要补做远程命令执行、文件读写或 MCP 工具能力。

## Phase 03：实现任务转发协议，不执行真实命令

### 本次任务目标

根据 `tasks/phase-03-task-routing.md`，只完成任务转发协议闭环：Controller/MCP 侧通过 relay-server 创建任务，relay-server 将任务下发给指定在线 Agent，Agent UI 显示任务并返回模拟结果。严格不执行真实命令，不读取真实文件，不写入真实文件，不做后续阶段。

### 实际完成内容

- `common-protocol` 补充 Phase 03 DTO：
  - `CreateTaskRequest`
  - `CreateTaskResponse`
  - `TaskPayload`
  - `TaskLogMessage`
  - `TaskResultMessage`
- `common-protocol` 调整任务协议：
  - `TaskType` 新增 `LIST_DIR`、`APPLY_PATCH`
  - `TaskStatus` 对齐 `PENDING`、`RUNNING`、`SUCCESS`、`FAILED`、`TIMEOUT`、`CANCELLED`、`BLOCKED`
  - `RemoteTask` 使用 `taskType` 和 `TaskPayload`
  - `TaskResult` 增加 `stderr`
- `relay-server` 新增内存任务服务和 REST API：
  - `POST /api/sessions/{sessionId}/tasks`
  - `GET /api/tasks/{taskId}`
  - `GET /api/tasks/{taskId}/logs`
- `relay-server` 根据 `sessionId` 查找在线 Agent，并通过 `/topic/agent/{deviceId}/events` 下发 `RemoteTask`。
- `relay-server` 新增 STOMP 消息处理：
  - `/app/agent/task-log`
  - `/app/agent/task-result`
- `relay-server` 暂时用内存保存任务状态和任务日志，不接数据库。
- `agent-client` 接收任务后在 UI 日志区显示 `taskId`、`taskType`、payload 摘要、任务开始和任务结束。
- `agent-client` 对所有任务类型返回模拟结果：
  - `LIST_DIR` 返回模拟目录列表
  - `READ_FILE` 返回模拟文件内容
  - `RUN_COMMAND` 返回模拟 stdout/stderr
  - `WRITE_FILE` 返回模拟成功
  - `APPLY_PATCH` 返回模拟成功
- 保持安全边界：本阶段没有加入任何真实命令执行、真实文件读取、真实文件写入或补丁应用逻辑。

### API 使用说明

创建任务：

```powershell
$body = @{
  taskType = "LIST_DIR"
  payload = @{
    data = @{
      path = "."
    }
  }
  timeoutSeconds = 30
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/sessions/<sessionId>/tasks" -ContentType "application/json" -Body $body
```

查询任务结果：

```powershell
Invoke-RestMethod "http://localhost:8080/api/tasks/<taskId>"
```

查询任务日志：

```powershell
Invoke-RestMethod "http://localhost:8080/api/tasks/<taskId>/logs"
```

### 修改/新增文件

- `common-protocol/src/main/java/com/airh/protocol/enums/TaskType.java`
- `common-protocol/src/main/java/com/airh/protocol/enums/TaskStatus.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/RemoteTask.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/TaskResult.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/CreateTaskRequest.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/CreateTaskResponse.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/TaskPayload.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/TaskLogMessage.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/TaskResultMessage.java`
- `relay-server/src/main/java/com/airh/relay/config/SecurityConfig.java`
- `relay-server/src/main/java/com/airh/relay/controller/TaskController.java`
- `relay-server/src/main/java/com/airh/relay/device/DeviceRegistry.java`
- `relay-server/src/main/java/com/airh/relay/task/TaskRecord.java`
- `relay-server/src/main/java/com/airh/relay/task/TaskService.java`
- `relay-server/src/main/java/com/airh/relay/websocket/AgentConnectionController.java`
- `agent-client/src/main/java/com/airh/agent/connection/AgentConnectionClient.java`
- `agent-client/src/main/java/com/airh/agent/connection/AgentConnectionListener.java`
- `agent-client/src/main/java/com/airh/agent/ui/AgentClientApplication.java`
- `DEVELOPMENT_REPORT.md`
- `复现记录.md`

### 构建检查

按本阶段额外要求，使用项目局部 Maven `.tools/apache-maven-3.9.9/bin/mvn.cmd` 构建，并先安装父 POM 和 common 模块。

已执行：

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd install -N
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -pl common-protocol,common-safety install
.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package
```

结果：`BUILD SUCCESS`。所有模块编译通过；当前项目没有测试源码，Maven 输出 `No tests to run`。

### 如何测试

1. 启动 relay-server。
2. 启动 agent-client。
3. 在 Agent UI 中手动选择授权目录并连接 relay-server。
4. 调用 `GET /api/devices/online` 获取在线 Agent 的 `sessionId`。
5. 调用 `POST /api/sessions/{sessionId}/tasks` 创建任务。
6. 观察 Agent UI 日志区，应显示收到任务、payload 摘要、任务开始、模拟处理、任务结束。
7. 调用 `GET /api/tasks/{taskId}` 查询任务状态，应看到模拟结果。
8. 调用 `GET /api/tasks/{taskId}/logs` 查询任务日志，应看到 relay-server 和 Agent 上报的任务日志。

本轮未自动启动 JavaFX GUI 做人工点击验证；已完成 Maven 编译构建检查。

### 当前问题

- 任务、日志和在线设备仍使用内存存储，服务重启后会丢失。
- 本阶段没有实现任务超时扫描器，只保留 `timeoutSeconds` 和 `expiresAt` 协议字段，后续阶段可补充调度处理。
- 本阶段没有真实执行任何命令、文件读取、文件写入或补丁应用，这是 Phase 03 的预期安全边界。

### 下一阶段计划

下一阶段如果任务文件要求进入真实执行能力，必须继续保持 Agent 可见、用户授权目录限制、危险命令拦截、任务超时、修改前备份和审计日志。不得绕过授权目录，不得隐藏运行，不得读取授权目录外文件。

## Phase 04：授权目录选择与路径沙箱

### 本次任务目标

根据 `tasks/phase-04-path-sandbox.md`，只完成 Agent 端授权目录选择与路径沙箱、Relay Server 记录授权目录、任务下发携带授权目录。不实现 Phase 05 及之后的真实文件读取、写入或命令执行。

### 实际完成内容

- 新增 Agent 端 `AuthorizedDirectoryChooser`，封装 JavaFX `DirectoryChooser`，用户取消选择时不产生授权目录。
- 新增 Agent 端 `PathSandbox`：
  - `isUnderAuthorizedDir(Path target)` 校验目标路径是否在授权目录内。
  - `resolveSecurely(String relativePath)` 只接受相对路径，并拒绝 `../` 路径穿越。
  - `normalize(String path)` 提供路径标准化。
- `AgentClientApplication` 改为通过 `AuthorizedDirectoryChooser` 选择授权目录，并在选择后初始化 `PathSandbox`。
- Agent 未选择授权目录或路径沙箱未初始化时禁止连接。
- `AgentConnectionClient` 继续在 hello 消息中发送 `authorizedDirectory`，并在收到任务时记录服务端下发的授权目录。
- `AgentConnectionController` 对缺失 `authorizedDirectory` 的 hello 消息返回错误并拒绝注册。
- `DeviceConnection` 已包含 `authorizedDirectory` 字段，本阶段保持该字段并确认在线设备 API 可暴露。
- `DeviceRegistry` 新增 `getAuthorizedDirectory(deviceId)`。
- `RemoteTask` 新增 `authorizedDirectory` 字段，`TaskService` 下发任务时从在线 Agent 连接记录中带上授权目录。
- 新增 `PathSandboxTest`，实际验证：
  - `src/main.java` 正常相对路径允许。
  - `../../../etc/passwd` 路径穿越被拒绝。

### 修改/新增文件

- `agent-client/pom.xml`
- `agent-client/src/main/java/com/airh/agent/safety/PathSandbox.java`
- `agent-client/src/main/java/com/airh/agent/ui/AuthorizedDirectoryChooser.java`
- `agent-client/src/main/java/com/airh/agent/ui/AgentClientApplication.java`
- `agent-client/src/main/java/com/airh/agent/connection/AgentConnectionClient.java`
- `agent-client/src/test/java/com/airh/agent/safety/PathSandboxTest.java`
- `common-protocol/src/main/java/com/airh/protocol/dto/RemoteTask.java`
- `relay-server/src/main/java/com/airh/relay/device/DeviceRegistry.java`
- `relay-server/src/main/java/com/airh/relay/task/TaskService.java`
- `relay-server/src/main/java/com/airh/relay/websocket/AgentConnectionController.java`
- `DEVELOPMENT_REPORT.md`
- `复现记录.md`

### 使用过的关键命令

按用户要求已执行：

```powershell
cd E:\openclaw-project\ai-remote-helper
mvn clean package
```

结果：失败，当前 PowerShell PATH 中没有 `mvn`。

随后使用项目局部 Maven 执行同等构建：

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package
```

结果：`BUILD SUCCESS`。

查看版本信息：

```powershell
java -version
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -version
```

### 测试结果

- `PathSandboxTest` 已随 `.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package` 实际运行。
- 测试结果：`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- 全量 Maven Reactor 构建结果：`BUILD SUCCESS`。
- 本轮未自动启动 JavaFX GUI 做人工点击验证；GUI 启动和接口验收命令见 `复现记录.md`。

### 环境信息

- 操作系统：Windows 11
- Java：OpenJDK 21.0.9 Temurin
- Maven：项目局部 Apache Maven 3.9.9
- Spring Boot：3.3.5
- JavaFX：21.0.5

### 当前问题

- 系统 PATH 中仍未找到 `mvn`，直接执行 `mvn clean package` 失败；实际构建使用项目内 `.tools\apache-maven-3.9.9\bin\mvn.cmd` 完成。
- 本阶段只完成路径沙箱和授权目录协议传递，真实文件读取、写入、命令执行仍未实现。
- 本轮没有自动启动 relay-server 和 agent-client 做端到端 GUI 验证，避免后台打开不可控窗口；已通过单元测试和全量构建验证核心代码。

### 下一阶段计划

下一阶段应严格按 `tasks/phase-05-readonly-operations.md` 执行。如果开始实现真实只读文件能力，必须统一使用 `PathSandbox` 校验路径，所有读取限制在用户手动选择的授权目录内，并继续保持 Agent UI 可见和操作日志可审计。

## Phase 05：list_dir / read_file 只读操作

### 本次任务目标

根据 `tasks/phase-05-readonly-operations.md`，实现 Agent 端 `LIST_DIR` 和 `READ_FILE` 的真实只读执行能力，所有文件路径必须经过 `PathSandbox` 校验，并通过 STOMP `/app/agent/task-result` 返回真实结果。

### 实际完成内容

- 新增 `FileSystemService`：
  - `listDirectory(String relativePath)` 返回真实目录项列表，包含 `name`、`size`、`modifiedTime`、`directory`。
  - `readFile(String relativePath)` 返回真实 UTF-8 文本文件内容。
  - 文件读取最大 1MB，超过限制时只返回文件信息和说明，不返回正文。
  - 检测到二进制文件时只返回文件信息和说明，不返回正文。
  - 所有路径统一通过 `PathSandbox.resolveSecurely` 校验。
- 新增 `TaskExecutor`：
  - 根据 `TaskType` 分发 `LIST_DIR` / `READ_FILE`。
  - 使用后台线程池异步执行，不阻塞 JavaFX UI 线程。
  - 成功和失败都包装为 `TaskResultMessage`。
  - 不属于 Phase 05 范围的任务类型返回失败结果，不做模拟执行。
- 更新 `AgentConnectionClient`：
  - 连接时基于用户授权目录创建 `PathSandbox`、`FileSystemService` 和 `TaskExecutor`。
  - 收到任务后改为异步真实执行，并把结果发送到 `/app/agent/task-result`。
  - 任务日志仍通过 `/app/agent/task-log` 上报。
- 更新 Agent UI：
  - 将“本阶段只返回模拟结果”改为真实只读执行提示。
  - 新增可折叠“任务结果（文件列表 / 文件内容）”区域，用于展示目录列表或文件读取结果。
- 更新 Relay Server：
  - `TaskService.receiveTaskResult` 继续存储真实 `summary/output/stderr/errorMessage`。
  - 日志文案从“模拟任务结果”改为“真实任务结果”。
- 新增 `FileSystemServiceTest`，覆盖目录列表、文本读取、路径越界拒绝、二进制文件跳过内容。

### 修改/新增文件

- `agent-client/pom.xml`
- `agent-client/src/main/java/com/airh/agent/filesystem/FileSystemService.java`
- `agent-client/src/main/java/com/airh/agent/executor/TaskExecutor.java`
- `agent-client/src/main/java/com/airh/agent/connection/AgentConnectionClient.java`
- `agent-client/src/main/java/com/airh/agent/connection/AgentConnectionListener.java`
- `agent-client/src/main/java/com/airh/agent/ui/AgentClientApplication.java`
- `agent-client/src/test/java/com/airh/agent/filesystem/FileSystemServiceTest.java`
- `relay-server/src/main/java/com/airh/relay/task/TaskService.java`
- `DEVELOPMENT_REPORT.md`
- `复现记录.md`

### 使用过的关键命令

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package
```

结果：`BUILD SUCCESS`。

```powershell
java -version
```

结果：OpenJDK 21.0.9 Temurin。

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -version
```

结果：Apache Maven 3.9.9，Java 21.0.9，Windows 11。

### 测试结果

- 已执行全量 Maven Reactor 构建：`BUILD SUCCESS`。
- `FileSystemServiceTest`：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `PathSandboxTest`：`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- Agent 端总测试：`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- 本轮未自动启动 JavaFX GUI 做端到端人工验证；启动和手工验证命令见 `复现记录.md`。

### 环境信息

- 操作系统：Windows 11
- Java：OpenJDK 21.0.9 Temurin
- Maven：项目局部 Apache Maven 3.9.9
- Spring Boot：3.3.5
- JavaFX：21.0.5

### 当前问题

- `READ_FILE` 当前按 UTF-8 文本读取；非 UTF-8 内容会按不可安全返回的文件处理，只返回文件信息，不返回正文。
- 大于 1MB 的文件当前不读取正文，只返回文件信息和限制说明。
- 本阶段未实现写文件、命令执行、补丁应用或报告生成，这些仍留给后续 phase。
- 未做 GUI 端到端人工验证，但核心只读能力已通过单元测试和全量构建验证。

### 下一步建议

- 按 Phase 06 再实现写文件和补丁能力，必须先做修改前备份。
- 后续 MCP Bridge 接入时，统一使用 `payload.data.path` 传递相对路径。
- 继续保持所有 Agent 端操作可见、可断开、可审计。

## Phase 06：write_file / apply_patch 写入操作

### 本次任务目标

根据 `tasks/phase-06-write-operations.md`，实现 Agent 端 `WRITE_FILE` 和 `APPLY_PATCH` 的真实写入能力。所有写入必须限制在授权目录内，并在修改已有文件前自动创建备份，备份最多保留最近 3 个版本。

### 实际完成内容

- 新增 `BackupService`：
  - 修改已有文件前备份到 `.ai-remote-helper/backups/{timestamp}/{relativePath}`。
  - 备份保留原相对目录结构。
  - 支持按文件列出备份。
  - 支持清理旧备份，每个文件只保留最近 3 版。
- 扩展 `FileSystemService`：
  - 新增 `writeFile(String relativePath, String content)`。
  - 新增 `applyPatch(String relativePath, String patch)`。
  - 写入和补丁路径都通过 `PathSandbox.resolveSecurely` 校验。
  - 写入已有文件和应用补丁前都会调用 `BackupService.backupFile`。
  - 写入后返回 `WriteFileResult`，包含文件路径、大小、备份路径和 diff 摘要。
  - `applyPatch` 支持常见 unified diff hunk 格式。
- 更新 `TaskExecutor`：
  - 增加 `WRITE_FILE` 分支，读取 payload 中的 `content` 或 `text`。
  - 增加 `APPLY_PATCH` 分支，读取 payload 中的 `patch` 或 `diff`。
  - 结果通过已有 `TaskResultMessage` 返回给 Relay Server。
- 扩展 `FileSystemServiceTest`：
  - 验证 `writeFile` 可以在授权目录内创建新文件。
  - 验证修改已有文件会创建备份并只保留最近 3 个版本。
  - 验证写入授权目录外路径会被拒绝。
  - 验证 `applyPatch` 可以修改文件并创建备份。

### 修改/新增文件

- `agent-client/src/main/java/com/airh/agent/filesystem/BackupService.java`
- `agent-client/src/main/java/com/airh/agent/filesystem/FileSystemService.java`
- `agent-client/src/main/java/com/airh/agent/executor/TaskExecutor.java`
- `agent-client/src/test/java/com/airh/agent/filesystem/FileSystemServiceTest.java`
- `DEVELOPMENT_REPORT.md`
- `复现记录.md`

### 使用过的关键命令

查看 Java 版本：

```powershell
cd E:\openclaw-project\ai-remote-helper
java -version
```

查看项目局部 Maven 版本：

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -version
```

按要求执行全量构建和测试：

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package
```

结果：`BUILD SUCCESS`。

### 测试结果

- 已执行全量 Maven Reactor 构建：`BUILD SUCCESS`。
- `FileSystemServiceTest`：`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。
- `PathSandboxTest`：`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- 全部测试：`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。
- 本轮未自动启动 JavaFX GUI 做端到端人工验证；启动和手工验证命令见 `复现记录.md`。

### 环境信息

- 操作系统：Windows 11
- Java：OpenJDK 21.0.9 Temurin
- Maven：项目局部 Apache Maven 3.9.9
- Spring Boot：3.3.5
- JavaFX：21.0.5

### 当前问题

- `applyPatch` 当前实现的是基础 unified diff hunk 解析，不是完整 `git apply` 兼容实现。
- 写入内容按 UTF-8 处理；当前阶段不支持二进制写入。
- 本阶段未实现 Agent UI diff 预览专区，只通过任务日志和任务结果返回 diff 摘要。
- 本阶段未实现命令执行、报告生成、数据库持久化或 MCP Bridge 写入工具。

### 下一步建议

- 后续 Phase 07 如实现命令执行，必须继续限制 cwd 在授权目录内，并接入危险命令检测和超时控制。
- 如果后续需要更完整补丁能力，可以引入成熟 diff/patch 库替代当前基础解析器。
- Relay 和 MCP 侧创建 `WRITE_FILE` 任务时建议使用 `payload.data.path` 和 `payload.data.content`；创建 `APPLY_PATCH` 任务时建议使用 `payload.data.path` 和 `payload.data.patch`。

## Phase 07：run_command 命令执行

### 本次任务目标

根据 `tasks/phase-07-command-execution.md`，在 Agent 端实现 `RUN_COMMAND` 任务：使用 `ProcessBuilder` 通过系统 shell 执行命令，分别捕获 stdout/stderr，支持授权目录内工作目录、默认 30 秒超时、最大 300 秒超时、进程强杀和实时输出日志。

### 实际完成内容

- 新增 `CommandExecutionService`：
  - 使用 `ProcessBuilder` 执行命令。
  - Windows 使用 `cmd.exe /c`，非 Windows 使用 `sh -c`。
  - 默认工作目录为 Agent 授权目录。
  - 支持相对工作目录，也允许授权目录内的绝对工作目录；越界目录会拒绝执行。
  - 默认超时 30 秒，最大超时 300 秒。
  - stdout 和 stderr 分开读取、分开累计。
  - 支持实时输出 callback。
  - 超时后调用 `kill()` 强杀进程及其子进程。
  - 关闭 stdin，不支持交互式命令。
- 新增 `CommandResult` record，字段为 `exitCode`、`stdout`、`stderr`、`durationMs`、`timedOut`、`killed`。
- 更新 `TaskExecutor`：
  - 增加 `RUN_COMMAND` 分支。
  - 从 payload 或 `payload.data` 中读取 `command`、`workingDir/cwd`、`timeoutSeconds/timeout`。
  - 执行期间把 stdout/stderr chunk 作为任务日志上报，前缀为 `[stdout]` 和 `[stderr]`。
  - 将 `CommandResult` 序列化为 JSON 作为任务 output 返回。
  - 非 0 exit code、超时或被 kill 时返回 `FAILED`；正常退出返回 `SUCCESS`。
  - 接入 `CommandRiskDetector`，命中 `BLOCKED` 风险时拒绝执行。
- 更新 `AgentConnectionClient`：
  - 连接时基于同一个 `PathSandbox` 初始化 `FileSystemService` 和 `CommandExecutionService`。
- 新增 `CommandExecutionServiceTest`：
  - 验证 echo 命令成功并流式返回 stdout。
  - 验证超时行为和 kill 标记。
  - 验证命令工作目录。
  - 验证非 0 exit code 捕获。

### 修改/新增文件

- `agent-client/src/main/java/com/airh/agent/executor/CommandExecutionService.java`
- `agent-client/src/main/java/com/airh/agent/executor/CommandResult.java`
- `agent-client/src/main/java/com/airh/agent/executor/TaskExecutor.java`
- `agent-client/src/main/java/com/airh/agent/connection/AgentConnectionClient.java`
- `agent-client/src/test/java/com/airh/agent/executor/CommandExecutionServiceTest.java`
- `DEVELOPMENT_REPORT.md`
- `复现记录.md`

### 使用过的关键命令

查看 Java 版本：

```powershell
cd E:\openclaw-project\ai-remote-helper
java -version
```

结果：OpenJDK 21.0.9 Temurin。

查看项目局部 Maven 版本：

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -version
```

结果：Apache Maven 3.9.9，Java 21.0.9，Windows 11。

按要求执行全量构建和测试：

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package
```

结果：`BUILD SUCCESS`。

### 测试结果

- 已执行全量 Maven Reactor 构建：`BUILD SUCCESS`。
- `CommandExecutionServiceTest`：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `FileSystemServiceTest`：`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。
- `PathSandboxTest`：`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- 全部测试：`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`。
- 本轮未自动启动 JavaFX GUI 做端到端人工验证；启动和手工验证命令见 `复现记录.md`。

### 环境信息

- 操作系统：Windows 11
- Java：OpenJDK 21.0.9 Temurin
- Maven：项目局部 Apache Maven 3.9.9
- Spring Boot：3.3.5
- JavaFX：21.0.5

### 当前问题

- `CommandExecutionService.kill()` 面向当前运行进程；当前任务执行器允许并发任务时，不建议同时下发多个长时间命令任务。
- 命令输出按当前 JVM 默认字符集读取；跨平台中文命令输出可能受系统 shell 编码影响。
- 本阶段已通过 STOMP `/app/agent/task-log` 上报命令输出日志，但未实现 Relay 端分页存储命令日志和 JavaFX 独立追加输出视图增强。

### 下一步建议

- 后续 Relay Server 端应补齐任务日志持久化和分页查询。
- 后续 Agent UI 可增加专门的命令输出追加区域，区分 stdout/stderr 样式。
- MCP Bridge 创建 `RUN_COMMAND` 任务时建议使用 `payload.data.command`、`payload.data.cwd` 和 `payload.data.timeoutSeconds`。

## Phase 08：危险命令拦截 + 敏感文件保护

### 本次任务目标

根据 `tasks/phase-08-safety-interception.md` 和本轮用户指定范围，增强 `common-safety` 的危险命令检测规则，新增敏感文件路径保护器，并在 Agent 端文件读写入口接入敏感路径检查。不实现本阶段未要求的 `safety-rules.json`、Agent UI 拦截展示、Relay 审计 DTO 或规则按任务类型配置。

### 实际完成内容

- 增强 `CommandRiskDetector`：
  - `BLOCKED`：拦截 `rm -rf /`、`format`、`del /f /s`、`reg delete`、`shutdown`、`mkfs`、`dd if=... of=/dev/...`、重定向到 `/dev/sda` 等磁盘设备、`curl|sh`、`wget|sh`。
  - `HIGH`：识别 `sudo`、`su -`、`chmod 777`、`chown`、非根目录 `rm -rf`、`netsh`、`sc stop/delete`、`taskkill /f`。
  - `MEDIUM`：识别普通 `rm`、`del`、`kill`、`service ... stop`。
- 新增 `SensitiveFileProtector`：
  - `BLOCKED`：`~/.ssh/`、`~/.gnupg/`、Chrome/Firefox/Edge 浏览器 profile、`/etc/passwd`、`/etc/shadow`、`/etc/sudoers`、Windows `System32\config\SAM/SECURITY/SYSTEM` 等系统凭据路径。
  - `HIGH`：`~/.bashrc`、`~/.zshrc`、`~/.profile`、Windows Startup 启动目录、hosts 文件。
  - 提供 `checkPath(String path)` 和 `checkPath(Path path)`，返回 `RiskLevel`。
- 更新 `SensitiveFileGuard`，内部复用 `SensitiveFileProtector`，保持旧接口可用。
- 在 `FileSystemService` 中接入 `SensitiveFileProtector`：
  - `readFile`、`writeFile`、`applyPatch` 在路径沙箱解析后检查敏感路径。
  - `BLOCKED` 抛出 `SecurityException` 拒绝访问。
  - `HIGH` 使用 Java logger 记录警告，但继续执行。
- 为 `common-safety` 增加 JUnit 5 测试依赖和 Surefire 插件。
- 新增单元测试：
  - `CommandRiskDetectorTest`
  - `SensitiveFileProtectorTest`
  - 扩展 `FileSystemServiceTest`，覆盖授权目录内 `.ssh` / `.gnupg` 敏感路径拦截。

### 修改/新增文件

- `common-safety/pom.xml`
- `common-safety/src/main/java/com/airh/safety/CommandRiskDetector.java`
- `common-safety/src/main/java/com/airh/safety/SensitiveFileGuard.java`
- `common-safety/src/main/java/com/airh/safety/SensitiveFileProtector.java`
- `common-safety/src/test/java/com/airh/safety/CommandRiskDetectorTest.java`
- `common-safety/src/test/java/com/airh/safety/SensitiveFileProtectorTest.java`
- `agent-client/src/main/java/com/airh/agent/filesystem/FileSystemService.java`
- `agent-client/src/test/java/com/airh/agent/filesystem/FileSystemServiceTest.java`
- `DEVELOPMENT_REPORT.md`
- `复现记录.md`

### 使用过的关键命令

读取任务说明：

```powershell
cd E:\openclaw-project\ai-remote-helper
Get-Content -Raw tasks\phase-08-safety-interception.md
```

查看项目文件和现有实现：

```powershell
cd E:\openclaw-project\ai-remote-helper
rg --files
rg "CommandRiskDetector|SensitiveFileGuard|RiskLevel" -n
Get-Content -Raw common-safety\src\main\java\com\airh\safety\CommandRiskDetector.java
Get-Content -Raw agent-client\src\main\java\com\airh\agent\filesystem\FileSystemService.java
```

查看环境版本：

```powershell
cd E:\openclaw-project\ai-remote-helper
java -version
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -version
```

执行全量构建和测试：

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package
```

查看修改状态：

```powershell
cd E:\openclaw-project\ai-remote-helper
git status --short
```

### 构建和测试结果

- 已执行 `.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package`。
- 结果：`BUILD SUCCESS`。
- `common-safety` 测试：`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- `agent-client` 测试：`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`。
- 全量实际测试合计：`Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`。

### 环境信息

- 操作系统：Windows 11 10.0 amd64
- Java：OpenJDK 21.0.9 Temurin
- Maven：项目局部 Apache Maven 3.9.9
- Maven 路径：`.tools\apache-maven-3.9.9\bin\mvn.cmd`
- Spring Boot：3.3.5
- JavaFX：21.0.5

### 当前问题

- 本轮只实现用户指定的核心拦截能力，没有实现 `phase-08` 文档中列出的可配置规则文件 `safety-rules.json`、Agent UI 拦截展示和 Relay 审计事件持久化。
- `HIGH` 敏感路径目前仅通过 Agent 本地 logger 记录警告并继续执行，还没有统一进入 Relay 审计日志。
- 命令检测仍是黑名单/正则规则，不是完整 shell AST 解析；后续如要覆盖复杂转义、变量拼接和多层 shell，需要引入更强的解析或执行前策略。

### 下一步建议

- 后续继续 Phase 08 时，可新增 `SafetyChecker` 统一返回 `ALLOW / WARN / DENY` 和原因。
- 增加 `safety-rules.json` 加载能力，让命令和路径规则可配置。
- 将 `BLOCKED` 和 `HIGH` 事件统一上报 Relay Server 审计日志，并在 Agent UI 中实时显示拦截原因。

## Phase 09：数据库集成（PostgreSQL + Redis）

### 本次任务目标

根据 `tasks/phase-09-database.md` 和用户指定范围，为 `relay-server` 接入 PostgreSQL 持久化任务记录和审计事件，并接入 Redis 缓存在线会话状态。

### 实际完成内容

- 在 `relay-server/pom.xml` 中新增 `spring-boot-starter-data-jpa`、`postgresql` 驱动和 `spring-boot-starter-data-redis`。
- 在 `application.yml` 配置 PostgreSQL：`localhost:15432/testdb`，用户名/密码为 `postgres/postgres`。
- 在 `application.yml` 配置 Redis：`localhost:16379`。
- 新增 JPA 实体：
  - `TaskRecordEntity` 映射 `task_records`。
  - `AuditEventEntity` 映射 `audit_events`。
- 新增 Repository：
  - `TaskRecordRepository`
  - `AuditEventRepository`
- 新增 `DatabaseConfig` 启用 JPA auditing。
- 更新 `TaskService`：
  - 创建任务时写入 `task_records`。
  - 下发任务时更新任务状态为 `RUNNING`。
  - 收到 Agent 结果时更新任务状态、输出、错误和完成时间。
  - `GET /api/tasks/{taskId}` 优先从数据库读取。
- 新增 `AuditService`，记录 `TASK_CREATED`、`TASK_DISPATCHED`、`TASK_RESULT_RECEIVED` 审计事件。
- 新增 `RedisConfig` 和 `SessionStateCache`，用 Redis 保存在线/离线 session 状态，TTL 为 5 分钟。
- 更新 `DeviceRegistry`，在注册、心跳、断开时刷新 Redis session 缓存。
- 新增 `relay-server/src/main/resources/db/migration/V1__init.sql`，记录 PostgreSQL 建表脚本。

### 修改/新增文件

- `relay-server/pom.xml`
- `relay-server/src/main/resources/application.yml`
- `relay-server/src/main/java/com/airh/relay/config/DatabaseConfig.java`
- `relay-server/src/main/java/com/airh/relay/config/RedisConfig.java`
- `relay-server/src/main/java/com/airh/relay/domain/TaskRecordEntity.java`
- `relay-server/src/main/java/com/airh/relay/domain/AuditEventEntity.java`
- `relay-server/src/main/java/com/airh/relay/repository/TaskRecordRepository.java`
- `relay-server/src/main/java/com/airh/relay/repository/AuditEventRepository.java`
- `relay-server/src/main/java/com/airh/relay/service/AuditService.java`
- `relay-server/src/main/java/com/airh/relay/session/SessionStateCache.java`
- `relay-server/src/main/java/com/airh/relay/device/DeviceRegistry.java`
- `relay-server/src/main/java/com/airh/relay/task/TaskRecord.java`
- `relay-server/src/main/java/com/airh/relay/task/TaskService.java`
- `relay-server/src/main/resources/db/migration/V1__init.sql`
- `relay-server/src/test/java/com/airh/relay/repository/TaskRecordRepositoryTest.java`
- `relay-server/src/test/java/com/airh/relay/repository/AuditEventRepositoryTest.java`
- `DEVELOPMENT_REPORT.md`
- `复现记录.md`

### 使用过的关键命令

读取任务说明：

```powershell
cd E:\openclaw-project\ai-remote-helper
Get-Content -LiteralPath tasks/phase-09-database.md
```

查看环境版本：

```powershell
cd E:\openclaw-project\ai-remote-helper
java -version
.\.tools\apache-maven-3.9.9\bin\mvn.cmd -version
```

执行全量构建和测试：

```powershell
cd E:\openclaw-project\ai-remote-helper
.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package
```

查看修改状态：

```powershell
cd E:\openclaw-project\ai-remote-helper
git status --short
```

### 构建和测试结果

- 已执行 `.\.tools\apache-maven-3.9.9\bin\mvn.cmd clean package`。
- 结果：`BUILD SUCCESS`。
- `common-safety` 测试：`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- `relay-server` 测试：`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- `agent-client` 测试：`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`。
- 全量实际测试合计：`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`。

### 环境信息

- 操作系统：Windows 11 10.0 amd64
- Java：OpenJDK 21.0.9 Temurin
- Maven：项目局部 Apache Maven 3.9.9
- Maven 路径：`.tools\apache-maven-3.9.9\bin\mvn.cmd`
- Spring Boot：3.3.5
- PostgreSQL 连接配置：`jdbc:postgresql://localhost:15432/testdb`
- Redis 连接配置：`localhost:16379`

### 当前问题

- 本轮按要求完成构建验证，但未启动本地 PostgreSQL/Redis 做真实接口联调。
- `V1__init.sql` 已创建为建表脚本；当前项目未引入 Flyway，运行时实际建表依赖 `spring.jpa.hibernate.ddl-auto=update`。
- 任务日志 `TaskLog` 仍保持内存存储，本阶段只持久化任务记录和审计事件。
- `SessionStateCache` 将 Redis 作为缓存层处理；Redis 不可用时不会阻断现有内存在线设备注册逻辑。
- 第一次补充 Repository 测试后执行全量构建时，relay-server 测试已通过，但 Windows 文件锁导致 Maven clean 删除 `agent-client\target\classes\com\airh` 失败；随后重跑同一条 `clean package` 成功。

### 下一步建议

- 如果后续要严格执行 SQL 迁移脚本，应引入 Flyway 或 Liquibase，并关闭生产环境中的 `ddl-auto=update`。
- 为 `TaskRecordRepository` 和 `AuditEventRepository` 增加 Testcontainers 或专用测试库集成测试，避免测试依赖开发机已有数据库状态。
- 后续可将任务日志也持久化，支持分页查询和审计追踪。
