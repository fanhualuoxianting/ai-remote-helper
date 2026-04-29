package com.airh.agent.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ReportGenerationServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void testGenerateReport() throws Exception {
        ReportGenerationService service = new ReportGenerationService();
        String report = service.generateReport(tempDir);
        assertNotNull(report);
        assertTrue(report.contains("AI Remote Helper 会话报告"));
        assertTrue(report.contains("授权目录"));
    }

    @Test
    void testSaveReport() throws Exception {
        ReportGenerationService service = new ReportGenerationService();
        String content = "# Test Report\n\nTest content.";
        Path saved = service.saveReport(tempDir, content);
        assertTrue(Files.exists(saved));
        assertTrue(saved.getFileName().toString().startsWith("report-"));
        assertTrue(saved.getFileName().toString().endsWith(".md"));
        assertEquals(content, Files.readString(saved));
    }
}
