package com.autonomousreview.dto;

import java.time.Instant;
import java.util.Map;

public record HealthResponse(
        String status,
        String service,
        String version,
        Instant timestamp,
        Map<String, Object> details
) {
    public static HealthResponse up(String service, String version, Map<String, Object> details) {
        return new HealthResponse("UP", service, version, Instant.now(), details);
    }
}
