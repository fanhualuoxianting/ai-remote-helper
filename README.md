# AI Remote Helper

[中文说明](README.zh-CN.md) · [Latest Windows LAN release](https://github.com/fanhualuoxianting/ai-remote-helper/releases/latest)

[![CI](https://github.com/fanhualuoxianting/ai-remote-helper/actions/workflows/ci.yml/badge.svg)](https://github.com/fanhualuoxianting/ai-remote-helper/actions/workflows/ci.yml)

AI Remote Helper is a **visible, consent-based remote development assistance platform** for AI coding workflows. It connects a JavaFX desktop agent, a Spring Boot relay service, and an MCP-compatible bridge while constraining file and command operations to a workspace explicitly selected by the assisted user.

> **Project scope**
> This repository is a local/LAN portfolio MVP, not a stealth remote-control product. The current AI integration launches visible Codex or OpenClaw sessions and transfers reviewed JSON tasks through the helper client or MCP bridge. It does **not** claim an embedded OpenAI Function Calling implementation.

<p align="center">
  <img src="docs/assets/airh-demo.png" width="880" alt="AI Remote Helper desktop demo" />
</p>

## What This Project Demonstrates

- **End-to-end task routing:** request review, session creation, relay dispatch, remote execution, result collection, and report generation.
- **Explicit user control:** the assisted user chooses the workspace, keeps the desktop client visible, sees logs, and can disconnect at any time.
- **Workspace sandboxing:** path normalization, traversal protection, sensitive-file blocking, and command risk classification.
- **Auditable execution:** task state, command output, file changes, errors, and audit events are recorded instead of hidden.
- **AI-tool interoperability:** a visible Codex/OpenClaw runner and an MCP-compatible bridge can submit structured operations without bypassing server-side checks.
- **Desktop delivery:** the JavaFX client can be packaged as a Windows app-image for LAN demonstrations.

## Architecture

<p align="center">
  <img src="docs/assets/architecture.png" width="880" alt="AI Remote Helper architecture" />
</p>

```text
Codex / OpenClaw / MCP client
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
   User-authorized workspace
```

| Module | Responsibility |
| --- | --- |
| `common-protocol` | Shared DTOs, task states, message types, and enums. |
| `common-safety` | Path sandboxing, sensitive-file protection, and command risk detection. |
| `relay-server` | Device/session management, task routing, logs, audit events, and WebSocket communication. |
| `agent-client` | Visible JavaFX client for authorization, execution, logs, review, and disconnect controls. |
| `mcp-bridge` | MCP-compatible adapter that forwards approved tool requests to the relay. |
| `web-console` | Vue monitoring-console prototype; it is not required for the desktop demo. |

## Demonstrable Workflow

1. The assisted user starts the Agent Client and selects an authorized project directory.
2. The helper connects with the generated connection code.
3. The assisted user submits a natural-language help request.
4. The helper reviews and approves the request.
5. A visible Codex/OpenClaw session writes a structured task into the local queue, or an MCP client submits an allowed tool request.
6. The helper client sends the task through the relay; the remote agent executes it only inside the authorized workspace.
7. Logs, task results, file-change records, and a session report are returned to the visible UI.

## Quick Start

Requirements: Java 21+, Maven 3.9+, Docker Compose, and Windows 10/11 for JavaFX packaging.

```powershell
git clone https://github.com/fanhualuoxianting/ai-remote-helper.git
cd ai-remote-helper

docker compose up -d
mvn clean package
scripts\start-relay-lan.bat
mvn -f agent-client/pom.xml javafx:run
```

Optional components:

```powershell
# MCP bridge
mvn -f mcp-bridge/pom.xml spring-boot:run

# Vue console prototype
cd web-console
npm install
npm run dev
```

Default development endpoints and LAN troubleshooting are documented in [docs/LAN_MODE.md](docs/LAN_MODE.md).

## Security Boundaries

AI Remote Helper intentionally does not:

- hide the desktop client or run silently;
- install persistence or start automatically at boot;
- request administrator privileges by default;
- take over mouse or keyboard input;
- bypass the authorized workspace;
- read browser data, SSH private keys, or system credentials;
- disable firewalls, security software, or audit logging.

The full model, blocked paths, and command-risk rules are documented in [SECURITY.md](SECURITY.md).

## Verification

The repository includes tests for path sandboxing, sensitive-file protection, command risk detection, task persistence, report generation, help-request handling, and MCP protocol behavior.

```powershell
mvn clean package
```

GitHub Actions builds and tests the Java modules, builds the Vue console, and performs a basic tracked-secret scan.

## Current Limitations

- Authentication and authorization are designed for a trusted local/LAN demo and still require hardening before shared-network or Internet deployment.
- The AI runner is externalized to visible Codex/OpenClaw processes; a direct model API planner is not implemented in this repository.
- The Vue console is a prototype and currently has less automated coverage than the Java modules.
- Release signing, SBOM generation, and automated Windows artifact publishing are not yet part of the delivery pipeline.

Remaining work and acceptance criteria are tracked in [docs/CODEX_HANDOFF.md](docs/CODEX_HANDOFF.md).

## Documentation

- [Security model](SECURITY.md)
- [Architecture](docs/ARCHITECTURE.md)
- [LAN mode](docs/LAN_MODE.md)
- [Windows packaging](docs/LAN_PACKAGING.md)
- [MCP usage](docs/MCP_USAGE.md)
- [Web console](docs/WEB_CONSOLE.md)
- [Roadmap](docs/ROADMAP.md)

## License

MIT. See [LICENSE](LICENSE).
