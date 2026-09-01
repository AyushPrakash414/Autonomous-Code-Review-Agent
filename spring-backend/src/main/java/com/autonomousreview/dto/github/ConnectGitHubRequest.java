package com.autonomousreview.dto.github;

import jakarta.validation.constraints.NotBlank;

public record ConnectGitHubRequest(
        @NotBlank(message = "GitHub access token is required")
        String accessToken
) {}
