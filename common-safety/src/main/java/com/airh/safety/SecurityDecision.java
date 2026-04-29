package com.airh.safety;

import com.airh.protocol.enums.RiskLevel;

/**
 * 安全检查决策结果
 */
public class SecurityDecision {
    public enum Decision {
        ALLOWED,
        BLOCKED,
        WARN
    }

    private final Decision decision;
    private final RiskLevel riskLevel;
    private final String reason;
    private final String normalizedPath;

    public SecurityDecision(Decision decision, RiskLevel riskLevel, String reason, String normalizedPath) {
        this.decision = decision;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.normalizedPath = normalizedPath;
    }

    public static SecurityDecision allowed(String normalizedPath) {
        return new SecurityDecision(Decision.ALLOWED, RiskLevel.LOW, "OK", normalizedPath);
    }

    public static SecurityDecision warn(String normalizedPath, String reason) {
        return new SecurityDecision(Decision.WARN, RiskLevel.MEDIUM, reason, normalizedPath);
    }

    public static SecurityDecision blocked(String reason) {
        return new SecurityDecision(Decision.BLOCKED, RiskLevel.HIGH, reason, null);
    }

    public boolean isAllowed() { return decision == Decision.ALLOWED || decision == Decision.WARN; }
    public boolean isBlocked() { return decision == Decision.BLOCKED; }
    public Decision getDecision() { return decision; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public String getReason() { return reason; }
    public String getNormalizedPath() { return normalizedPath; }
}
