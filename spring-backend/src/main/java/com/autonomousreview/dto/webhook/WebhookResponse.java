package com.autonomousreview.dto.webhook;

import com.autonomousreview.model.ReviewStatus;

public record WebhookResponse(
        String status,
        String message,
        String jobId,
        ReviewStatus reviewStatus
) {
    public static WebhookResponse accepted(String jobId, ReviewStatus reviewStatus, String message) {
        return new WebhookResponse("ACCEPTED", message, jobId, reviewStatus);
    }

    public static WebhookResponse ignored(String message) {
        return new WebhookResponse("IGNORED", message, null, null);
    }

    public static WebhookResponse duplicate(String jobId, ReviewStatus reviewStatus, String message) {
        return new WebhookResponse("DUPLICATE", message, jobId, reviewStatus);
    }

    public static WebhookResponse rejected(String message) {
        return new WebhookResponse("REJECTED", message, null, null);
    }
}
