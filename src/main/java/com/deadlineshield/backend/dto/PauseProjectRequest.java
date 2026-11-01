package com.deadlineshield.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PauseProjectRequest(
        @NotNull UUID milestoneId,
        @NotBlank
        @Size(max = 500)
        @Pattern(regexp = "^[^\\p{Cc}\\p{Cf}]*$")
        String reason) {
}
