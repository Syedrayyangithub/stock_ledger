CREATE TABLE items (
    id              UUID PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    unit            VARCHAR(32)  NOT NULL,
    disabled_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_items_code UNIQUE (code)
);

CREATE TABLE warehouses (
    id              UUID PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    disabled_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_warehouses_code UNIQUE (code)
);

CREATE TABLE movements (
    id                    UUID PRIMARY KEY,
    kind                  VARCHAR(32)     NOT NULL,
    item_id               UUID            NOT NULL REFERENCES items (id),
    item_code_snapshot    VARCHAR(64)     NOT NULL,
    item_name_snapshot    VARCHAR(255)    NOT NULL,
    item_unit_snapshot    VARCHAR(32)     NOT NULL,
    quantity              NUMERIC(19, 6)  NOT NULL,
    reason                VARCHAR(1000)   NOT NULL,
    occurred_at           TIMESTAMPTZ     NOT NULL,
    recorded_at           TIMESTAMPTZ     NOT NULL,
    recorded_by           VARCHAR(255)    NOT NULL,
    cancels_movement_id   UUID            REFERENCES movements (id),
    CONSTRAINT ck_movements_quantity_positive CHECK (quantity > 0),
    CONSTRAINT uk_movements_cancels UNIQUE (cancels_movement_id)
);

CREATE INDEX idx_movements_item_occurred
    ON movements (item_id, occurred_at DESC, recorded_at DESC);

CREATE TABLE movement_lines (
    id              UUID PRIMARY KEY,
    movement_id     UUID           NOT NULL REFERENCES movements (id),
    warehouse_id    UUID           NOT NULL REFERENCES warehouses (id),
    quantity_delta  NUMERIC(19, 6) NOT NULL,
    CONSTRAINT ck_lines_delta_nonzero CHECK (quantity_delta <> 0)
);

CREATE INDEX idx_lines_movement ON movement_lines (movement_id);
CREATE INDEX idx_lines_warehouse ON movement_lines (warehouse_id);
CREATE INDEX idx_lines_stock
    ON movement_lines (warehouse_id, movement_id);
