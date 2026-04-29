package com.airh.agent.filesystem;

import com.airh.agent.safety.PathSandbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemServiceTest {
    @TempDir
    Path authorizedDirectory;

    @Test
    void listsRealDirectoryEntries() throws Exception {
        Files.createDirectories(authorizedDirectory.resolve("src"));
        Files.writeString(authorizedDirectory.resolve("README.md"), "hello", StandardCharsets.UTF_8);
        FileSystemService service = new FileSystemService(new PathSandbox(authorizedDirectory));

        var entries = service.listDirectory(".");

        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(entry -> entry.name().equals("src") && entry.directory()));
        assertTrue(entries.stream().anyMatch(entry -> entry.name().equals("README.md") && !entry.directory() && entry.size() == 5));
    }

    @Test
    void readsUtf8TextFile() throws Exception {
        Files.writeString(authorizedDirectory.resolve("note.txt"), "中文内容", StandardCharsets.UTF_8);
        FileSystemService service = new FileSystemService(new PathSandbox(authorizedDirectory));

        var result = service.readFile("note.txt");

        assertTrue(result.contentReturned());
        assertFalse(result.binary());
        assertEquals("中文内容", result.content());
    }

    @Test
    void rejectsPathOutsideAuthorizedDirectory() {
        FileSystemService service = new FileSystemService(new PathSandbox(authorizedDirectory));

        assertThrows(SecurityException.class, () -> service.readFile("../secret.txt"));
    }

    @Test
    void skipsBinaryFileContent() throws Exception {
        Files.write(authorizedDirectory.resolve("image.bin"), new byte[]{0x01, 0x02, 0x00, 0x03});
        FileSystemService service = new FileSystemService(new PathSandbox(authorizedDirectory));

        var result = service.readFile("image.bin");

        assertTrue(result.binary());
        assertFalse(result.contentReturned());
        assertEquals(null, result.content());
    }
}
