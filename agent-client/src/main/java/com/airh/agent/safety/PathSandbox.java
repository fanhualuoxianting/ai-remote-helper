package com.airh.agent.safety;

import java.nio.file.Path;
import java.util.Objects;

public class PathSandbox {
    private final Path authorizedDirectory;

    public PathSandbox(Path authorizedDirectory) {
        Objects.requireNonNull(authorizedDirectory, "authorizedDirectory must not be null");
        this.authorizedDirectory = authorizedDirectory.toAbsolutePath().normalize();
    }

    public boolean isUnderAuthorizedDir(Path target) {
        if (target == null) {
            return false;
        }
        Path normalizedTarget = target.isAbsolute()
                ? target.toAbsolutePath().normalize()
                : authorizedDirectory.resolve(target).normalize();
        return normalizedTarget.startsWith(authorizedDirectory);
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
        if (!isUnderAuthorizedDir(resolvedPath)) {
            throw new SecurityException("路径越界，拒绝访问：" + relativePath);
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
}
