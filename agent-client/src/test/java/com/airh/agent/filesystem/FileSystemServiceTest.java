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
    void readFileRejectsBlockedSensitivePathInsideAuthorizedDirectory() throws Exception {
        Files.createDirectories(authorizedDirectory.resolve(".ssh"));
        Files.writeString(authorizedDirectory.resolve(".ssh/id_rsa"), "private-key", StandardCharsets.UTF_8);
        FileSystemService service = new FileSystemService(new PathSandbox(authorizedDirectory));

        assertThrows(SecurityException.class, () -> service.readFile(".ssh/id_rsa"));
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

    @Test
    void writeFileCreatesNewFileInsideAuthorizedDirectory() throws Exception {
        FileSystemService service = new FileSystemService(new PathSandbox(authorizedDirectory));

        var result = service.writeFile("docs/note.txt", "hello\nworld\n");

        assertEquals("hello\nworld\n", Files.readString(authorizedDirectory.resolve("docs/note.txt"), StandardCharsets.UTF_8));
        assertEquals(Path.of("docs", "note.txt").toString(), result.path());
        assertEquals(null, result.backupPath());
        assertEquals(2, result.diffSummary().addedLines());
    }

    @Test
    void writeFileBacksUpExistingFileAndKeepsLastThreeVersions() throws Exception {
        PathSandbox sandbox = new PathSandbox(authorizedDirectory);
        FileSystemService service = new FileSystemService(sandbox);
        BackupService backupService = new BackupService(sandbox);
        Files.writeString(authorizedDirectory.resolve("note.txt"), "version-0\n", StandardCharsets.UTF_8);

        for (int index = 1; index <= 5; index++) {
            service.writeFile("note.txt", "version-" + index + "\n");
        }

        assertEquals("version-5\n", Files.readString(authorizedDirectory.resolve("note.txt"), StandardCharsets.UTF_8));
        var backups = backupService.listBackups("note.txt");
        assertEquals(3, backups.size());
        assertTrue(backups.stream().anyMatch(path -> {
            try {
                return Files.readString(path, StandardCharsets.UTF_8).equals("version-4\n");
            } catch (Exception exception) {
                return false;
            }
        }));
    }

    @Test
    void writeFileRejectsPathOutsideAuthorizedDirectory() {
        FileSystemService service = new FileSystemService(new PathSandbox(authorizedDirectory));

        assertThrows(SecurityException.class, () -> service.writeFile("../outside.txt", "nope"));
    }

    @Test
    void writeFileRejectsBlockedSensitivePathInsideAuthorizedDirectory() {
        FileSystemService service = new FileSystemService(new PathSandbox(authorizedDirectory));

        assertThrows(SecurityException.class, () -> service.writeFile(".gnupg/private.key", "nope"));
    }

    @Test
    void applyPatchUpdatesExistingFileAndCreatesBackup() throws Exception {
        Files.writeString(authorizedDirectory.resolve("note.txt"), "alpha\nbeta\ngamma\n", StandardCharsets.UTF_8);
        FileSystemService service = new FileSystemService(new PathSandbox(authorizedDirectory));
        String patch = """
                --- a/note.txt
                +++ b/note.txt
                @@ -1,3 +1,4 @@
                 alpha
                -beta
                +bravo
                +charlie
                 gamma
                """;

        var result = service.applyPatch("note.txt", patch);

        assertEquals("alpha\r\nbravo\r\ncharlie\r\ngamma\r\n",
                Files.readString(authorizedDirectory.resolve("note.txt"), StandardCharsets.UTF_8));
        assertTrue(result.backupPath() != null && Files.exists(Path.of(result.backupPath())));
        assertEquals(2, result.diffSummary().addedLines());
        assertEquals(1, result.diffSummary().removedLines());
    }

    @Test
    void applyPatchRejectsBlockedSensitivePathInsideAuthorizedDirectory() throws Exception {
        Files.createDirectories(authorizedDirectory.resolve(".ssh"));
        Files.writeString(authorizedDirectory.resolve(".ssh/config"), "Host *\n", StandardCharsets.UTF_8);
        FileSystemService service = new FileSystemService(new PathSandbox(authorizedDirectory));

        assertThrows(SecurityException.class, () -> service.applyPatch(".ssh/config", """
                --- a/.ssh/config
                +++ b/.ssh/config
                @@ -1,1 +1,1 @@
                -Host *
                +Host example
                """));
    }
}
