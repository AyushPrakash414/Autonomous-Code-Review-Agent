package com.autonomousreview.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubUserApiResponse(
        Long id,
        String login,
        @JsonProperty("avatar_url")
        String avatarUrl,
        String name,
        String email
) {}
