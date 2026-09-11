package com.ledger.stock_ledger.api.dto;

import com.ledger.stock_ledger.domain.Movement;
import com.ledger.stock_ledger.domain.MovementKind;
import com.ledger.stock_ledger.domain.Unit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MovementResponse(
        UUID id,
        MovementKind kind,
        UUID itemId,
        String itemCode,
        String itemName,
        Unit itemUnit,
        BigDecimal quantity,
        String reason,
        Instant occurredAt,
        Instant recordedAt,
        String recordedBy,
        UUID cancelsMovementId,
        UUID cancelledByMovementId,
        List<MovementLineResponse> lines
) {
    public static MovementResponse from(Movement movement, UUID cancelledByMovementId) {
        List<MovementLineResponse> lines = movement.getLines().stream()
                .map(line -> new MovementLineResponse(
                        line.getWarehouse().getId(),
                        line.getWarehouse().getCode(),
                        line.getWarehouse().getName(),
                        Quantities.normalize(line.getQuantityDelta())
                ))
                .toList();
        return new MovementResponse(
                movement.getId(),
                movement.getKind(),
                movement.getItem().getId(),
                movement.getItemCodeSnapshot(),
                movement.getItemNameSnapshot(),
                movement.getItemUnitSnapshot(),
                Quantities.normalize(movement.getQuantity()),
                movement.getReason(),
                movement.getOccurredAt(),
                movement.getRecordedAt(),
                movement.getRecordedBy(),
                movement.getCancelsMovementId(),
                cancelledByMovementId,
                lines
        );
    }
}
