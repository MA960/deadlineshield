package com.deadlineshield.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PauseProjectRequest(
        @NotNull UUID milestoneId,
        @NotBlank String reason) {
}
