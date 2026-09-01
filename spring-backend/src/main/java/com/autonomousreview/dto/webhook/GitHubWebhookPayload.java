package com.autonomousreview.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubWebhookPayload(
        String action,
        Integer number,
        @JsonProperty("pull_request")
        PullRequestInfo pullRequest,
        RepositoryInfo repository,
        SenderInfo sender
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequestInfo(
            Long id,
            Integer number,
            String title,
            String state,
            @JsonProperty("html_url")
            String htmlUrl,
            UserInfo user,
            CommitRef head,
            CommitRef base
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RepositoryInfo(
            Long id,
            String name,
            @JsonProperty("full_name")
            String fullName,
            @JsonProperty("html_url")
            String htmlUrl,
            @JsonProperty("private")
            boolean isPrivate,
            OwnerInfo owner
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OwnerInfo(
            String login,
            @JsonProperty("avatar_url")
            String avatarUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserInfo(
            String login,
            @JsonProperty("avatar_url")
            String avatarUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommitRef(
            String sha,
            String ref,
            @JsonProperty("html_url")
            String htmlUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SenderInfo(
            String login,
            @JsonProperty("avatar_url")
            String avatarUrl
    ) {}
}
