package com.ledger.stock_ledger.service;

import com.ledger.stock_ledger.api.ApiException;
import com.ledger.stock_ledger.api.dto.CreateWarehouseRequest;
import com.ledger.stock_ledger.api.dto.PageResponse;
import com.ledger.stock_ledger.api.dto.UpdateWarehouseRequest;
import com.ledger.stock_ledger.api.dto.WarehouseResponse;
import com.ledger.stock_ledger.domain.Warehouse;
import com.ledger.stock_ledger.repo.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouses;
    private final Clock clock;

    public WarehouseService(WarehouseRepository warehouses, Clock clock) {
        this.warehouses = warehouses;
        this.clock = clock;
    }

    @Transactional
    public WarehouseResponse create(CreateWarehouseRequest request) {
        String code = request.code().toUpperCase();
        if (warehouses.existsByCode(code)) {
            throw ApiException.conflict("Warehouse code already exists: " + code);
        }
        Warehouse warehouse = new Warehouse(UUID.randomUUID(), code, request.name().trim(), Instant.now(clock));
        return WarehouseResponse.from(warehouses.save(warehouse));
    }

    @Transactional(readOnly = true)
    public PageResponse<WarehouseResponse> list(Pageable pageable) {
        Page<Warehouse> page = warehouses.findAll(pageable);
        return new PageResponse<>(
                page.getContent().stream().map(WarehouseResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public WarehouseResponse get(UUID id) {
        return WarehouseResponse.from(require(id));
    }

    @Transactional
    public WarehouseResponse update(UUID id, UpdateWarehouseRequest request) {
        Warehouse warehouse = require(id);
        if (request.name() != null && !request.name().isBlank()) {
            warehouse.rename(request.name().trim());
        }
        return WarehouseResponse.from(warehouse);
    }

    @Transactional
    public WarehouseResponse disable(UUID id) {
        Warehouse warehouse = require(id);
        if (!warehouse.isDisabled()) {
            warehouse.disable(Instant.now(clock));
        }
        return WarehouseResponse.from(warehouse);
    }

    public Warehouse require(UUID id) {
        return warehouses.findById(id).orElseThrow(() -> ApiException.notFound("Warehouse not found: " + id));
    }

    public Warehouse requireByCode(String code) {
        return warehouses.findByCode(code.toUpperCase())
                .orElseThrow(() -> ApiException.notFound("Warehouse not found: " + code));
    }
}
