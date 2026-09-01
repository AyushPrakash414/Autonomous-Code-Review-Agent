package com.autonomousreview.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "repositories")
@CompoundIndex(name = "user_repo_idx", def = "{'userId': 1, 'githubRepoId': 1}", unique = true)
public class Repository {

    @Id
    private String id;

    @Indexed
    private String userId;

    private Long githubRepoId;

    private String name;

    @Indexed
    private String fullName;

    private String owner;

    private String htmlUrl;

    private String defaultBranch = "main";

    private boolean isPrivate = false;

    private boolean enabledForReview = false;

    private String webhookId;

    @CreatedDate
    private Instant connectedAt = Instant.now();

    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    public Repository() {
    }

    public Repository(String userId, Long githubRepoId, String name, String fullName, String owner, String htmlUrl, String defaultBranch, boolean isPrivate) {
        this.userId = userId;
        this.githubRepoId = githubRepoId;
        this.name = name;
        this.fullName = fullName;
        this.owner = owner;
        this.htmlUrl = htmlUrl;
        this.defaultBranch = defaultBranch != null ? defaultBranch : "main";
        this.isPrivate = isPrivate;
        this.enabledForReview = false;
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

    public Long getGithubRepoId() {
        return githubRepoId;
    }

    public void setGithubRepoId(Long githubRepoId) {
        this.githubRepoId = githubRepoId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public boolean isEnabledForReview() {
        return enabledForReview;
    }

    public void setEnabledForReview(boolean enabledForReview) {
        this.enabledForReview = enabledForReview;
    }

    public String getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
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
