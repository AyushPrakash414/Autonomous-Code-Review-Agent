package com.autonomousreview.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "github_connections")
public class GitHubConnection {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private Long githubUserId;

    private String githubUsername;

    private String accessToken;

    private String avatarUrl;

    private List<String> scopes;

    @CreatedDate
    private Instant connectedAt = Instant.now();

    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    public GitHubConnection() {
    }

    public GitHubConnection(String userId, Long githubUserId, String githubUsername, String accessToken, String avatarUrl, List<String> scopes) {
        this.userId = userId;
        this.githubUserId = githubUserId;
        this.githubUsername = githubUsername;
        this.accessToken = accessToken;
        this.avatarUrl = avatarUrl;
        this.scopes = scopes;
        this.connectedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getGithubUserId() {
        return githubUserId;
    }

    public void setGithubUserId(Long githubUserId) {
        this.githubUserId = githubUserId;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
