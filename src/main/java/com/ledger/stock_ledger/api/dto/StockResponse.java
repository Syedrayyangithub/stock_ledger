package com.ledger.stock_ledger.api.dto;

import com.ledger.stock_ledger.domain.Unit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockResponse(
        UUID itemId,
        String itemCode,
        String itemName,
        Unit unit,
        Instant asOf,
        List<WarehouseStockResponse> warehouses,
        BigDecimal total
) {
}
