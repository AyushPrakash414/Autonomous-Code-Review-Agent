package com.autonomousreview.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "review_jobs")
public class ReviewJob {

    @Id
    private String id;

    @Indexed
    private String repositoryId;

    @Indexed
    private String repoFullName;

    private Integer pullNumber;

    @Indexed
    private String commitSha;

    private String baseSha;

    private String prTitle;

    private String prAuthor;

    private String prUrl;

    private String action;

    private ReviewStatus status = ReviewStatus.QUEUED;

    private String statusDetails;

    private Integer riskScore;

    @Indexed(unique = true)
    private String deduplicationKey;

    @CreatedDate
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    public ReviewJob() {
    }

    public ReviewJob(String repositoryId, String repoFullName, Integer pullNumber, String commitSha, String baseSha, String prTitle, String prAuthor, String prUrl, String action) {
        this.repositoryId = repositoryId;
        this.repoFullName = repoFullName;
        this.pullNumber = pullNumber;
        this.commitSha = commitSha;
        this.baseSha = baseSha;
        this.prTitle = prTitle;
        this.prAuthor = prAuthor;
        this.prUrl = prUrl;
        this.action = action;
        this.status = ReviewStatus.QUEUED;
        this.deduplicationKey = buildDeduplicationKey(repoFullName, pullNumber, commitSha);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static String buildDeduplicationKey(String repoFullName, Integer pullNumber, String commitSha) {
        return (repoFullName != null ? repoFullName.toLowerCase() : "unknown") + ":" + pullNumber + ":" + commitSha;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepoFullName() {
        return repoFullName;
    }

    public void setRepoFullName(String repoFullName) {
        this.repoFullName = repoFullName;
    }

    public Integer getPullNumber() {
        return pullNumber;
    }

    public void setPullNumber(Integer pullNumber) {
        this.pullNumber = pullNumber;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getBaseSha() {
        return baseSha;
    }

    public void setBaseSha(String baseSha) {
        this.baseSha = baseSha;
    }

    public String getPrTitle() {
        return prTitle;
    }

    public void setPrTitle(String prTitle) {
        this.prTitle = prTitle;
    }

    public String getPrAuthor() {
        return prAuthor;
    }

    public void setPrAuthor(String prAuthor) {
        this.prAuthor = prAuthor;
    }

    public String getPrUrl() {
        return prUrl;
    }

    public void setPrUrl(String prUrl) {
        this.prUrl = prUrl;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getStatusDetails() {
        return statusDetails;
    }

    public void setStatusDetails(String statusDetails) {
        this.statusDetails = statusDetails;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }

    public void setDeduplicationKey(String deduplicationKey) {
        this.deduplicationKey = deduplicationKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
