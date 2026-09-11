package com.ledger.stock_ledger.service;

import com.ledger.stock_ledger.api.ApiException;
import com.ledger.stock_ledger.api.dto.Quantities;
import com.ledger.stock_ledger.api.dto.StockResponse;
import com.ledger.stock_ledger.api.dto.WarehouseStockResponse;
import com.ledger.stock_ledger.domain.Item;
import com.ledger.stock_ledger.domain.Warehouse;
import com.ledger.stock_ledger.repo.MovementRepository;
import com.ledger.stock_ledger.repo.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final MovementRepository movements;
    private final WarehouseRepository warehouses;
    private final ItemService itemService;
    private final Clock clock;

    public StockService(
            MovementRepository movements,
            WarehouseRepository warehouses,
            ItemService itemService,
            Clock clock
    ) {
        this.movements = movements;
        this.warehouses = warehouses;
        this.itemService = itemService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StockResponse current(UUID itemId, UUID warehouseId) {
        return asOf(itemId, warehouseId, Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public StockResponse asOf(UUID itemId, UUID warehouseId, Instant at) {
        if (at == null) {
            throw ApiException.badRequest("at is required");
        }
        Item item = itemService.require(itemId);
        Map<UUID, BigDecimal> totals = movements.stockByWarehouse(itemId, at).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (BigDecimal) row[1]
                ));

        List<WarehouseStockResponse> rows = new ArrayList<>();
        if (warehouseId != null) {
            Warehouse warehouse = warehouses.findById(warehouseId)
                    .orElseThrow(() -> ApiException.notFound("Warehouse not found: " + warehouseId));
            BigDecimal qty = totals.getOrDefault(warehouseId, BigDecimal.ZERO);
            rows.add(toRow(warehouse, qty));
        } else {
            for (Warehouse warehouse : warehouses.findAll()) {
                BigDecimal qty = totals.getOrDefault(warehouse.getId(), BigDecimal.ZERO);
                if (qty.compareTo(BigDecimal.ZERO) != 0) {
                    rows.add(toRow(warehouse, qty));
                }
            }
        }

        BigDecimal total = rows.stream()
                .map(WarehouseStockResponse::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StockResponse(
                item.getId(),
                item.getCode(),
                item.getName(),
                item.getUnit(),
                at,
                rows,
                Quantities.normalize(total)
        );
    }

    private WarehouseStockResponse toRow(Warehouse warehouse, BigDecimal qty) {
        return new WarehouseStockResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                Quantities.normalize(qty)
        );
    }
}
