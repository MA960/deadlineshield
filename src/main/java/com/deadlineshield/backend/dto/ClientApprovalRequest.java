package com.deadlineshield.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClientApprovalRequest(
        @NotBlank
        @Size(max = 2000)
        @Pattern(regexp = "^[^\\p{Cc}\\p{Cf}]*$")
        String feedback) {
}
