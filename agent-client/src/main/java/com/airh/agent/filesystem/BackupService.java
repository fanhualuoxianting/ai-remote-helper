package com.airh.agent.filesystem;

import com.airh.agent.safety.PathSandbox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class BackupService {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMddHHmmssSSSSSSSSS")
            .withZone(ZoneId.systemDefault());

    private final PathSandbox pathSandbox;
    private final Path backupRoot;
    private final Clock clock;

    public BackupService(PathSandbox pathSandbox) {
        this(pathSandbox, Clock.systemDefaultZone());
    }

    BackupService(PathSandbox pathSandbox, Clock clock) {
        this.pathSandbox = Objects.requireNonNull(pathSandbox, "pathSandbox must not be null");
        this.backupRoot = pathSandbox.authorizedDirectory().resolve(".ai-remote-helper").resolve("backups");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public BackupResult backupFile(Path target) throws IOException {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (!pathSandbox.isUnderAuthorizedDir(normalizedTarget)) {
            throw new SecurityException("备份目标不在授权目录内：" + target);
        }
        if (!Files.exists(normalizedTarget)) {
            return new BackupResult(null, false);
        }
        if (!Files.isRegularFile(normalizedTarget)) {
            throw new IOException("只允许备份普通文件：" + target);
        }

        Path relativePath = pathSandbox.authorizedDirectory().relativize(normalizedTarget);
        Instant backupInstant = Instant.now(clock);
        Path backupTarget = backupRoot.resolve(TIMESTAMP_FORMATTER.format(backupInstant)).resolve(relativePath).normalize();
        while (Files.exists(backupTarget)) {
            backupInstant = backupInstant.plusNanos(1);
            backupTarget = backupRoot.resolve(TIMESTAMP_FORMATTER.format(backupInstant)).resolve(relativePath).normalize();
        }
        if (!backupTarget.startsWith(backupRoot)) {
            throw new SecurityException("备份路径越界：" + relativePath);
        }

        Files.createDirectories(backupTarget.getParent());
        Files.copy(normalizedTarget, backupTarget);
        return new BackupResult(backupTarget, true);
    }

    public List<Path> listBackups(String relativePath) throws IOException {
        Path requestedPath = Path.of(relativePath);
        if (requestedPath.isAbsolute()) {
            throw new SecurityException("备份查询只接受相对路径：" + relativePath);
        }
        Path normalizedRelativePath = requestedPath.normalize();
        if (normalizedRelativePath.startsWith("..")) {
            throw new SecurityException("备份查询路径越界：" + relativePath);
        }
        if (!Files.exists(backupRoot)) {
            return List.of();
        }

        try (Stream<Path> timestampDirectories = Files.list(backupRoot)) {
            return timestampDirectories
                    .filter(Files::isDirectory)
                    .map(directory -> directory.resolve(normalizedRelativePath))
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(this::backupTimestamp).reversed())
                    .toList();
        }
    }

    public void cleanupOldBackups(String relativePath, int keepCount) throws IOException {
        List<Path> backups = listBackups(relativePath);
        for (int index = keepCount; index < backups.size(); index++) {
            Path backup = backups.get(index);
            Files.deleteIfExists(backup);
            deleteEmptyParentsUntilBackupRoot(backup.getParent());
        }
    }

    private String backupTimestamp(Path backupPath) {
        Path relativeToBackupRoot = backupRoot.relativize(backupPath);
        return relativeToBackupRoot.getName(0).toString();
    }

    private void deleteEmptyParentsUntilBackupRoot(Path directory) throws IOException {
        Path current = directory;
        while (current != null && current.startsWith(backupRoot) && !current.equals(backupRoot)) {
            try (Stream<Path> children = Files.list(current)) {
                if (children.findAny().isPresent()) {
                    return;
                }
            }
            Files.deleteIfExists(current);
            current = current.getParent();
        }
    }

    public record BackupResult(Path backupPath, boolean created) {
    }
}
