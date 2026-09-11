package com.ledger.stock_ledger.api.dto;

import com.ledger.stock_ledger.domain.Warehouse;

import java.time.Instant;
import java.util.UUID;

public record WarehouseResponse(
        UUID id,
        String code,
        String name,
        Instant createdAt,
        Instant disabledAt
) {
    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getCreatedAt(),
                warehouse.getDisabledAt()
        );
    }
}
