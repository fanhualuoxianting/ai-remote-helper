package com.airh.safety;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 路径沙箱守卫 - 防止路径穿越和符号链接逃逸。
 */
public class PathGuard {
    private final Path workspaceRoot;
    private final Path workspaceRealRoot;

    public PathGuard(Path workspaceRoot) {
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("workspaceRoot cannot be null");
        }
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        this.workspaceRealRoot = resolveRealPath(this.workspaceRoot);
    }

    public Path getWorkspaceRoot() {
        return workspaceRoot;
    }

    public Path resolveSafePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return workspaceRoot;
        }
        if (containsPathTraversal(relativePath)) {
            throw new SecurityException("Path traversal detected: " + relativePath);
        }

        Path resolved = workspaceRoot.resolve(relativePath).normalize().toAbsolutePath();
        if (!resolved.startsWith(workspaceRoot)) {
            throw new SecurityException("Path outside workspace: " + relativePath);
        }
        try {
            if (!nearestExistingRealPath(resolved).startsWith(workspaceRealRoot)) {
                throw new SecurityException("Path escapes workspace through symbolic link: " + relativePath);
            }
        } catch (IOException exception) {
            throw new SecurityException("Unable to verify path safety: " + relativePath, exception);
        }
        return resolved;
    }

    public boolean isInsideWorkspace(Path path) {
        if (path == null) {
            return false;
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(workspaceRoot)) {
            return false;
        }
        try {
            return nearestExistingRealPath(normalized).startsWith(workspaceRealRoot);
        } catch (IOException exception) {
            return false;
        }
    }

    public boolean containsPathTraversal(String path) {
        if (path == null) {
            return false;
        }
        Path parsed = Paths.get(path);
        if (parsed.isAbsolute()) {
            return true;
        }
        for (Path segment : parsed) {
            if ("..".equals(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    public String getRelativePath(Path absolutePath) {
        Path normalized = absolutePath.toAbsolutePath().normalize();
        if (!isInsideWorkspace(normalized)) {
            throw new SecurityException("Path outside workspace: " + absolutePath);
        }
        return workspaceRoot.relativize(normalized).toString();
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
