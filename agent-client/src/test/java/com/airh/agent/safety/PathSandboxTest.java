package com.airh.agent.safety;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
}
