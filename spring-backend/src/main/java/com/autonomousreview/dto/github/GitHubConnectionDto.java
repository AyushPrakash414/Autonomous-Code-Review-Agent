package com.autonomousreview.dto.github;

import com.autonomousreview.model.GitHubConnection;

import java.time.Instant;
import java.util.List;

public record GitHubConnectionDto(
        String id,
        Long githubUserId,
        String githubUsername,
        String avatarUrl,
        List<String> scopes,
        Instant connectedAt
) {
    public static GitHubConnectionDto fromEntity(GitHubConnection connection) {
        return new GitHubConnectionDto(
                connection.getId(),
                connection.getGithubUserId(),
                connection.getGithubUsername(),
                connection.getAvatarUrl(),
                connection.getScopes(),
                connection.getConnectedAt()
        );
    }
}
