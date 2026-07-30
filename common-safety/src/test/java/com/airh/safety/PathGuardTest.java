package com.airh.safety;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathGuardTest {
    @TempDir
    Path tempDir;

    @Test
    void testResolveSafePath() {
        PathGuard guard = new PathGuard(tempDir);
        Path resolved = guard.resolveSafePath("test.txt");
        assertTrue(guard.isInsideWorkspace(resolved));
    }

    @Test
    void testPathTraversalBlocked() {
        PathGuard guard = new PathGuard(tempDir);
        assertThrows(SecurityException.class, () -> guard.resolveSafePath("../outside.txt"));
    }

    @Test
    void testAbsoluteBlocked() {
        PathGuard guard = new PathGuard(tempDir);
        assertThrows(SecurityException.class, () -> guard.resolveSafePath(tempDir.getRoot().resolve("outside.txt").toString()));
    }

    @Test
    void testDoubleDotBlocked() {
        PathGuard guard = new PathGuard(tempDir);
        assertThrows(SecurityException.class, () -> guard.resolveSafePath("subdir/../../outside.txt"));
    }

    @Test
    void fileNameContainingTwoDotsIsAllowed() {
        PathGuard guard = new PathGuard(tempDir);
        assertTrue(guard.isInsideWorkspace(guard.resolveSafePath("release..notes.txt")));
    }

    @Test
    void testNullPathReturnsRoot() {
        PathGuard guard = new PathGuard(tempDir);
        assertEquals(tempDir.toAbsolutePath(), guard.resolveSafePath(null));
    }

    @Test
    void testBlankPathReturnsRoot() {
        PathGuard guard = new PathGuard(tempDir);
        assertEquals(tempDir.toAbsolutePath(), guard.resolveSafePath(""));
    }

    @Test
    void testIsInsideWorkspace() {
        PathGuard guard = new PathGuard(tempDir);
        assertTrue(guard.isInsideWorkspace(tempDir.resolve("sub")));
        assertFalse(guard.isInsideWorkspace(tempDir.resolve("../outside")));
    }

    @Test
    void testGetRelativePath() throws IOException {
        PathGuard guard = new PathGuard(tempDir);
        Path abs = Files.createDirectories(tempDir.resolve("sub")).resolve("file.txt");
        assertEquals(Path.of("sub", "file.txt").toString(), guard.getRelativePath(abs));
    }

    @Test
    void blocksSymbolicLinkEscapeWhenSupported() throws IOException {
        Path outside = Files.createTempDirectory(tempDir.getParent(), "airh-outside-");
        Path link = tempDir.resolve("outside-link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | SecurityException | IOException exception) {
            return;
        }

        PathGuard guard = new PathGuard(tempDir);
        assertThrows(SecurityException.class, () -> guard.resolveSafePath("outside-link/secret.txt"));
        assertFalse(guard.isInsideWorkspace(link.resolve("secret.txt")));
    }
}
