package com.deadlineshield.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ClientApprovalRequest(
        @NotBlank String feedback) {
}
