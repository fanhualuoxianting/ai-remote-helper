package com.airh.agent.executor;

import com.airh.agent.safety.PathSandbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class CommandExecutionService implements AutoCloseable {
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int MAX_TIMEOUT_SECONDS = 300;

    private final PathSandbox pathSandbox;
    private final ExecutorService outputReaderExecutor;
    private volatile Process runningProcess;
    private final AtomicBoolean killed = new AtomicBoolean(false);

    public CommandExecutionService(PathSandbox pathSandbox) {
        this.pathSandbox = Objects.requireNonNull(pathSandbox, "pathSandbox must not be null");
        this.outputReaderExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "agent-command-output-reader");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CommandResult execute(String command, String workingDir, Integer timeoutSeconds, Consumer<String> outputCallback)
            throws IOException, InterruptedException {
        return execute(command, workingDir, timeoutSeconds, outputCallback, outputCallback);
    }

    public CommandResult execute(String command, String workingDir, Integer timeoutSeconds,
                                 Consumer<String> stdoutCallback, Consumer<String> stderrCallback)
            throws IOException, InterruptedException {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command 不能为空");
        }

        Path effectiveWorkingDirectory = resolveWorkingDirectory(workingDir);
        int effectiveTimeoutSeconds = normalizeTimeout(timeoutSeconds);
        Instant startedAt = Instant.now();
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        killed.set(false);

        ProcessBuilder processBuilder = new ProcessBuilder(shellCommand(command));
        processBuilder.directory(effectiveWorkingDirectory.toFile());
        processBuilder.redirectErrorStream(false);

        Process process = processBuilder.start();
        runningProcess = process;
        closeProcessStdin(process);

        CompletableFuture<Void> stdoutReader = CompletableFuture.runAsync(
                () -> readStream(process.getInputStream(), stdout, stdoutCallback), outputReaderExecutor);
        CompletableFuture<Void> stderrReader = CompletableFuture.runAsync(
                () -> readStream(process.getErrorStream(), stderr, stderrCallback), outputReaderExecutor);

        boolean finished = process.waitFor(effectiveTimeoutSeconds, TimeUnit.SECONDS);
        boolean timedOut = !finished;
        if (timedOut) {
            kill();
            process.waitFor(5, TimeUnit.SECONDS);
        }

        waitForReader(stdoutReader);
        waitForReader(stderrReader);
        runningProcess = null;

        int exitCode = process.isAlive() ? -1 : process.exitValue();
        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        return new CommandResult(exitCode, stdout.toString(), stderr.toString(), durationMs, timedOut, killed.get());
    }

    public void kill() {
        Process process = runningProcess;
        if (process == null) {
            return;
        }
        killed.set(true);
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private Path resolveWorkingDirectory(String workingDir) throws IOException {
        Path candidate;
        if (workingDir == null || workingDir.isBlank() || ".".equals(workingDir.strip())) {
            candidate = pathSandbox.authorizedDirectory();
        } else {
            Path requestedPath = Path.of(workingDir);
            if (requestedPath.isAbsolute()) {
                candidate = requestedPath.toAbsolutePath().normalize();
                if (!pathSandbox.isUnderAuthorizedDir(candidate)) {
                    throw new SecurityException("命令工作目录越界，拒绝执行：" + workingDir);
                }
            } else {
                candidate = pathSandbox.resolveSecurely(workingDir);
            }
        }
        if (!Files.isDirectory(candidate)) {
            throw new IOException("命令工作目录不存在或不是目录：" + candidate);
        }
        return candidate;
    }

    private int normalizeTimeout(Integer timeoutSeconds) {
        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
        return Math.min(timeoutSeconds, MAX_TIMEOUT_SECONDS);
    }

    private List<String> shellCommand(String command) {
        String os = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
        if (os.contains("win")) {
            return List.of("cmd.exe", "/c", command);
        }
        return List.of("sh", "-c", command);
    }

    private void closeProcessStdin(Process process) throws IOException {
        process.getOutputStream().close();
    }

    private void readStream(InputStream inputStream, StringBuilder output, Consumer<String> callback) {
        Charset charset = Charset.defaultCharset();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, read);
                synchronized (output) {
                    output.append(chunk);
                }
                if (callback != null) {
                    callback.accept(chunk);
                }
            }
        } catch (IOException exception) {
            if (callback != null) {
                callback.accept("读取命令输出失败：" + exception.getMessage());
            }
        }
    }

    private void waitForReader(CompletableFuture<Void> reader) {
        try {
            reader.get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            reader.cancel(true);
        }
    }

    @Override
    public void close() {
        kill();
        outputReaderExecutor.shutdownNow();
    }
}
