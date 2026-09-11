package com.ledger.stock_ledger.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelMovementRequest(
        @NotBlank @Size(max = 255) String recordedBy,
        @NotBlank @Size(max = 1000) String reason
) {
}
