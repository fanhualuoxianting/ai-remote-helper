package com.airh.agent.filesystem;

import com.airh.agent.safety.PathSandbox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class FileSystemService {
    public static final long MAX_TEXT_FILE_BYTES = 1024L * 1024L;

    private final PathSandbox pathSandbox;

    public FileSystemService(PathSandbox pathSandbox) {
        this.pathSandbox = Objects.requireNonNull(pathSandbox, "pathSandbox must not be null");
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

    public static class FileSystemAccessException extends RuntimeException {
        public FileSystemAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
