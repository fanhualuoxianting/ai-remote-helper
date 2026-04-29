package com.airh.safety;

import com.airh.protocol.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensitiveFileProtectorTest {
    private final SensitiveFileProtector protector = new SensitiveFileProtector();

    @Test
    void blocksSshAndGpgDirectories() {
        assertEquals(RiskLevel.BLOCKED, protector.checkPath("~/.ssh/id_rsa"));
        assertEquals(RiskLevel.BLOCKED, protector.checkPath("/home/user/.gnupg/private-keys-v1.d/key"));
    }

    @Test
    void blocksBrowserProfiles() {
        assertEquals(RiskLevel.BLOCKED,
                protector.checkPath("C:\\Users\\dev\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\Cookies"));
        assertEquals(RiskLevel.BLOCKED,
                protector.checkPath("C:\\Users\\dev\\AppData\\Roaming\\Mozilla\\Firefox\\Profiles\\abc.default\\logins.json"));
        assertEquals(RiskLevel.BLOCKED,
                protector.checkPath("C:\\Users\\dev\\AppData\\Local\\Microsoft\\Edge\\User Data\\Default\\Login Data"));
    }

    @Test
    void blocksSystemCredentialFiles() {
        assertEquals(RiskLevel.BLOCKED, protector.checkPath("/etc/shadow"));
        assertEquals(RiskLevel.BLOCKED, protector.checkPath("/etc/passwd"));
        assertEquals(RiskLevel.BLOCKED,
                protector.checkPath("C:\\Windows\\System32\\config\\SAM"));
    }

    @Test
    void marksShellStartupHostsAndWindowsStartupAsHighRisk() {
        assertEquals(RiskLevel.HIGH, protector.checkPath("~/.bashrc"));
        assertEquals(RiskLevel.HIGH, protector.checkPath("/home/user/.zshrc"));
        assertEquals(RiskLevel.HIGH, protector.checkPath("/etc/hosts"));
        assertEquals(RiskLevel.HIGH,
                protector.checkPath("C:\\Users\\dev\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\helper.bat"));
    }

    @Test
    void allowsNormalProjectFiles() {
        assertEquals(RiskLevel.LOW, protector.checkPath("src/main/java/App.java"));
        assertEquals(RiskLevel.LOW, protector.checkPath(Path.of("docs", "note.md")));
        assertEquals(RiskLevel.LOW, protector.checkPath(""));
        assertEquals(RiskLevel.LOW, protector.checkPath((String) null));
    }
}
