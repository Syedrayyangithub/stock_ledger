package com.ledger.stock_ledger.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MovementLineResponse(
        UUID warehouseId,
        String warehouseCode,
        String warehouseName,
        BigDecimal quantityDelta
) {
}
