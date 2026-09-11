package com.ledger.stock_ledger.api.dto;

import com.ledger.stock_ledger.domain.Item;
import com.ledger.stock_ledger.domain.Unit;

import java.time.Instant;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        String code,
        String name,
        Unit unit,
        Instant createdAt,
        Instant disabledAt
) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getCode(),
                item.getName(),
                item.getUnit(),
                item.getCreatedAt(),
                item.getDisabledAt()
        );
    }
}
