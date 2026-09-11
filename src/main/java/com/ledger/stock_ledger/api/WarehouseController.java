package com.ledger.stock_ledger.api;

import com.ledger.stock_ledger.api.dto.CreateWarehouseRequest;
import com.ledger.stock_ledger.api.dto.PageResponse;
import com.ledger.stock_ledger.api.dto.UpdateWarehouseRequest;
import com.ledger.stock_ledger.api.dto.WarehouseResponse;
import com.ledger.stock_ledger.service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService warehouses;

    public WarehouseController(WarehouseService warehouses) {
        this.warehouses = warehouses;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse create(@Valid @RequestBody CreateWarehouseRequest request) {
        return warehouses.create(request);
    }

    @GetMapping
    public PageResponse<WarehouseResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return warehouses.list(pageable);
    }

    @GetMapping("/{id}")
    public WarehouseResponse get(@PathVariable UUID id) {
        return warehouses.get(id);
    }

    @PatchMapping("/{id}")
    public WarehouseResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateWarehouseRequest request) {
        return warehouses.update(id, request);
    }

    @PostMapping("/{id}/disable")
    public WarehouseResponse disable(@PathVariable UUID id) {
        return warehouses.disable(id);
    }
}
