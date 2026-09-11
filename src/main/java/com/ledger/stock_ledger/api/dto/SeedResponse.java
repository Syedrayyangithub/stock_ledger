package com.ledger.stock_ledger.api.dto;

import java.util.List;
import java.util.UUID;

public record SeedResponse(
        List<ItemResponse> items,
        List<WarehouseResponse> warehouses,
        String note
) {
    public static SeedResponse of(List<ItemResponse> items, List<WarehouseResponse> warehouses) {
        UUID firstItem = items.get(0).id();
        UUID firstWarehouse = warehouses.get(0).id();
        return new SeedResponse(
                items,
                warehouses,
                "Record a movement with itemId=" + firstItem + " and warehouseId=" + firstWarehouse
        );
    }
}
