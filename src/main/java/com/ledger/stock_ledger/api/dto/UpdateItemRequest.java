package com.ledger.stock_ledger.api.dto;

import com.ledger.stock_ledger.domain.Unit;
import jakarta.validation.constraints.Size;

public record UpdateItemRequest(
        @Size(max = 255) String name,
        Unit unit
) {
}
