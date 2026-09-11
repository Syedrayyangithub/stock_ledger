package com.ledger.stock_ledger.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WarehouseStockResponse(
        UUID warehouseId,
        String warehouseCode,
        String warehouseName,
        BigDecimal quantity
) {
}
