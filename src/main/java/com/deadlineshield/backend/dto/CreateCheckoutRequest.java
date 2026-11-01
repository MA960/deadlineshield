package com.deadlineshield.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCheckoutRequest(@NotNull UUID organizationId) {
}
