package com.airh.safety;

import java.nio.file.Path;

public class PathGuard {
    public boolean isInsideAuthorizedDirectory(Path authorizedDirectory, Path requestedPath) {
        if (authorizedDirectory == null || requestedPath == null) {
            return false;
        }

        Path normalizedBase = authorizedDirectory.toAbsolutePath().normalize();
        Path normalizedRequested = requestedPath.toAbsolutePath().normalize();
        return normalizedRequested.startsWith(normalizedBase);
    }
}
