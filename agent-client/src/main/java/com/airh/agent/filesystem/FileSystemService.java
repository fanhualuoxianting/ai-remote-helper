package com.airh.agent.filesystem;

import com.airh.agent.safety.PathSandbox;
import com.airh.protocol.enums.RiskLevel;
import com.airh.safety.SensitiveFileProtector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FileSystemService {
    public static final long MAX_TEXT_FILE_BYTES = 1024L * 1024L;
    private static final Logger LOGGER = Logger.getLogger(FileSystemService.class.getName());

    private final PathSandbox pathSandbox;
    private final BackupService backupService;
    private final SensitiveFileProtector sensitiveFileProtector;

    public FileSystemService(PathSandbox pathSandbox) {
        this.pathSandbox = Objects.requireNonNull(pathSandbox, "pathSandbox must not be null");
        this.backupService = new BackupService(pathSandbox);
        this.sensitiveFileProtector = new SensitiveFileProtector();
    }

    public List<FileEntry> listDirectory(String relativePath) throws IOException {
        Path directory = pathSandbox.resolveSecurely(normalizeInputPath(relativePath));
        if (!Files.exists(directory)) {
            throw new IOException("目录不存在：" + relativePath);
        }
        if (!Files.isDirectory(directory)) {
            throw new IOException("目标不是目录：" + relativePath);
        }

        try (Stream<Path> children = Files.list(directory)) {
            return children
                    .map(this::toFileEntry)
                    .sorted(Comparator.comparing(FileEntry::directory).reversed()
                            .thenComparing(FileEntry::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
    }

    public ReadFileResult readFile(String relativePath) throws IOException {
        Path file = pathSandbox.resolveSecurely(normalizeInputPath(relativePath));
        checkSensitivePath(file, "读取");
        if (!Files.exists(file)) {
            throw new IOException("文件不存在：" + relativePath);
        }
        if (!Files.isRegularFile(file)) {
            throw new IOException("目标不是普通文件：" + relativePath);
        }

        long size = Files.size(file);
        String contentType = Files.probeContentType(file);
        FileTime modifiedTime = Files.getLastModifiedTime(file);
        if (size > MAX_TEXT_FILE_BYTES) {
            return new ReadFileResult(file.getFileName().toString(), size, modifiedTime.toInstant(),
                    contentType, false, false, null, "文件超过 1MB 限制，未读取内容");
        }

        byte[] bytes = Files.readAllBytes(file);
        if (isBinary(bytes)) {
            return new ReadFileResult(file.getFileName().toString(), size, modifiedTime.toInstant(),
                    contentType, true, false, null, "检测到二进制文件，未读取内容");
        }

        String content;
        try {
            content = decodeUtf8(bytes);
        } catch (CharacterCodingException exception) {
            return new ReadFileResult(file.getFileName().toString(), size, modifiedTime.toInstant(),
                    contentType, true, false, null, "文件不是有效 UTF-8 文本，未读取内容");
        }
        return new ReadFileResult(file.getFileName().toString(), size, modifiedTime.toInstant(),
                contentType, false, true, content, null);
    }

    public WriteFileResult writeFile(String relativePath, String content) throws IOException {
        String normalizedRelativePath = normalizeWritePath(relativePath);
        Path file = pathSandbox.resolveSecurely(normalizedRelativePath);
        checkSensitivePath(file, "写入");
        if (Files.exists(file) && !Files.isRegularFile(file)) {
            throw new IOException("目标不是普通文件：" + relativePath);
        }

        String oldContent = Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
        BackupService.BackupResult backup = backupService.backupFile(file);

        Files.createDirectories(file.getParent());
        String newContent = content == null ? "" : content;
        Files.writeString(file, newContent, StandardCharsets.UTF_8);
        backupService.cleanupOldBackups(normalizedRelativePath, 3);

        DiffSummary diffSummary = createDiffSummary(oldContent, newContent);
        return new WriteFileResult(normalizedRelativePath, Files.size(file), backupPathText(backup),
                diffSummary, "文件写入成功：" + normalizedRelativePath);
    }

    public WriteFileResult applyPatch(String relativePath, String patch) throws IOException {
        String normalizedRelativePath = normalizeWritePath(relativePath);
        Path file = pathSandbox.resolveSecurely(normalizedRelativePath);
        checkSensitivePath(file, "应用补丁");
        if (!Files.exists(file)) {
            throw new IOException("文件不存在，无法应用补丁：" + relativePath);
        }
        if (!Files.isRegularFile(file)) {
            throw new IOException("目标不是普通文件：" + relativePath);
        }

        String oldContent = Files.readString(file, StandardCharsets.UTF_8);
        String newContent = applyUnifiedPatch(oldContent, patch);
        BackupService.BackupResult backup = backupService.backupFile(file);

        Files.writeString(file, newContent, StandardCharsets.UTF_8);
        backupService.cleanupOldBackups(normalizedRelativePath, 3);

        DiffSummary diffSummary = createDiffSummary(oldContent, newContent);
        return new WriteFileResult(normalizedRelativePath, Files.size(file), backupPathText(backup),
                diffSummary, "补丁应用成功：" + normalizedRelativePath);
    }

    private FileEntry toFileEntry(Path path) {
        try {
            boolean directory = Files.isDirectory(path);
            long size = directory ? 0L : Files.size(path);
            Instant modifiedTime = Files.getLastModifiedTime(path).toInstant();
            return new FileEntry(path.getFileName().toString(), size, modifiedTime, directory);
        } catch (IOException exception) {
            throw new FileSystemAccessException("读取文件信息失败：" + path.getFileName(), exception);
        }
    }

    private String normalizeInputPath(String relativePath) {
        return relativePath == null || relativePath.isBlank() ? "." : relativePath;
    }

    private String normalizeWritePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || ".".equals(relativePath.trim())) {
            throw new SecurityException("写入路径必须是具体文件路径");
        }
        return pathSandbox.normalize(relativePath);
    }

    private void checkSensitivePath(Path path, String operation) {
        RiskLevel riskLevel = sensitiveFileProtector.checkPath(path);
        if (riskLevel == RiskLevel.BLOCKED) {
            throw new SecurityException(operation + "命中敏感文件阻断规则，拒绝访问：" + path);
        }
        if (riskLevel == RiskLevel.HIGH) {
            LOGGER.warning(operation + "命中 HIGH 敏感路径规则，将继续执行：" + path);
        }
    }

    private String applyUnifiedPatch(String oldContent, String patch) throws IOException {
        if (patch == null || patch.isBlank()) {
            throw new IOException("补丁内容不能为空");
        }

        List<String> originalLines = splitLines(oldContent);
        List<String> patchLines = patch.lines().toList();
        List<String> resultLines = new ArrayList<>();
        int originalIndex = 0;
        int patchIndex = 0;

        while (patchIndex < patchLines.size()) {
            String line = patchLines.get(patchIndex);
            if (line.startsWith("---") || line.startsWith("+++") || line.isBlank()) {
                patchIndex++;
                continue;
            }
            if (!line.startsWith("@@")) {
                throw new IOException("不支持的补丁格式，缺少 hunk 头：" + line);
            }

            HunkRange range = parseHunkRange(line);
            int targetOriginalIndex = range.oldStart() - 1;
            while (originalIndex < targetOriginalIndex && originalIndex < originalLines.size()) {
                resultLines.add(originalLines.get(originalIndex++));
            }
            patchIndex++;

            while (patchIndex < patchLines.size() && !patchLines.get(patchIndex).startsWith("@@")) {
                String patchLine = patchLines.get(patchIndex);
                if (patchLine.startsWith("\\ No newline at end of file")) {
                    patchIndex++;
                    continue;
                }
                if (patchLine.isEmpty()) {
                    throw new IOException("补丁行必须以空格、+ 或 - 开头");
                }
                char marker = patchLine.charAt(0);
                String value = patchLine.substring(1);
                switch (marker) {
                    case ' ' -> {
                        ensureOriginalLineMatches(originalLines, originalIndex, value);
                        resultLines.add(originalLines.get(originalIndex++));
                    }
                    case '-' -> {
                        ensureOriginalLineMatches(originalLines, originalIndex, value);
                        originalIndex++;
                    }
                    case '+' -> resultLines.add(value);
                    default -> throw new IOException("不支持的补丁行标记：" + marker);
                }
                patchIndex++;
            }
        }

        while (originalIndex < originalLines.size()) {
            resultLines.add(originalLines.get(originalIndex++));
        }
        return String.join(System.lineSeparator(), resultLines)
                + (oldContent.endsWith("\n") || oldContent.endsWith("\r\n") ? System.lineSeparator() : "");
    }

    private HunkRange parseHunkRange(String hunkHeader) throws IOException {
        String[] parts = hunkHeader.split(" ");
        if (parts.length < 2 || !parts[1].startsWith("-")) {
            throw new IOException("无法解析 hunk 头：" + hunkHeader);
        }
        String range = parts[1].substring(1);
        String startText = range.contains(",") ? range.substring(0, range.indexOf(',')) : range;
        try {
            return new HunkRange(Integer.parseInt(startText));
        } catch (NumberFormatException exception) {
            throw new IOException("无法解析 hunk 起始行：" + hunkHeader, exception);
        }
    }

    private void ensureOriginalLineMatches(List<String> originalLines, int originalIndex, String expected) throws IOException {
        if (originalIndex >= originalLines.size()) {
            throw new IOException("补丁上下文超出文件长度");
        }
        String actual = originalLines.get(originalIndex);
        if (!actual.equals(expected)) {
            throw new IOException("补丁上下文不匹配，期望：" + expected + "，实际：" + actual);
        }
    }

    private List<String> splitLines(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        return content.lines().toList();
    }

    private DiffSummary createDiffSummary(String oldContent, String newContent) {
        List<String> oldLines = splitLines(oldContent);
        List<String> newLines = splitLines(newContent);
        int commonPrefix = 0;
        while (commonPrefix < oldLines.size()
                && commonPrefix < newLines.size()
                && oldLines.get(commonPrefix).equals(newLines.get(commonPrefix))) {
            commonPrefix++;
        }

        int oldSuffix = oldLines.size() - 1;
        int newSuffix = newLines.size() - 1;
        while (oldSuffix >= commonPrefix
                && newSuffix >= commonPrefix
                && oldLines.get(oldSuffix).equals(newLines.get(newSuffix))) {
            oldSuffix--;
            newSuffix--;
        }

        int removedLines = Math.max(0, oldSuffix - commonPrefix + 1);
        int addedLines = Math.max(0, newSuffix - commonPrefix + 1);
        String summary = "新增 " + addedLines + " 行，删除 " + removedLines + " 行";
        return new DiffSummary(addedLines, removedLines, oldLines.size(), newLines.size(), summary);
    }

    private String backupPathText(BackupService.BackupResult backup) {
        return backup.created() ? backup.backupPath().toString() : null;
    }

    private boolean isBinary(byte[] bytes) {
        int inspectedLength = Math.min(bytes.length, 8192);
        for (int i = 0; i < inspectedLength; i++) {
            int value = bytes[i] & 0xFF;
            if (value == 0) {
                return true;
            }
            if (value < 0x09 || (value > 0x0D && value < 0x20)) {
                return true;
            }
        }
        return false;
    }

    private String decodeUtf8(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    public record FileEntry(String name, long size, Instant modifiedTime, boolean directory) {
    }

    public record ReadFileResult(
            String name,
            long size,
            Instant modifiedTime,
            String contentType,
            boolean binary,
            boolean contentReturned,
            String content,
            String message
    ) {
    }

    public record WriteFileResult(
            String path,
            long size,
            String backupPath,
            DiffSummary diffSummary,
            String message
    ) {
    }

    public record DiffSummary(
            int addedLines,
            int removedLines,
            int oldLineCount,
            int newLineCount,
            String summary
    ) {
    }

    private record HunkRange(int oldStart) {
    }

    public static class FileSystemAccessException extends RuntimeException {
        public FileSystemAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
