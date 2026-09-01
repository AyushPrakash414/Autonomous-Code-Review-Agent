package com.autonomousreview.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepoApiResponse(
        Long id,
        String name,
        @JsonProperty("full_name")
        String fullName,
        @JsonProperty("html_url")
        String htmlUrl,
        @JsonProperty("default_branch")
        String defaultBranch,
        @JsonProperty("private")
        boolean isPrivate,
        GitHubOwnerApiResponse owner
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubOwnerApiResponse(
            String login,
            @JsonProperty("avatar_url")
            String avatarUrl
    ) {}
}
