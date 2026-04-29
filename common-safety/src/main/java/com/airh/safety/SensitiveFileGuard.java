package com.airh.safety;

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

    public boolean isSensitive(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }

        String fileName = path.getFileName().toString().toLowerCase();
        return BLOCKED_FILE_NAMES.contains(fileName) || fileName.endsWith(".pem") || fileName.endsWith(".key");
    }
}
