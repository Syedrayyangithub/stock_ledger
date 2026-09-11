package com.ledger.stock_ledger.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateWarehouseRequest(
        @Size(max = 255) String name
) {
}
