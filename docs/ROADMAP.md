# Roadmap

AI Remote Helper is currently a public MVP focused on Windows LAN-assisted AI coding workflows. The next work should make the existing workflow easier to demo, safer to operate, and more reliable before expanding into public-network scenarios.

## Completed

- Maven multi-module architecture with `agent-client`, `relay-server`, `mcp-bridge`, `common-protocol`, `common-safety`, and `web-console`.
- JavaFX Agent Client for visible directory authorization, connection codes, task logs, help requests, and helper review.
- Spring Boot Relay Server for device sessions, WebSocket task routing, task status, logs, audit records, and health endpoints.
- MCP Bridge module for AI tool integration through relay-mediated remote operations.
- Workspace sandboxing, path traversal protection, sensitive-file protection, and command risk detection.
- PostgreSQL-backed task, audit, file-change, and help-request persistence with Redis-backed online/session cache.
- LAN packaging scripts for Windows app-image distribution and GitHub Release artifacts.
- Versioned releases through `v0.1.5`, including LAN stability fixes, task timeout handling, Agent auto-reconnect, offline status prompts, link self-test, Chinese command output decoding, and JavaFX UI polish.

## In Progress

- Web Console enhancement for online devices, session details, task logs, and audit events.
- Integration tests around WebSocket task routing and relay-to-agent result delivery.
- Authentication and authorization hardening for shared networks.
- Cloud deployment guide for a hardened relay-server setup.
- AI runner ergonomics, including clearer queue visibility and result feedback inside the helper UI.

## Planned

- Signed Windows release packages and published SHA256 checksums.
- Multi-agent task routing and clearer session selection.
- User-side approval workflow for higher-risk operations.
- Public demo video and website landing page embedding.
- Public-network mode after token/session permissions, audit export, and high-risk confirmation are implemented.
