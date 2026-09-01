package com.autonomousreview.controller;

import com.autonomousreview.config.TestRepositoryMockConfig;
import com.autonomousreview.model.Repository;
import com.autonomousreview.model.ReviewJob;
import com.autonomousreview.model.ReviewStatus;
import com.autonomousreview.repository.RepositoryEntityRepository;
import com.autonomousreview.repository.ReviewJobRepository;
import com.autonomousreview.security.HmacSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRepositoryMockConfig.class)
class WebhookControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HmacSignatureValidator signatureValidator;

    @Autowired
    private ReviewJobRepository reviewJobRepository;

    @Autowired
    private RepositoryEntityRepository repositoryRepository;

    private String validPayload;
    private String validSignature;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.reset(reviewJobRepository, repositoryRepository);

        validPayload = """
                {
                  "action": "opened",
                  "number": 42,
                  "pull_request": {
                    "id": 1001,
                    "number": 42,
                    "title": "feat: Add payment authorization check",
                    "state": "open",
                    "html_url": "https://github.com/octocat/demo-repo/pull/42",
                    "user": {
                      "login": "octocat"
                    },
                    "head": {
                      "sha": "a1b2c3d4e5f67890123456789abcdef012345678",
                      "ref": "feature-branch"
                    },
                    "base": {
                      "sha": "0000000000000000000000000000000000000000",
                      "ref": "main"
                    }
                  },
                  "repository": {
                    "id": 9999,
                    "name": "demo-repo",
                    "full_name": "octocat/demo-repo",
                    "html_url": "https://github.com/octocat/demo-repo",
                    "private": false
                  }
                }
                """;

        validSignature = signatureValidator.computeSignature(validPayload);
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/github creates QUEUED ReviewJob for valid signature and PR opened event")
    void testValidWebhookOpenedEventQueuesReviewJob() throws Exception {
        Repository repo = new Repository("user-1", 9999L, "demo-repo", "octocat/demo-repo", "octocat", "https://github.com/octocat/demo-repo", "main", false);
        repo.setId("repo-db-id-1");
        repo.setEnabledForReview(true);
        when(repositoryRepository.findByFullName("octocat/demo-repo")).thenReturn(Optional.of(repo));

        String deduplicationKey = ReviewJob.buildDeduplicationKey("octocat/demo-repo", 42, "a1b2c3d4e5f67890123456789abcdef012345678");
        when(reviewJobRepository.findByDeduplicationKey(deduplicationKey)).thenReturn(Optional.empty());

        ReviewJob savedJob = new ReviewJob("repo-db-id-1", "octocat/demo-repo", 42, "a1b2c3d4e5f67890123456789abcdef012345678", "0000000000000000000000000000000000000000", "feat: Add payment authorization check", "octocat", "https://github.com/octocat/demo-repo/pull/42", "opened");
        savedJob.setId("job-uuid-101");
        savedJob.setStatus(ReviewStatus.QUEUED);
        when(reviewJobRepository.save(any(ReviewJob.class))).thenReturn(savedJob);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", validSignature)
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "delivery-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.jobId").value("job-uuid-101"))
                .andExpect(jsonPath("$.reviewStatus").value("QUEUED"));
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/github rejects invalid HMAC signature with 400/401 (Negative Test)")
    void testInvalidHmacSignatureRejected() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", "sha256=invalidSignature123456789abcdef")
                        .header("X-GitHub-Event", "pull_request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("GitHub API Error"))
                .andExpect(jsonPath("$.message").value("Invalid or missing HMAC-SHA256 signature"));

        verify(reviewJobRepository, never()).save(any(ReviewJob.class));
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/github rejects missing signature header (Negative Test)")
    void testMissingSignatureRejected() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-GitHub-Event", "pull_request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("GitHub API Error"))
                .andExpect(jsonPath("$.message").value("Invalid or missing HMAC-SHA256 signature"));

        verify(reviewJobRepository, never()).save(any(ReviewJob.class));
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/github ignores ping event gracefully")
    void testPingEventIgnored() throws Exception {
        String pingPayload = "{\"zen\":\"Design for failure.\",\"hook_id\":12345}";
        String pingSignature = signatureValidator.computeSignature(pingPayload);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", pingSignature)
                        .header("X-GitHub-Event", "ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pingPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED"))
                .andExpect(jsonPath("$.message").value("GitHub ping event acknowledged successfully"));

        verify(reviewJobRepository, never()).save(any(ReviewJob.class));
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/github ignores unsupported event type (e.g. issues)")
    void testUnsupportedEventTypeIgnored() throws Exception {
        String issuesPayload = "{\"action\":\"opened\",\"issue\":{\"number\":1}}";
        String issuesSignature = signatureValidator.computeSignature(issuesPayload);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", issuesSignature)
                        .header("X-GitHub-Event", "issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issuesPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED"))
                .andExpect(jsonPath("$.message").value("Event type 'issues' is not supported"));

        verify(reviewJobRepository, never()).save(any(ReviewJob.class));
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/github ignores unsupported action (e.g. pull_request.closed)")
    void testUnsupportedActionIgnored() throws Exception {
        String closedPayload = """
                {
                  "action": "closed",
                  "number": 42,
                  "pull_request": {
                    "id": 1001,
                    "number": 42,
                    "title": "feat: Add payment authorization check",
                    "state": "closed",
                    "html_url": "https://github.com/octocat/demo-repo/pull/42",
                    "user": { "login": "octocat" },
                    "head": { "sha": "a1b2c3d4e5f67890123456789abcdef012345678", "ref": "feature" },
                    "base": { "sha": "0000000000000000000000000000000000000000", "ref": "main" }
                  },
                  "repository": {
                    "id": 9999,
                    "name": "demo-repo",
                    "full_name": "octocat/demo-repo",
                    "html_url": "https://github.com/octocat/demo-repo",
                    "private": false
                  }
                }
                """;
        String closedSignature = signatureValidator.computeSignature(closedPayload);

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", closedSignature)
                        .header("X-GitHub-Event", "pull_request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closedPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED"))
                .andExpect(jsonPath("$.message").value("Pull request action 'closed' is ignored"));

        verify(reviewJobRepository, never()).save(any(ReviewJob.class));
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/github deduplicates identical commit delivery idempotently")
    void testDuplicateWebhookDeliveryIdempotency() throws Exception {
        String deduplicationKey = ReviewJob.buildDeduplicationKey("octocat/demo-repo", 42, "a1b2c3d4e5f67890123456789abcdef012345678");

        ReviewJob existingJob = new ReviewJob("repo-db-id-1", "octocat/demo-repo", 42, "a1b2c3d4e5f67890123456789abcdef012345678", "0000000000000000000000000000000000000000", "feat: Add payment authorization check", "octocat", "https://github.com/octocat/demo-repo/pull/42", "opened");
        existingJob.setId("existing-job-id-999");
        existingJob.setStatus(ReviewStatus.QUEUED);

        when(reviewJobRepository.findByDeduplicationKey(deduplicationKey)).thenReturn(Optional.of(existingJob));

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", validSignature)
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "duplicate-delivery-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"))
                .andExpect(jsonPath("$.jobId").value("existing-job-id-999"))
                .andExpect(jsonPath("$.reviewStatus").value("QUEUED"));

        // Must never save a new duplicate job
        verify(reviewJobRepository, never()).save(any(ReviewJob.class));
    }
}
