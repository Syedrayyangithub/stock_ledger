package com.ledger.stock_ledger.api;

import com.ledger.stock_ledger.api.dto.StockResponse;
import com.ledger.stock_ledger.service.StockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stock;

    public StockController(StockService stock) {
        this.stock = stock;
    }

    @GetMapping
    public StockResponse current(
            @RequestParam UUID itemId,
            @RequestParam(required = false) UUID warehouseId
    ) {
        return stock.current(itemId, warehouseId);
    }

    @GetMapping("/as-of")
    public StockResponse asOf(
            @RequestParam UUID itemId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam Instant at
    ) {
        return stock.asOf(itemId, warehouseId, at);
    }
}
