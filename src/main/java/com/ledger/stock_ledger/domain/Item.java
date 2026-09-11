package com.ledger.stock_ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "items")
public class Item {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Unit unit;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Item() {
    }

    public Item(UUID id, String code, String name, Unit unit, Instant createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.unit = unit;
        this.createdAt = createdAt;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeUnit(Unit unit) {
        this.unit = unit;
    }

    public void disable(Instant at) {
        this.disabledAt = at;
    }

    public boolean isDisabled() {
        return disabledAt != null;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Unit getUnit() {
        return unit;
    }

    public Instant getDisabledAt() {
        return disabledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
