package com.airh.safety;

import com.airh.protocol.enums.RiskLevel;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CommandRiskDetector {
    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
            Pattern.compile("(?i)(^|[;&|\\s])rm\\s+-[a-z]*r[a-z]*f[a-z]*\\s+/(\\s|$|[*])"),
            Pattern.compile("(?i)(^|[;&|\\s])rm\\s+-[a-z]*f[a-z]*r[a-z]*\\s+/(\\s|$|[*])"),
            Pattern.compile("(?i)(^|[;&|\\s])format(\\s|$|[.:])"),
            Pattern.compile("(?i)(^|[;&|\\s])del\\s+([^;&|]*\\s)?/f(\\s+[^;&|]*)?/s(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])del\\s+([^;&|]*\\s)?/s(\\s+[^;&|]*)?/f(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])reg(istry)?\\s+delete(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])shutdown(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])mkfs(\\.[a-z0-9]+)?(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])dd\\s+[^;&|]*\\bif=[^;&|]*\\s+[^;&|]*\\bof=/dev/[^\\s;&|]+"),
            Pattern.compile("(?i)(^|[;&|\\s])dd\\s+[^;&|]*\\bof=/dev/[^\\s;&|]+\\s+[^;&|]*\\bif=[^;&|]*"),
            Pattern.compile("(?i)>\\s*/dev/(sd[a-z]|hd[a-z]|nvme\\d+n\\d+|mmcblk\\d+)\\b"),
            Pattern.compile("(?i)(curl|wget)\\b[^;&|]*\\|\\s*(sh|bash|zsh|fish)\\b"),
            Pattern.compile("(?i)powershell\\s+-(enc|encodedcommand)\\b"),
            Pattern.compile("(?i)rd\\s+/s\\s+/q(\\s|$)"),
            Pattern.compile("(?i)net\\s+user\\b")
    );

    private static final List<Pattern> HIGH_PATTERNS = List.of(
            Pattern.compile("(?i)(^|[;&|\\s])sudo(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])su\\s+-"),
            Pattern.compile("(?i)(^|[;&|\\s])chmod\\s+777(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])chown(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])rm\\s+-[a-z]*r[a-z]*f[a-z]*(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])rm\\s+-[a-z]*f[a-z]*r[a-z]*(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])netsh(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])sc\\s+(stop|delete)(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])taskkill\\s+[^;&|]*(/f|-f)\\b")
    );

    private static final List<Pattern> MEDIUM_PATTERNS = List.of(
            Pattern.compile("(?i)(^|[;&|\\s])rm(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])del(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])kill(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])service\\s+\\S+\\s+stop(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])npm\\s+install(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])pip\\s+install(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])docker\\s+compose\\s+up(\\s|$)"),
            Pattern.compile("(?i)(^|[;&|\\s])git\\s+pull(\\s|$)")
    );

    public RiskLevel detect(String command) {
        if (command == null || command.isBlank()) {
            return RiskLevel.LOW;
        }

        String normalized = command.toLowerCase(Locale.ROOT)
                .replace('\\', '/')
                .replaceAll("\\s+", " ")
                .trim();
        if (matchesAny(BLOCKED_PATTERNS, normalized)) {
            return RiskLevel.BLOCKED;
        }
        if (matchesAny(HIGH_PATTERNS, normalized)) {
            return RiskLevel.HIGH;
        }
        if (matchesAny(MEDIUM_PATTERNS, normalized)) {
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    private boolean matchesAny(List<Pattern> patterns, String command) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(command).find());
    }
}
