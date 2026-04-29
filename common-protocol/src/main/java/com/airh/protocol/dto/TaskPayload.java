package com.airh.protocol.dto;

import java.util.Map;

public record TaskPayload(
        Map<String, Object> data
) {
}
