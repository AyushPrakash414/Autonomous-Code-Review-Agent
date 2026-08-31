package com.autonomousreview.controller;

import com.autonomousreview.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Value("${spring.application.name:autonomous-code-review-control-plane}")
    private String applicationName;

    @GetMapping
    public ResponseEntity<HealthResponse> checkHealth() {
        HealthResponse response = HealthResponse.up(
                applicationName,
                "1.0.0",
                Map.of(
                        "environment", "production-ready",
                        "control_plane", "active"
                )
        );
        return ResponseEntity.ok(response);
    }
}
