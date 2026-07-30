package com.airh.agent.safety;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

public class PathSandbox {
    private final Path authorizedDirectory;
    private final Path authorizedRealDirectory;

    public PathSandbox(Path authorizedDirectory) {
        Objects.requireNonNull(authorizedDirectory, "authorizedDirectory must not be null");
        this.authorizedDirectory = authorizedDirectory.toAbsolutePath().normalize();
        this.authorizedRealDirectory = resolveRealPath(this.authorizedDirectory);
    }

    public boolean isUnderAuthorizedDir(Path target) {
        if (target == null) {
            return false;
        }
        Path normalizedTarget = target.isAbsolute()
                ? target.toAbsolutePath().normalize()
                : authorizedDirectory.resolve(target).normalize();
        if (!normalizedTarget.startsWith(authorizedDirectory)) {
            return false;
        }
        try {
            return nearestExistingRealPath(normalizedTarget).startsWith(authorizedRealDirectory);
        } catch (IOException exception) {
            return false;
        }
    }

    public Path resolveSecurely(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new SecurityException("路径不能为空");
        }
        Path requestedPath = Path.of(relativePath);
        if (requestedPath.isAbsolute()) {
            throw new SecurityException("只允许授权目录内的相对路径：" + relativePath);
        }

        Path resolvedPath = authorizedDirectory.resolve(requestedPath).normalize();
        if (!resolvedPath.startsWith(authorizedDirectory)) {
            throw new SecurityException("路径越界，拒绝访问：" + relativePath);
        }
        try {
            Path realAncestor = nearestExistingRealPath(resolvedPath);
            if (!realAncestor.startsWith(authorizedRealDirectory)) {
                throw new SecurityException("路径通过符号链接越过授权目录，拒绝访问：" + relativePath);
            }
        } catch (IOException exception) {
            throw new SecurityException("无法验证路径安全性：" + relativePath, exception);
        }
        return resolvedPath;
    }

    public String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return Path.of(path).normalize().toString();
    }

    public Path authorizedDirectory() {
        return authorizedDirectory;
    }

    private Path nearestExistingRealPath(Path path) throws IOException {
        Path candidate = path;
        while (candidate != null && !Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            candidate = candidate.getParent();
        }
        if (candidate == null) {
            throw new IOException("No existing ancestor for path: " + path);
        }
        return candidate.toRealPath();
    }

    private Path resolveRealPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException exception) {
            return path;
        }
    }
}
