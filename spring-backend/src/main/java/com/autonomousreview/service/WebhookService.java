package com.autonomousreview.service;

import com.autonomousreview.dto.webhook.GitHubWebhookPayload;
import com.autonomousreview.dto.webhook.WebhookResponse;
import com.autonomousreview.exception.GitHubApiException;
import com.autonomousreview.model.Repository;
import com.autonomousreview.model.ReviewJob;
import com.autonomousreview.model.ReviewStatus;
import com.autonomousreview.repository.RepositoryEntityRepository;
import com.autonomousreview.repository.ReviewJobRepository;
import com.autonomousreview.security.HmacSignatureValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class WebhookService {

    private static final Set<String> SUPPORTED_PR_ACTIONS = Set.of("opened", "synchronize", "reopened");

    private final HmacSignatureValidator signatureValidator;
    private final ReviewJobRepository reviewJobRepository;
    private final RepositoryEntityRepository repositoryRepository;
    private final ObjectMapper objectMapper;

    public WebhookService(
            HmacSignatureValidator signatureValidator,
            ReviewJobRepository reviewJobRepository,
            RepositoryEntityRepository repositoryRepository,
            ObjectMapper objectMapper
    ) {
        this.signatureValidator = signatureValidator;
        this.reviewJobRepository = reviewJobRepository;
        this.repositoryRepository = repositoryRepository;
        this.objectMapper = objectMapper;
    }

    public WebhookResponse processGitHubWebhook(String rawPayload, String signatureHeader, String eventType, String deliveryId) {
        // 1. Cryptographic HMAC-SHA256 Signature Verification
        if (!signatureValidator.isValidSignature(rawPayload, signatureHeader)) {
            throw new GitHubApiException("Invalid or missing HMAC-SHA256 signature");
        }

        // 2. Handle GitHub ping event
        if ("ping".equalsIgnoreCase(eventType)) {
            return WebhookResponse.ignored("GitHub ping event acknowledged successfully");
        }

        // 3. Event Type Filter: Only process pull_request events
        if (!"pull_request".equalsIgnoreCase(eventType)) {
            return WebhookResponse.ignored("Event type '" + eventType + "' is not supported");
        }

        // 4. Parse Webhook Payload
        GitHubWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawPayload, GitHubWebhookPayload.class);
        } catch (Exception e) {
            throw new GitHubApiException("Failed to parse GitHub webhook JSON payload: " + e.getMessage(), e);
        }

        if (payload.pullRequest() == null || payload.repository() == null) {
            return WebhookResponse.ignored("Malformed pull request payload received");
        }

        String action = payload.action() != null ? payload.action().toLowerCase() : "";
        if (!SUPPORTED_PR_ACTIONS.contains(action)) {
            return WebhookResponse.ignored("Pull request action '" + action + "' is ignored");
        }

        String repoFullName = payload.repository().fullName();
        Integer pullNumber = payload.pullRequest().number() != null ? payload.pullRequest().number() : payload.number();
        String commitSha = payload.pullRequest().head() != null ? payload.pullRequest().head().sha() : null;
        String baseSha = payload.pullRequest().base() != null ? payload.pullRequest().base().sha() : null;
        String prTitle = payload.pullRequest().title();
        String prAuthor = payload.pullRequest().user() != null ? payload.pullRequest().user().login() : "unknown";
        String prUrl = payload.pullRequest().htmlUrl();

        if (commitSha == null || pullNumber == null || repoFullName == null) {
            return WebhookResponse.ignored("Pull request payload missing required identifiers (commitSha, pullNumber, or repository)");
        }

        // 5. Repository Check (Ensure repository is registered and enabled for review)
        Optional<Repository> repoOpt = repositoryRepository.findByFullName(repoFullName);
        String repositoryId = repoOpt.map(Repository::getId).orElse(null);

        // 6. Idempotency & Deduplication: Check if review job already exists for this commit SHA
        String deduplicationKey = ReviewJob.buildDeduplicationKey(repoFullName, pullNumber, commitSha);
        Optional<ReviewJob> existingJobOpt = reviewJobRepository.findByDeduplicationKey(deduplicationKey);

        if (existingJobOpt.isPresent()) {
            ReviewJob existingJob = existingJobOpt.get();
            return WebhookResponse.duplicate(
                    existingJob.getId(),
                    existingJob.getStatus(),
                    "Review job already exists for commit " + commitSha + " in PR #" + pullNumber
            );
        }

        // 7. Create & Persist ReviewJob in QUEUED state
        ReviewJob job = new ReviewJob(
                repositoryId,
                repoFullName,
                pullNumber,
                commitSha,
                baseSha,
                prTitle,
                prAuthor,
                prUrl,
                action
        );

        ReviewJob savedJob = reviewJobRepository.save(job);

        return WebhookResponse.accepted(
                savedJob.getId(),
                savedJob.getStatus(),
                "Review job successfully queued for PR #" + pullNumber + " at commit " + commitSha
        );
    }
}
