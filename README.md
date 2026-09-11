# Stock ledger

An append-only stock ledger for warehouses. You never update a stock number. You insert a row that says what happened, and stock is the sum of those rows.

## What you need

- JDK 17 or 21 (`java -version` must not be 8)
- Docker (to run PostgreSQL for the app)
- Maven is optional; the project includes `mvnw`

If `java -version` still shows 1.8, point this shell at a newer JDK before you run Maven, for example:

```bat
set JAVA_HOME=C:\Program Files\Java\jdk-21
```

## Database

PostgreSQL 16. From the project directory:

```bash
docker compose up -d
```

That starts Postgres on port `5432` with:

- database: `stock_ledger`
- user: `stock`
- password: `stock`

If something else is already using port 5432, stop it or change the port in `compose.yaml` and `src/main/resources/application.properties`.

Flyway creates the tables when the app starts. You do not run SQL by hand.

## Run the app

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8080`.

## Seed data

Create two items (`PEN-BLUE`, `PAPER-A4`) and two warehouses (`WH-NORTH`, `WH-SOUTH`):

```bash
curl -X POST http://localhost:8080/api/demo/seed
```

The response includes their ids. Use those ids in the movement examples below. Calling seed again returns the same rows; it does not duplicate them.

## Tests

Tests start a real PostgreSQL process (not H2). Docker is not required for tests. The first run downloads a Postgres binary, so it needs network access.

```bash
./mvnw test
```

On Windows, if the default JDK is still 8:

```bat
set JAVA_HOME=C:\Program Files\Java\jdk-21
mvnw.cmd test
```

## Endpoints

Pagination uses `?page=0&size=20`. Pages are zero-based. List responses look like `{ "content": [...], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1 }`.

Errors look like `{ "status": 409, "error": "Conflict", "message": "...", "timestamp": "..." }`.

### Items

```bash
curl -X POST http://localhost:8080/api/items \
  -H "Content-Type: application/json" \
  -d '{"code":"PEN-BLUE","name":"Blue Pen","unit":"PIECES"}'
```

Units: `PIECES`, `KG`, `LITRES`.

```bash
curl http://localhost:8080/api/items
curl http://localhost:8080/api/items/{id}
curl -X PATCH http://localhost:8080/api/items/{id} \
  -H "Content-Type: application/json" \
  -d '{"name":"Blue Gel Pen"}'
curl -X POST http://localhost:8080/api/items/{id}/disable
```

### Warehouses

```bash
curl -X POST http://localhost:8080/api/warehouses \
  -H "Content-Type: application/json" \
  -d '{"code":"WH-NORTH","name":"North Warehouse"}'
```

```bash
curl http://localhost:8080/api/warehouses
curl http://localhost:8080/api/warehouses/{id}
curl -X PATCH http://localhost:8080/api/warehouses/{id} \
  -H "Content-Type: application/json" \
  -d '{"name":"North Hub"}'
curl -X POST http://localhost:8080/api/warehouses/{id}/disable
```

### Movements

IN:

```bash
curl -X POST http://localhost:8080/api/movements \
  -H "Content-Type: application/json" \
  -d "{\"kind\":\"IN\",\"itemId\":\"ITEM_ID\",\"quantity\":100,\"warehouseId\":\"WH_ID\",\"reason\":\"purchase order 4471\",\"occurredAt\":\"2026-03-01T09:00:00Z\",\"recordedBy\":\"alex\"}"
```

OUT: same body with `"kind":"OUT"`.

TRANSFER uses `fromWarehouseId` and `toWarehouseId` instead of `warehouseId`.

```bash
curl -X POST http://localhost:8080/api/movements/{id}/cancel \
  -H "Content-Type: application/json" \
  -d '{"recordedBy":"alex","reason":"wrong quantity"}'
```

```bash
curl http://localhost:8080/api/movements/{id}
curl "http://localhost:8080/api/items/{itemId}/movements?page=0&size=20"
```

There is no login. Send `recordedBy` as a name or id on each write.

### Stock

```bash
curl "http://localhost:8080/api/stock?itemId=ITEM_ID"
curl "http://localhost:8080/api/stock?itemId=ITEM_ID&warehouseId=WH_ID"
curl "http://localhost:8080/api/stock/as-of?itemId=ITEM_ID&at=2026-03-03T00:00:00Z"
curl "http://localhost:8080/api/stock/as-of?itemId=ITEM_ID&warehouseId=WH_ID&at=2026-03-03T00:00:00Z"
```

## Before you open the code

- Movement rows are insert-only. Cancellation inserts a reversing movement; the original is not edited.
- Each movement snapshots the item name and unit. History keeps the old name after a rename.
- Stock is summed from `movement_lines`. There is no quantity column on items or warehouses.
- A transfer is one movement with two lines (minus at source, plus at destination).
- OUT and TRANSFER are refused if they would take stock below zero.

## Not finished / what I would do next

See `DESIGN.md`. Short version: no authentication, no snapshot table for large ledgers, no warehouse-level reports across all items, and no UI.

## Running this for real

Run PostgreSQL with backups and point-in-time recovery. Run two or more app instances behind a load balancer. Flyway still owns schema changes. Put the database password in the environment, not in the file. Add real user accounts before anything leaves a laptop. If the movement table grows into the millions, add periodic stock snapshots (the design document explains why).
