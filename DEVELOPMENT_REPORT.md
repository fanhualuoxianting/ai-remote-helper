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
