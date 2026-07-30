package com.airh.safety;

import com.airh.protocol.enums.RiskLevel;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class SensitiveFileProtector {
    private static final List<String> BLOCKED_SEGMENTS = List.of(
            "/.ssh/",
            "/.gnupg/",
            "/.aws/",
            "/.azure/",
            "/.kube/",
            "/.config/gcloud/",
            "/appdata/local/google/chrome/user data/",
            "/appdata/roaming/mozilla/firefox/profiles/",
            "/appdata/local/microsoft/edge/user data/",
            "/library/application support/google/chrome/",
            "/library/application support/firefox/profiles/",
            "/library/application support/microsoft edge/",
            "/.config/google-chrome/",
            "/.mozilla/firefox/",
            "/.config/microsoft-edge/",
            "/windows/system32/config/sam/",
            "/windows/system32/config/security/",
            "/windows/system32/config/system/",
            "/etc/passwd/",
            "/etc/shadow/",
            "/etc/sudoers/",
            "/etc/security/"
    );

    private static final List<String> BLOCKED_FILE_NAMES = List.of(
            "id_rsa", "id_dsa", "id_ecdsa", "id_ed25519",
            "credentials", "application_default_credentials.json",
            "kubeconfig", "login data", "cookies", "logins.json", "key4.db"
    );

    private static final List<String> HIGH_SEGMENTS = List.of(
            "/.bashrc/",
            "/.zshrc/",
            "/.profile/",
            "/appdata/roaming/microsoft/windows/start menu/programs/startup/",
            "/programdata/microsoft/windows/start menu/programs/startup/",
            "/windows/system32/drivers/etc/hosts/",
            "/etc/hosts/"
    );

    private static final List<String> HIGH_FILE_NAMES = List.of(
            ".env", ".env.local", ".env.production", ".npmrc", ".pypirc",
            "settings.xml", "gradle.properties"
    );

    public RiskLevel checkPath(String path) {
        if (path == null || path.isBlank()) {
            return RiskLevel.LOW;
        }

        String normalized = normalizePath(path);
        String fileName = fileName(normalized);
        if (containsAny(normalized, BLOCKED_SEGMENTS) || BLOCKED_FILE_NAMES.contains(fileName)) {
            return RiskLevel.BLOCKED;
        }
        if (containsAny(normalized, HIGH_SEGMENTS) || HIGH_FILE_NAMES.contains(fileName)) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.LOW;
    }

    public RiskLevel checkPath(Path path) {
        return path == null ? RiskLevel.LOW : checkPath(path.toString());
    }

    private String normalizePath(String path) {
        String expanded = expandHome(path.trim());
        String normalized = expanded.replace('\\', '/')
                .replaceAll("/+", "/")
                .toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private String fileName(String normalizedPath) {
        String withoutTrailingSlash = normalizedPath.substring(0, normalizedPath.length() - 1);
        int separator = withoutTrailingSlash.lastIndexOf('/');
        return separator >= 0 ? withoutTrailingSlash.substring(separator + 1) : withoutTrailingSlash;
    }

    private String expandHome(String path) {
        if (!path.equals("~") && !path.startsWith("~/") && !path.startsWith("~\\")) {
            return path;
        }
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return path.substring(1);
        }
        return home + path.substring(1);
    }

    private boolean containsAny(String path, List<String> protectedSegments) {
        return protectedSegments.stream().anyMatch(path::contains);
    }
}
