package com.ledger.stock_ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "movement_lines")
public class MovementLine {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movement_id", nullable = false)
    private Movement movement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity_delta", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityDelta;

    protected MovementLine() {
    }

    public MovementLine(UUID id, Movement movement, Warehouse warehouse, BigDecimal quantityDelta) {
        this.id = id;
        this.movement = movement;
        this.warehouse = warehouse;
        this.quantityDelta = quantityDelta;
    }

    public UUID getId() {
        return id;
    }

    public Movement getMovement() {
        return movement;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public BigDecimal getQuantityDelta() {
        return quantityDelta;
    }
}
