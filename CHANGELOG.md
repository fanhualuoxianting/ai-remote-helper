# Changelog

All notable changes to AI Remote Helper will be documented in this file.

## 0.1.4 - LAN stability hotfix

- Added relay-side timeout sweeping so stale `PENDING` / `RUNNING` tasks become `TIMEOUT` instead of hanging forever.
- Added assisted Agent Client auto-reconnect after unexpected WebSocket transport disconnects.
- Added helper-side offline detection and a `一键链路自检` button for quick `LIST_DIR .` path checks.
- Improved Chinese Windows command output decoding, with `AIRH_COMMAND_OUTPUT_CHARSET` / `airh.commandOutputCharset` override support.
- Updated relay health version reporting, in-app version labels, and Windows packaging defaults to `0.1.4`.

## 0.1.3 - Agent task result hotfix

- Fixed the agent-side STOMP JSON converter so `Instant` fields in task logs and task results can be serialized correctly.
- Added defensive fallback handling around task log and task result reporting to avoid silent execution-chain aborts.
- Updated relay health version reporting, in-app version labels, and Windows packaging defaults to `0.1.3`.
- Rebuilt the Windows LAN app-image package for redistribution.

## 0.1.2 - OpenClaw runner and AI task bridge polish

- Added a helper-side AI Runner selector so the desktop client can launch either `Codex` or `OpenClaw`.
- Kept the local JSON task queue bridge as the preferred execution path, so child-shell WinSock issues no longer block remote help.
- Updated in-app version labels and LAN packaging default app version to `0.1.2`.
- Rebuilt the Windows LAN app-image package for Release distribution.

## 0.1.1 - Help request workflow and Windows icon

- Added assisted-user help request submission and helper-side approval workflow.
- Added visible Codex runner launch after helper approval.
- Added helper-side direct AI assist prompt entry.
- Replaced the Windows app icon and wired `jpackage --icon` so Explorer no longer shows the default Java icon.
- Updated in-app version labels and LAN packaging default app version to `0.1.1`.
- Rebuilt the Windows LAN app-image package for Release distribution.

## 0.1.0 - MVP

- Added Maven multi-module project structure.
- Added shared protocol DTOs and safety utilities.
- Added Spring Boot relay server with WebSocket task routing.
- Added JavaFX Agent Client with visible authorization, LAN connection, task logs, and disconnect flow.
- Added MCP Bridge for AI tool integration.
- Added file operations, command execution, task logs, report generation, and safety interception.
- Added LAN-oriented Windows app-image packaging scripts.
- Added public security documentation and GitHub-ready repository cleanup.
