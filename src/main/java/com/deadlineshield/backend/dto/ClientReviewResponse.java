package com.deadlineshield.backend.dto;

import java.time.Instant;

public record ClientReviewResponse(
        String projectTitle,
        String clientName,
        Instant originalTargetDate,
        Instant adjustedTargetDate,
        String status,
        Instant pausedAt) {
}
