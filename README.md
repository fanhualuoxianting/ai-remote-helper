# AI Remote Helper

[中文说明](README.zh-CN.md) | [Download latest Windows LAN release](https://github.com/fanhualuoxianting/ai-remote-helper/releases/latest)

<p align="center">
  <img src="docs/assets/airh-demo.png" width="880" alt="AI Remote Helper desktop demo" />
</p>

AI Remote Helper is a visible, authorized remote development assistance platform for AI coding workflows. It lets a helper or AI tool operate only inside a directory that the assisted user explicitly selects, while the assisted user keeps a desktop client open and can see logs, results, and disconnect controls.

This project is designed for legitimate pair-programming and troubleshooting scenarios. It is not a hidden remote-control tool.

## What It Does

- Provides a JavaFX desktop Agent Client for the assisted user.
- Uses a Spring Boot Relay Server to manage sessions, task routing, logs, and audit records.
- Exposes an MCP Bridge so tools such as OpenClaw, Codex, or Claude-compatible clients can request authorized file and command operations.
- Restricts file and command operations to the user-selected workspace.
- Blocks sensitive files, path traversal, and dangerous commands.
- Generates task logs, file-change records, and session reports.
- Lets the assisted user submit natural-language help requests that the helper reviews before launching a local AI session.
- Supports a LAN packaging flow for Windows app-image distribution.

## Current Status

This repository is a public MVP / portfolio-ready version. The core safety model, relay flow, JavaFX client, MCP bridge, LAN packaging scripts, and documentation are present. It is suitable for local development, demonstrations, and further hardening.

Recommended next steps before real-world use:

- Add authentication and authorization hardening for shared or public networks.
- Replace development database credentials with environment-specific configuration.
- Add more integration tests around WebSocket task routing.
- Publish signed release artifacts through GitHub Releases rather than committing build outputs.

## Architecture

<p align="center">
  <img src="docs/assets/architecture.png" width="880" alt="AI Remote Helper architecture" />
</p>

```text
AI Tool / MCP Client
        |
        v
   MCP Bridge
        |
        v
   Relay Server  <---->  PostgreSQL / Redis
        |
        v
   Agent Client
        |
        v
Authorized Project Directory
```

Modules:

| Module | Purpose |
| --- | --- |
| `common-protocol` | Shared DTOs, message types, task states, and enums. |
| `common-safety` | Path sandboxing, sensitive-file protection, and command risk detection. |
| `relay-server` | Spring Boot relay for devices, sessions, tasks, logs, audit events, and WebSocket routing. |
| `agent-client` | JavaFX desktop client for directory authorization, connection, task execution, and visible logs. |
| `mcp-bridge` | MCP-compatible bridge that forwards AI tool requests to the relay. |
| `web-console` | Vue-based web console prototype for monitoring and administration. |

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for more detail.

## Desktop Workflow

| Assisted Agent Client | Helper Review Workbench |
| --- | --- |
| <img src="docs/assets/agent-client.png" width="420" alt="Assisted user agent client" /> | <img src="docs/assets/helper-review.png" width="420" alt="Helper review workbench" /> |

## Safety Boundaries

AI Remote Helper intentionally does not implement stealth or persistence behavior.

It does not:

- Hide the Agent Client window.
- Start automatically on boot.
- Install a system service.
- Request administrator privileges by default.
- Bypass the authorized directory.
- Take over mouse or keyboard input.
- Disable firewalls or security software.
- Read browser data, SSH private keys, or system credentials.

See [SECURITY.md](SECURITY.md) for the full security model.

## 3-Minute Local Demo

```powershell
git clone https://github.com/fanhualuoxianting/ai-remote-helper.git
cd ai-remote-helper

docker compose up -d
mvn clean package
scripts\start-relay-lan.bat
mvn -f agent-client/pom.xml javafx:run
```

Demo scenario:

1. Assisted user selects a workspace.
2. Helper connects using the session code.
3. Assisted user submits a natural-language request.
4. Helper reviews and approves the request.
5. Codex or OpenClaw writes a JSON task into the local AI queue.
6. Agent executes file or command operations inside the authorized directory.
7. Logs, task results, and report output are returned visibly.

## Requirements

- Java 21+
- Maven 3.9+
- PostgreSQL for relay persistence
- Redis for online session/cache state
- Node.js 18+ if you want to run the Web Console
- Windows 10/11 with `jpackage` for LAN app-image packaging

## Quick Start

Build all Java modules:

```powershell
mvn clean package
```

Run the Relay Server:

```powershell
mvn -f relay-server/pom.xml spring-boot:run
```

For Windows LAN demos, the helper can use the wrapper script below. It starts local Docker dependencies when available and runs the Relay Server on port `18080`, avoiding common conflicts with local Tomcat on `8080`:

```powershell
scripts\start-relay-lan.bat
```

Run the Agent Client:

```powershell
mvn -f agent-client/pom.xml javafx:run
```

Run the MCP Bridge:

```powershell
mvn -f mcp-bridge/pom.xml spring-boot:run
```

Run the Web Console:

```powershell
cd web-console
npm install
npm run dev
```

Default local development endpoints:

- Relay Server: `http://localhost:8080`
- LAN Relay helper script: `http://localhost:18080`
- MCP Bridge SSE: `http://localhost:9090/mcp/sse`
- Web Console: `http://localhost:3000`

## Local Development Configuration

The default relay configuration is intended for local development:

- PostgreSQL: `localhost:15432`
- Database: `testdb`
- Username/password: `postgres/postgres`
- Redis: `localhost:16379`

Do not reuse these development credentials in production or on a shared network. Override them with environment-specific configuration before deployment.

## Basic Usage

Assisted user:

1. Start the Agent Client.
2. Choose `我需要别人帮忙`.
3. Select a project directory as the authorized workspace.
4. Enter the helper machine's LAN IP and relay port.
5. Test the connection, connect, and share the generated connection code.
6. Optionally open `提交需求` and describe what you want the helper's AI to do.
7. Watch all operations in the visible Agent Client window and disconnect at any time.

Helper:

1. Start the Relay Server. On Windows LAN demos, prefer `scripts\start-relay-lan.bat`.
2. Start the Agent Client and choose `我要帮别人处理项目`, or connect through the MCP Bridge.
3. Enter the assisted user's connection code.
4. Click `一键链路自检` after connecting to verify directory listing, task dispatch, and result return.
5. Review pending requests in `需求审核`; approving a request opens a visible PowerShell/Codex session on the helper machine.
6. In `AI 协助`, let the AI write JSON task files into its work directory, then click `执行下一条 AI 任务` so the JavaFX client submits them through the relay on the AI's behalf.
7. Browse authorized files, run allowed commands, inspect logs, and generate reports.

LAN details are documented in [docs/LAN_MODE.md](docs/LAN_MODE.md).

## LAN Troubleshooting

If a connection becomes unstable:

- Confirm both machines are on the same LAN and the assisted user is using the helper machine's LAN address, for example `http://192.168.x.x:18080`.
- Confirm the Relay Server is still running with `scripts\start-relay-lan.bat` and that Windows Firewall allows the port.
- Ask the assisted user to reconnect and share the new connection code if the helper UI shows `远程设备离线`.
- Use `一键链路自检` on the helper page. A passing self-test means `LIST_DIR .`, relay dispatch, and result return are working.
- Tasks that do not return before their timeout are now marked `TIMEOUT` by the relay instead of staying `RUNNING` forever.
- The assisted Agent Client automatically retries relay WebSocket reconnection after unexpected disconnects.
- On Chinese Windows, command output defaults to `GBK` decoding to avoid mojibake. Override it when needed:

```powershell
$env:AIRH_COMMAND_OUTPUT_CHARSET='UTF-8'
# or JVM property:
# -Dairh.commandOutputCharset=UTF-8
```

## Windows LAN Packaging

Build a Windows app-image with bundled runtime:

```powershell
agent-client\scripts\package-lan-windows.bat -Offline
```

The script writes output to `dist/AI-Remote-Helper-LAN/`. Build outputs are intentionally ignored by Git. If you want to distribute the app, zip the generated directory and upload it to a GitHub Release.

See [docs/LAN_PACKAGING.md](docs/LAN_PACKAGING.md).

## Startup Easter Egg

The Matrix-style startup overlay is a default-off development easter egg. It only appears when explicitly enabled:

```powershell
$env:AIRH_STARTUP_EASTER_EGG='matrix'
```

or with JVM properties:

```text
-Dairh.startupEasterEgg=matrix
-Dairh.startupEasterEggDuration=5
```

It is rendered inside the application window, can be skipped, and does not create OS-level popups or control user input.

## Approved AI Runner

The helper-side AI runner can now switch between `Codex` and `OpenClaw` from the helper UI. It is only launched after the helper approves a submitted request or clicks the direct AI entry, and it always opens as a visible PowerShell window without dangerous bypass flags.

Optional environment variables:

```powershell
$env:AIRH_AI_RUNNER_COMMAND='codex'
$env:AIRH_AI_RUNNER_COMMAND='openclaw'
$env:AIRH_AI_RUNNER_WORKDIR="$env:USERPROFILE\.ai-remote-helper\ai-runs"
```

The generated prompt now prefers a local task-queue bridge. The AI writes JSON task files into its run directory, and the helper client executes them with its own Java `HttpClient`, which avoids child-shell WinSock issues while still staying inside the assisted user's authorized directory.

## Documentation

- [SECURITY.md](SECURITY.md)
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/LAN_MODE.md](docs/LAN_MODE.md)
- [docs/LAN_PACKAGING.md](docs/LAN_PACKAGING.md)
- [docs/MCP_USAGE.md](docs/MCP_USAGE.md)
- [docs/WEB_CONSOLE.md](docs/WEB_CONSOLE.md)
- [docs/ROADMAP.md](docs/ROADMAP.md)

## License

MIT. See [LICENSE](LICENSE).
