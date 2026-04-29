package com.airh.safety;

import java.nio.file.Path;
import java.util.Set;

/**
 * 敏感文件守卫 - 检测并阻止访问敏感文件
 */
public class SensitiveFileGuard {

    private static final Set<String> BLOCKED_FILES = Set.of(
        "id_rsa", "id_ed25519", "id_ecdsa", "known_hosts",
        "credentials", ".netrc", ".pgpass"
    );

    private static final Set<String> BLOCKED_DIRS = Set.of(
        ".ssh", ".aws", ".kube", ".gnupg", ".config/gcloud"
    );

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
        ".pem", ".key", ".p12", ".pfx", ".jks"
    );

    /**
     * 检查文件是否是敏感文件
     */
    public SecurityDecision checkFileAccess(Path filePath, Path workspaceRoot) {
        if (filePath == null) {
            return SecurityDecision.blocked("Null file path");
        }

        String pathStr = filePath.toString().toLowerCase();
        String fileName = filePath.getFileName().toString().toLowerCase();

        // 检查明确阻止的文件
        if (BLOCKED_FILES.contains(fileName)) {
            return SecurityDecision.blocked("Sensitive file: " + fileName);
        }

        // 检查阻止的目录
        for (String blockedDir : BLOCKED_DIRS) {
            if (pathStr.contains(blockedDir)) {
                return SecurityDecision.blocked("Sensitive directory: " + blockedDir);
            }
        }

        // 检查阻止的扩展名
        for (String ext : BLOCKED_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return SecurityDecision.blocked("Sensitive file type: " + ext);
            }
        }

        // .env 文件 - 警告但不阻止
        if (fileName.equals(".env")) {
            return SecurityDecision.warn(pathStr, ".env file detected - potential credentials");
        }

        return SecurityDecision.allowed(pathStr);
    }

    /**
     * 检查目录是否是敏感目录
     */
    public boolean isSensitiveDirectory(Path dirPath) {
        if (dirPath == null) return false;
        String pathStr = dirPath.toString().toLowerCase();
        for (String blockedDir : BLOCKED_DIRS) {
            if (pathStr.contains(blockedDir)) {
                return true;
            }
        }
        return false;
    }
}
