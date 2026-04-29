package com.airh.safety;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 路径沙箱守卫 - 防止路径穿越攻击
 */
public class PathGuard {
    private final Path workspaceRoot;

    public PathGuard(Path workspaceRoot) {
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("workspaceRoot cannot be null");
        }
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
    }

    public Path getWorkspaceRoot() {
        return workspaceRoot;
    }

    /**
     * 安全解析路径 - 防止路径穿越
     */
    public Path resolveSafePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return workspaceRoot;
        }

        // 检测危险模式
        if (containsPathTraversal(relativePath)) {
            throw new SecurityException("Path traversal detected: " + relativePath);
        }

        Path resolved = workspaceRoot.resolve(relativePath).normalize().toAbsolutePath();

        // 双重检查：解析后必须在工作空间内
        if (!isInsideWorkspace(resolved)) {
            throw new SecurityException("Path outside workspace: " + relativePath);
        }

        return resolved;
    }

    /**
     * 检查路径是否在工作空间内
     */
    public boolean isInsideWorkspace(Path path) {
        if (path == null) return false;
        Path normalized = path.toAbsolutePath().normalize();
        return normalized.startsWith(workspaceRoot);
    }

    /**
     * 检查路径是否包含穿越模式
     */
    public boolean containsPathTraversal(String path) {
        if (path == null) return false;
        // 检查 ..
        if (path.contains("..")) return true;
        // 检查绝对路径（非相对路径）
        if (Paths.get(path).isAbsolute()) return true;
        return false;
    }

    /**
     * 获取相对路径
     */
    public String getRelativePath(Path absolutePath) {
        return workspaceRoot.relativize(absolutePath).toString();
    }
}
