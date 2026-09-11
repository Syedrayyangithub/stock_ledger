package com.ledger.stock_ledger.service;

import com.ledger.stock_ledger.api.ApiException;
import com.ledger.stock_ledger.api.dto.CancelMovementRequest;
import com.ledger.stock_ledger.api.dto.CreateMovementRequest;
import com.ledger.stock_ledger.api.dto.MovementResponse;
import com.ledger.stock_ledger.api.dto.PageResponse;
import com.ledger.stock_ledger.domain.Item;
import com.ledger.stock_ledger.domain.Movement;
import com.ledger.stock_ledger.domain.MovementKind;
import com.ledger.stock_ledger.domain.Warehouse;
import com.ledger.stock_ledger.repo.ItemLock;
import com.ledger.stock_ledger.repo.MovementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MovementService {

    private final MovementRepository movements;
    private final ItemLock itemLock;
    private final ItemService itemService;
    private final WarehouseService warehouseService;
    private final Clock clock;

    public MovementService(
            MovementRepository movements,
            ItemLock itemLock,
            ItemService itemService,
            WarehouseService warehouseService,
            Clock clock
    ) {
        this.movements = movements;
        this.itemLock = itemLock;
        this.itemService = itemService;
        this.warehouseService = warehouseService;
        this.clock = clock;
    }

    @Transactional
    public MovementResponse record(CreateMovementRequest request) {
        if (request.kind() == MovementKind.CANCEL) {
            throw ApiException.badRequest("Cancellations must be created with POST /api/movements/{id}/cancel");
        }

        Instant now = Instant.now(clock);
        itemLock.acquire(request.itemId());
        Item item = itemService.require(request.itemId());

        if (item.isDisabled()) {
            throw ApiException.conflict("Item " + item.getCode() + " is disabled; new movements are refused");
        }
        if (request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("quantity must be greater than 0");
        }

        Movement movement = new Movement(
                UUID.randomUUID(),
                request.kind(),
                item,
                request.quantity(),
                request.reason().trim(),
                request.occurredAt(),
                now,
                request.recordedBy().trim(),
                null
        );

        switch (request.kind()) {
            case IN -> addIn(movement, request, item);
            case OUT -> addOut(movement, request, item);
            case TRANSFER -> addTransfer(movement, request, item);
            case CANCEL -> throw ApiException.badRequest("Invalid kind");
        }

        Movement saved = movements.save(movement);
        return MovementResponse.from(saved, null);
    }

    @Transactional
    public MovementResponse cancel(UUID movementId, CancelMovementRequest request) {
        Instant now = Instant.now(clock);
        Movement original = movements.findById(movementId)
                .orElseThrow(() -> ApiException.notFound("Movement not found: " + movementId));

        itemLock.acquire(original.getItem().getId());
        original = movements.findById(movementId)
                .orElseThrow(() -> ApiException.notFound("Movement not found: " + movementId));

        if (original.getKind() == MovementKind.CANCEL) {
            throw ApiException.conflict("A cancellation cannot itself be cancelled; record a new movement instead");
        }
        if (movements.existsByCancelsMovementId(original.getId())) {
            throw ApiException.conflict("Movement " + original.getId() + " has already been cancelled");
        }

        Movement cancellation = new Movement(
                UUID.randomUUID(),
                MovementKind.CANCEL,
                original.getItem(),
                original.getQuantity(),
                request.reason().trim(),
                now,
                now,
                request.recordedBy().trim(),
                original.getId()
        );
        original.getLines().forEach(line ->
                cancellation.addLine(line.getWarehouse(), line.getQuantityDelta().negate())
        );

        Movement saved = movements.save(cancellation);
        return MovementResponse.from(saved, null);
    }

    @Transactional(readOnly = true)
    public MovementResponse get(UUID id) {
        Movement movement = movements.findById(id)
                .orElseThrow(() -> ApiException.notFound("Movement not found: " + id));
        UUID cancelledBy = movements.findByCancelsMovementId(id).map(Movement::getId).orElse(null);
        return MovementResponse.from(movement, cancelledBy);
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementResponse> history(UUID itemId, Pageable pageable) {
        itemService.require(itemId);
        Page<Movement> page = movements.findByItem_IdOrderByOccurredAtDescRecordedAtDescIdDesc(itemId, pageable);
        Map<UUID, UUID> cancelledBy = cancelledByMap(page.getContent());
        return new PageResponse<>(
                page.getContent().stream()
                        .map(m -> MovementResponse.from(m, cancelledBy.get(m.getId())))
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private Map<UUID, UUID> cancelledByMap(List<Movement> page) {
        List<UUID> ids = page.stream().map(Movement::getId).toList();
        Map<UUID, UUID> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        movements.findByCancelsMovementIdIn(ids)
                .forEach(cancel -> map.put(cancel.getCancelsMovementId(), cancel.getId()));
        return map;
    }

    private void addIn(Movement movement, CreateMovementRequest request, Item item) {
        Warehouse warehouse = requireActiveWarehouse(request.warehouseId(), "warehouseId is required for IN");
        movement.addLine(warehouse, request.quantity());
    }

    private void addOut(Movement movement, CreateMovementRequest request, Item item) {
        Warehouse warehouse = requireActiveWarehouse(request.warehouseId(), "warehouseId is required for OUT");
        assertEnoughStock(item, warehouse, request.quantity(), request.occurredAt());
        movement.addLine(warehouse, request.quantity().negate());
    }

    private void addTransfer(Movement movement, CreateMovementRequest request, Item item) {
        if (request.fromWarehouseId() == null || request.toWarehouseId() == null) {
            throw ApiException.badRequest("fromWarehouseId and toWarehouseId are required for TRANSFER");
        }
        if (request.fromWarehouseId().equals(request.toWarehouseId())) {
            throw ApiException.badRequest("A transfer must be between two different warehouses");
        }
        Warehouse from = requireActiveWarehouse(request.fromWarehouseId(), "fromWarehouseId is required for TRANSFER");
        Warehouse to = requireActiveWarehouse(request.toWarehouseId(), "toWarehouseId is required for TRANSFER");
        assertEnoughStock(item, from, request.quantity(), request.occurredAt());
        movement.addLine(from, request.quantity().negate());
        movement.addLine(to, request.quantity());
    }

    private Warehouse requireActiveWarehouse(UUID id, String missingMessage) {
        if (id == null) {
            throw ApiException.badRequest(missingMessage);
        }
        Warehouse warehouse = warehouseService.require(id);
        if (warehouse.isDisabled()) {
            throw ApiException.conflict("Warehouse " + warehouse.getCode() + " is disabled; new movements are refused");
        }
        return warehouse;
    }

    private void assertEnoughStock(Item item, Warehouse warehouse, BigDecimal quantity, Instant occurredAt) {
        BigDecimal asOfThen = movements.stockAt(item.getId(), warehouse.getId(), occurredAt);
        if (asOfThen.compareTo(quantity) < 0) {
            throw ApiException.conflict(
                    "Not enough stock of " + item.getCode() + " in " + warehouse.getCode()
                            + " at " + occurredAt + ": available " + asOfThen.stripTrailingZeros().toPlainString()
                            + ", requested " + quantity.stripTrailingZeros().toPlainString()
            );
        }
        Instant now = Instant.now(clock);
        if (occurredAt.isBefore(now)) {
            BigDecimal asOfNow = movements.stockAt(item.getId(), warehouse.getId(), now);
            if (asOfNow.compareTo(quantity) < 0) {
                throw ApiException.conflict(
                        "Not enough current stock of " + item.getCode() + " in " + warehouse.getCode()
                                + ": available " + asOfNow.stripTrailingZeros().toPlainString()
                                + ", requested " + quantity.stripTrailingZeros().toPlainString()
                );
            }
        }
    }
}
