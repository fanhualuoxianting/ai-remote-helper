package com.airh.safety;

import com.airh.protocol.enums.RiskLevel;

import java.nio.file.Path;
import java.util.Set;

public class SensitiveFileGuard {
    private static final Set<String> BLOCKED_FILE_NAMES = Set.of(
            "id_rsa",
            "id_ed25519",
            "known_hosts",
            "credentials",
            "cookies"
    );

    private final SensitiveFileProtector sensitiveFileProtector = new SensitiveFileProtector();

    public boolean isSensitive(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }

        String fileName = path.getFileName().toString().toLowerCase();
        return sensitiveFileProtector.checkPath(path) == RiskLevel.BLOCKED
                || BLOCKED_FILE_NAMES.contains(fileName)
                || fileName.endsWith(".pem")
                || fileName.endsWith(".key");
    }
}
