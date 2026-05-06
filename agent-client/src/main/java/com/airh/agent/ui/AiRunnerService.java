package com.airh.agent.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;

public class AiRunnerService {
    private static final String DEFAULT_RUNNER = "codex";

    public Path launchCodex(String requestId, String relayUrl, String sessionId, String requestContent) throws IOException {
        return launchRequest(
                requestId,
                relayUrl,
                sessionId,
                requestContent,
                "# AI Remote Helper Approved Request",
                "## 被协助方需求"
        );
    }

    public Path launchDirectAssist(String relayUrl, String sessionId, String requestContent) throws IOException {
        return launchRequest(
                "direct-" + Instant.now().toEpochMilli(),
                relayUrl,
                sessionId,
                requestContent,
                "# AI Remote Helper Direct Assist Request",
                "## 协助方直接输入的需求"
        );
    }

    private Path launchRequest(String requestId, String relayUrl, String sessionId, String requestContent,
                               String title, String requestHeading) throws IOException {
        Path runDir = resolveRunDir(requestId);
        Files.createDirectories(runDir);
        Path promptPath = runDir.resolve("approved-request-prompt.md");
        Path scriptPath = runDir.resolve("start-codex.ps1");
        Files.writeString(promptPath, buildPrompt(relayUrl, sessionId, requestContent, title, requestHeading), StandardCharsets.UTF_8);
        Files.writeString(scriptPath, buildScript(promptPath, runDir), StandardCharsets.UTF_8);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "cmd.exe",
                "/c",
                "start",
                "",
                "powershell.exe",
                "-NoExit",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                scriptPath.toString()
        );
        processBuilder.directory(runDir.toFile());
        processBuilder.start();
        return promptPath;
    }

    private Path resolveRunDir(String requestId) {
        String configuredRoot = System.getenv("AIRH_AI_RUNNER_WORKDIR");
        Path root = configuredRoot == null || configuredRoot.isBlank()
                ? Path.of(System.getProperty("user.home"), ".ai-remote-helper", "ai-runs")
                : Path.of(configuredRoot);
        return root.resolve(safeName(requestId));
    }

    private String buildScript(Path promptPath, Path runDir) {
        String runner = System.getenv("AIRH_AI_RUNNER_COMMAND");
        if (runner == null || runner.isBlank()) {
            runner = DEFAULT_RUNNER;
        }
        return """
                $ErrorActionPreference = 'Stop'
                $runner = '%s'
                $promptPath = '%s'
                $workDir = '%s'
                Write-Host '[AI Remote Helper] Approved request prompt:' $promptPath -ForegroundColor Cyan
                Write-Host '[AI Remote Helper] Work directory:' $workDir -ForegroundColor Cyan
                Write-Host '[AI Remote Helper] Launching visible Codex session. Dangerous bypass flags are not used.' -ForegroundColor Green
                $prompt = Get-Content -LiteralPath $promptPath -Raw
                & $runner -C $workDir --ask-for-approval on-request $prompt
                """.formatted(escapePowerShellSingleQuoted(runner), escapePowerShellSingleQuoted(promptPath.toString()),
                escapePowerShellSingleQuoted(runDir.toString()));
    }

    private String buildPrompt(String relayUrl, String sessionId, String requestContent, String title, String requestHeading) {
        return """
                %s

                你正在帮助一台远程 Agent 电脑处理已授权目录内的问题。

                %s

                %s

                ## Relay 信息

                - relay-server: `%s`
                - sessionId: `%s`

                ## 可用 REST API 示例

                - 查看在线设备：`GET %s/api/devices/online`
                - 创建任务：`POST %s/api/sessions/%s/tasks`
                - 查看任务：`GET %s/api/tasks/{taskId}`
                - 查看日志：`GET %s/api/tasks/{taskId}/logs`

                创建任务 JSON 示例：

                ```json
                {
                  "taskType": "LIST_DIR",
                  "payload": {
                    "data": {
                      "path": "."
                    }
                  },
                  "timeoutSeconds": 30
                }
                ```

                ## 强制安全边界

                - 不要修改本机 AI Remote Helper 仓库来完成对方需求。
                - 必须通过 relay-server REST API 操作远程 Agent，不要假设本地目录就是对方目录。
                - 所有文件和命令只能作用于被协助方已授权目录。
                - 不要请求管理员权限，不要隐藏运行，不要开机自启，不要接管鼠标键盘。
                - 遇到高风险命令、删除、覆盖、大规模改动时，先向协助者说明风险并等待确认。
                - 每次操作后尽量通过任务日志或任务结果确认实际效果。
                """.formatted(title, requestHeading, requestContent, relayUrl, sessionId, relayUrl, relayUrl, sessionId, relayUrl, relayUrl);
    }

    private String safeName(String value) {
        String raw = value == null || value.isBlank() ? Instant.now().toString() : value;
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    private String escapePowerShellSingleQuoted(String value) {
        return value.replace("'", "''");
    }
}
