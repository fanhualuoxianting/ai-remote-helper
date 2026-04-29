package com.airh.safety;

import com.airh.protocol.enums.RiskLevel;

import java.util.List;
import java.util.Locale;

public class CommandRiskDetector {
    private static final List<String> BLOCKED_KEYWORDS = List.of(
            "format ",
            "reg add",
            "net user",
            "shutdown",
            "bcdedit",
            "takeown"
    );

    public RiskLevel detect(String command) {
        if (command == null || command.isBlank()) {
            return RiskLevel.LOW;
        }

        String normalized = command.toLowerCase(Locale.ROOT);
        boolean blocked = BLOCKED_KEYWORDS.stream().anyMatch(normalized::contains);
        if (blocked) {
            return RiskLevel.BLOCKED;
        }

        if (normalized.contains("rm ") || normalized.contains("del ")) {
            return RiskLevel.HIGH;
        }

        return RiskLevel.LOW;
    }
}
