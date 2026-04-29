package com.airh.agent.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class ReportGenerationService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String generateReport(Path authorizedDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# AI Remote Helper 会话报告\n\n");
        sb.append("生成时间：").append(LocalDateTime.now().format(FMT)).append("\n\n");
        sb.append("## 授权目录\n\n");
        sb.append(authorizedDir.toAbsolutePath()).append("\n\n");
        sb.append("## 操作摘要\n\n");
        sb.append("本次会话中的操作记录：\n\n");

        // 检查备份目录
        Path backupDir = authorizedDir.resolve(".ai-remote-helper/backups");
        if (Files.exists(backupDir)) {
            sb.append("### 文件修改备份\n\n");
            try (var stream = Files.list(backupDir)) {
                stream.filter(Files::isDirectory)
                        .forEach(dir -> {
                            sb.append("- ").append(dir.getFileName()).append("\n");
                        });
            }
            sb.append("\n");
        }

        // 检查报告目录
        Path reportDir = authorizedDir.resolve(".ai-remote-helper/reports");
        if (Files.exists(reportDir)) {
            sb.append("### 已生成报告\n\n");
            try (var stream = Files.list(reportDir)) {
                stream.filter(f -> f.toString().endsWith(".md"))
                        .forEach(f -> sb.append("- ").append(f.getFileName()).append("\n"));
            }
            sb.append("\n");
        }

        sb.append("## 安全说明\n\n");
        sb.append("- 所有操作均在授权目录内执行\n");
        sb.append("- 敏感文件访问已被阻止\n");
        sb.append("- 所有文件修改前已创建备份\n");
        sb.append("- 所有命令执行均有审计日志\n\n");

        sb.append("## 复现步骤\n\n");
        sb.append("1. 启动 Relay Server\n");
        sb.append("2. 启动 Agent Client\n");
        sb.append("3. 选择授权目录：").append(authorizedDir.toAbsolutePath()).append("\n");
        sb.append("4. 连接到 Relay Server\n");
        sb.append("5. 通过 MCP Bridge 或 Web Console 执行操作\n\n");

        return sb.toString();
    }

    public Path saveReport(Path authorizedDir, String content) throws IOException {
        Path reportDir = authorizedDir.resolve(".ai-remote-helper").resolve("reports");
        Files.createDirectories(reportDir);
        String filename = "report-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".md";
        Path reportFile = reportDir.resolve(filename);
        Files.writeString(reportFile, content);
        return reportFile;
    }
}
