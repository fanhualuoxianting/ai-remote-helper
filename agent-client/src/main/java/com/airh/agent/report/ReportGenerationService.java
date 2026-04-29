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
        sb.append("# AI Remote Helper Report\n\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(FMT)).append("\n\n");
        sb.append("## Authorized Directory\n\n");
        sb.append(authorizedDir.toAbsolutePath()).append("\n\n");
        sb.append("## Operations Summary\n\n");
        sb.append("Report generation is a placeholder in this version.\n");
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
