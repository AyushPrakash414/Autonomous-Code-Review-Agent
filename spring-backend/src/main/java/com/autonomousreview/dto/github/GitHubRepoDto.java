package com.autonomousreview.dto.github;

import com.autonomousreview.model.Repository;

import java.time.Instant;

public record GitHubRepoDto(
        String id,
        Long githubRepoId,
        String name,
        String fullName,
        String owner,
        String htmlUrl,
        String defaultBranch,
        boolean isPrivate,
        boolean enabledForReview,
        Instant connectedAt
) {
    public static GitHubRepoDto fromEntity(Repository repository) {
        return new GitHubRepoDto(
                repository.getId(),
                repository.getGithubRepoId(),
                repository.getName(),
                repository.getFullName(),
                repository.getOwner(),
                repository.getHtmlUrl(),
                repository.getDefaultBranch(),
                repository.isPrivate(),
                repository.isEnabledForReview(),
                repository.getConnectedAt()
        );
    }
}
