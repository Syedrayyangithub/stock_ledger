package com.ledger.stock_ledger.api.dto;

import com.ledger.stock_ledger.domain.MovementKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateMovementRequest(
        @NotNull MovementKind kind,
        @NotNull UUID itemId,
        @NotNull @DecimalMin(value = "0.000001", inclusive = true) @Digits(integer = 13, fraction = 6)
        BigDecimal quantity,
        UUID warehouseId,
        UUID fromWarehouseId,
        UUID toWarehouseId,
        @NotBlank @Size(max = 1000) String reason,
        @NotNull Instant occurredAt,
        @NotBlank @Size(max = 255) String recordedBy
) {
}
