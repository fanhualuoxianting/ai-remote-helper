package com.airh.safety;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

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
        assertThrows(SecurityException.class, () -> guard.resolveSafePath("/etc/passwd"));
    }

    @Test
    void testDoubleDotBlocked() {
        PathGuard guard = new PathGuard(tempDir);
        assertThrows(SecurityException.class, () -> guard.resolveSafePath("subdir/../../etc/passwd"));
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
    void testGetRelativePath() {
        PathGuard guard = new PathGuard(tempDir);
        Path abs = tempDir.resolve("sub/file.txt");
        assertEquals("sub\\file.txt", guard.getRelativePath(abs));
    }
}
