package com.airh.agent.executor;

public record CommandResult(
        int exitCode,
        String stdout,
        String stderr,
        long durationMs,
        boolean timedOut,
        boolean killed
) {
}
