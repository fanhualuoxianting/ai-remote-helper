package com.airh.agent.executor;

import com.airh.agent.safety.PathSandbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandExecutionServiceTest {
    @TempDir
    Path authorizedDirectory;

    @Test
    void echoCommandSucceedsAndStreamsStdout() throws Exception {
        try (CommandExecutionService service = new CommandExecutionService(new PathSandbox(authorizedDirectory))) {
            List<String> chunks = new ArrayList<>();

            CommandResult result = service.execute(echoCommand("hello-airh"), ".", 5, chunks::add);

            assertEquals(0, result.exitCode());
            assertTrue(result.stdout().contains("hello-airh"));
            assertEquals("", result.stderr());
            assertFalse(result.timedOut());
            assertFalse(result.killed());
            assertTrue(String.join("", chunks).contains("hello-airh"));
        }
    }

    @Test
    void commandTimesOutAndIsKilled() throws Exception {
        try (CommandExecutionService service = new CommandExecutionService(new PathSandbox(authorizedDirectory))) {
            CommandResult result = service.execute(longRunningCommand(), ".", 1, ignored -> {
            });

            assertTrue(result.timedOut());
            assertTrue(result.killed());
            assertTrue(result.durationMs() < 5_000);
        }
    }

    @Test
    void commandUsesRequestedWorkingDirectory() throws Exception {
        Path childDirectory = Files.createDirectories(authorizedDirectory.resolve("child"));
        try (CommandExecutionService service = new CommandExecutionService(new PathSandbox(authorizedDirectory))) {
            CommandResult result = service.execute(printWorkingDirectoryCommand(), "child", 5, ignored -> {
            });

            assertEquals(0, result.exitCode());
            assertTrue(result.stdout().toLowerCase(java.util.Locale.ROOT)
                    .contains(childDirectory.toString().toLowerCase(java.util.Locale.ROOT)));
        }
    }

    @Test
    void capturesNonZeroExitCode() throws Exception {
        try (CommandExecutionService service = new CommandExecutionService(new PathSandbox(authorizedDirectory))) {
            CommandResult result = service.execute(nonZeroExitCommand(), ".", 5, ignored -> {
            });

            assertEquals(7, result.exitCode());
            assertFalse(result.timedOut());
        }
    }

    private String echoCommand(String text) {
        return "echo " + text;
    }

    private String longRunningCommand() {
        if (isWindows()) {
            return "ping 127.0.0.1 -n 4 > nul";
        }
        return "sleep 3";
    }

    private String printWorkingDirectoryCommand() {
        if (isWindows()) {
            return "cd";
        }
        return "pwd";
    }

    private String nonZeroExitCommand() {
        if (isWindows()) {
            return "exit /b 7";
        }
        return "exit 7";
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}
