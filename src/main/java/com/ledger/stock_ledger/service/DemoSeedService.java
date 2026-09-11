package com.ledger.stock_ledger.service;

import com.ledger.stock_ledger.api.dto.CreateItemRequest;
import com.ledger.stock_ledger.api.dto.CreateWarehouseRequest;
import com.ledger.stock_ledger.api.dto.ItemResponse;
import com.ledger.stock_ledger.api.dto.SeedResponse;
import com.ledger.stock_ledger.api.dto.WarehouseResponse;
import com.ledger.stock_ledger.domain.Unit;
import com.ledger.stock_ledger.repo.ItemRepository;
import com.ledger.stock_ledger.repo.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DemoSeedService {

    private final ItemService itemService;
    private final WarehouseService warehouseService;
    private final ItemRepository items;
    private final WarehouseRepository warehouses;

    public DemoSeedService(
            ItemService itemService,
            WarehouseService warehouseService,
            ItemRepository items,
            WarehouseRepository warehouses
    ) {
        this.itemService = itemService;
        this.warehouseService = warehouseService;
        this.items = items;
        this.warehouses = warehouses;
    }

    @Transactional
    public SeedResponse seed() {
        ItemResponse pen = items.findByCode("PEN-BLUE")
                .map(ItemResponse::from)
                .orElseGet(() -> itemService.create(new CreateItemRequest("PEN-BLUE", "Blue Pen", Unit.PIECES)));
        ItemResponse paper = items.findByCode("PAPER-A4")
                .map(ItemResponse::from)
                .orElseGet(() -> itemService.create(new CreateItemRequest("PAPER-A4", "A4 Paper", Unit.PIECES)));

        WarehouseResponse north = warehouses.findByCode("WH-NORTH")
                .map(WarehouseResponse::from)
                .orElseGet(() -> warehouseService.create(new CreateWarehouseRequest("WH-NORTH", "North Warehouse")));
        WarehouseResponse south = warehouses.findByCode("WH-SOUTH")
                .map(WarehouseResponse::from)
                .orElseGet(() -> warehouseService.create(new CreateWarehouseRequest("WH-SOUTH", "South Warehouse")));

        return SeedResponse.of(List.of(pen, paper), List.of(north, south));
    }
}
