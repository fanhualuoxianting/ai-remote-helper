package com.airh.agent.safety;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathSandboxTest {
    @TempDir
    Path authorizedDirectory;

    @Test
    void allowsNormalRelativePath() {
        PathSandbox sandbox = new PathSandbox(authorizedDirectory);

        Path resolved = sandbox.resolveSecurely("src/main.java");

        assertEquals(authorizedDirectory.resolve("src/main.java").normalize(), resolved);
        assertTrue(sandbox.isUnderAuthorizedDir(resolved));
    }

    @Test
    void rejectsPathTraversal() {
        PathSandbox sandbox = new PathSandbox(authorizedDirectory);

        assertThrows(SecurityException.class, () -> sandbox.resolveSecurely("../../../etc/passwd"));
        assertFalse(sandbox.isUnderAuthorizedDir(authorizedDirectory.resolve("../../../etc/passwd").normalize()));
    }

    @Test
    void rejectsAbsolutePath() {
        PathSandbox sandbox = new PathSandbox(authorizedDirectory);
        assertThrows(SecurityException.class,
                () -> sandbox.resolveSecurely(authorizedDirectory.getRoot().resolve("outside.txt").toString()));
    }

    @Test
    void blocksSymbolicLinkEscapeWhenSupported() throws IOException {
        Path outside = Files.createTempDirectory(authorizedDirectory.getParent(), "airh-outside-");
        Path link = authorizedDirectory.resolve("outside-link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | SecurityException | IOException exception) {
            return;
        }

        PathSandbox sandbox = new PathSandbox(authorizedDirectory);
        assertThrows(SecurityException.class, () -> sandbox.resolveSecurely("outside-link/secret.txt"));
        assertFalse(sandbox.isUnderAuthorizedDir(link.resolve("secret.txt")));
    }
}
