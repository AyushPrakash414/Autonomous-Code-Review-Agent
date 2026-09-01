package com.autonomousreview.service;

import com.autonomousreview.client.GitHubClient;
import com.autonomousreview.dto.github.GitHubConnectionDto;
import com.autonomousreview.dto.github.GitHubRepoApiResponse;
import com.autonomousreview.dto.github.GitHubRepoDto;
import com.autonomousreview.dto.github.GitHubUserApiResponse;
import com.autonomousreview.exception.GitHubApiException;
import com.autonomousreview.exception.GitHubConnectionNotFoundException;
import com.autonomousreview.model.GitHubConnection;
import com.autonomousreview.model.Repository;
import com.autonomousreview.repository.GitHubConnectionRepository;
import com.autonomousreview.repository.RepositoryEntityRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GitHubService {

    private final GitHubClient gitHubClient;
    private final GitHubConnectionRepository connectionRepository;
    private final RepositoryEntityRepository repositoryRepository;

    public GitHubService(
            GitHubClient gitHubClient,
            GitHubConnectionRepository connectionRepository,
            RepositoryEntityRepository repositoryRepository
    ) {
        this.gitHubClient = gitHubClient;
        this.connectionRepository = connectionRepository;
        this.repositoryRepository = repositoryRepository;
    }

    public GitHubConnectionDto connectAccount(String userId, String accessToken) {
        // Validate token with GitHub API
        GitHubUserApiResponse ghUser = gitHubClient.getCurrentUser(accessToken);

        // Save or update GitHub connection
        GitHubConnection connection = connectionRepository.findByUserId(userId)
                .orElse(new GitHubConnection());

        connection.setUserId(userId);
        connection.setGithubUserId(ghUser.id());
        connection.setGithubUsername(ghUser.login());
        connection.setAccessToken(accessToken);
        connection.setAvatarUrl(ghUser.avatarUrl());
        connection.setScopes(List.of("repo", "read:user"));
        connection.setUpdatedAt(Instant.now());

        GitHubConnection savedConnection = connectionRepository.save(connection);

        // Synchronize user repositories on connect
        syncRepositories(userId, accessToken);

        return GitHubConnectionDto.fromEntity(savedConnection);
    }

    public GitHubConnectionDto getConnection(String userId) {
        return connectionRepository.findByUserId(userId)
                .map(GitHubConnectionDto::fromEntity)
                .orElseThrow(() -> new GitHubConnectionNotFoundException("No connected GitHub account found for user"));
    }

    public List<GitHubRepoDto> getUserRepositories(String userId) {
        GitHubConnection connection = connectionRepository.findByUserId(userId)
                .orElseThrow(() -> new GitHubConnectionNotFoundException("Please connect your GitHub account first"));

        return syncRepositories(userId, connection.getAccessToken());
    }

    public List<GitHubRepoDto> syncRepositories(String userId, String accessToken) {
        List<GitHubRepoApiResponse> remoteRepos = gitHubClient.getUserRepositories(accessToken);
        List<GitHubRepoDto> result = new ArrayList<>();

        for (GitHubRepoApiResponse remote : remoteRepos) {
            Optional<Repository> existing = repositoryRepository.findByUserIdAndGithubRepoId(userId, remote.id());
            Repository repo = existing.orElse(new Repository());

            repo.setUserId(userId);
            repo.setGithubRepoId(remote.id());
            repo.setName(remote.name());
            repo.setFullName(remote.fullName());
            repo.setOwner(remote.owner() != null ? remote.owner().login() : "");
            repo.setHtmlUrl(remote.htmlUrl());
            repo.setDefaultBranch(remote.defaultBranch());
            repo.setPrivate(remote.isPrivate());
            repo.setUpdatedAt(Instant.now());

            Repository saved = repositoryRepository.save(repo);
            result.add(GitHubRepoDto.fromEntity(saved));
        }

        return result;
    }

    public GitHubRepoDto enableRepository(String userId, Long githubRepoId) {
        Repository repository = repositoryRepository.findByUserIdAndGithubRepoId(userId, githubRepoId)
                .orElseThrow(() -> new GitHubApiException("Repository with GitHub ID " + githubRepoId + " not found for current user"));

        repository.setEnabledForReview(true);
        repository.setUpdatedAt(Instant.now());
        Repository saved = repositoryRepository.save(repository);

        return GitHubRepoDto.fromEntity(saved);
    }

    public List<GitHubRepoDto> getEnabledRepositories(String userId) {
        return repositoryRepository.findByUserIdAndEnabledForReviewTrue(userId)
                .stream()
                .map(GitHubRepoDto::fromEntity)
                .toList();
    }
}
