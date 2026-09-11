package com.ledger.stock_ledger.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "movements")
public class Movement {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MovementKind kind;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "item_code_snapshot", nullable = false, length = 64)
    private String itemCodeSnapshot;

    @Column(name = "item_name_snapshot", nullable = false)
    private String itemNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_unit_snapshot", nullable = false, length = 32)
    private Unit itemUnitSnapshot;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by", nullable = false)
    private String recordedBy;

    @Column(name = "cancels_movement_id")
    private UUID cancelsMovementId;

    @OneToMany(mappedBy = "movement", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    private List<MovementLine> lines = new ArrayList<>();

    protected Movement() {
    }

    public Movement(
            UUID id,
            MovementKind kind,
            Item item,
            BigDecimal quantity,
            String reason,
            Instant occurredAt,
            Instant recordedAt,
            String recordedBy,
            UUID cancelsMovementId
    ) {
        this.id = id;
        this.kind = kind;
        this.item = item;
        this.itemCodeSnapshot = item.getCode();
        this.itemNameSnapshot = item.getName();
        this.itemUnitSnapshot = item.getUnit();
        this.quantity = quantity;
        this.reason = reason;
        this.occurredAt = occurredAt;
        this.recordedAt = recordedAt;
        this.recordedBy = recordedBy;
        this.cancelsMovementId = cancelsMovementId;
    }

    public void addLine(Warehouse warehouse, BigDecimal quantityDelta) {
        lines.add(new MovementLine(UUID.randomUUID(), this, warehouse, quantityDelta));
    }

    public UUID getId() {
        return id;
    }

    public MovementKind getKind() {
        return kind;
    }

    public Item getItem() {
        return item;
    }

    public String getItemCodeSnapshot() {
        return itemCodeSnapshot;
    }

    public String getItemNameSnapshot() {
        return itemNameSnapshot;
    }

    public Unit getItemUnitSnapshot() {
        return itemUnitSnapshot;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public UUID getCancelsMovementId() {
        return cancelsMovementId;
    }

    public List<MovementLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
