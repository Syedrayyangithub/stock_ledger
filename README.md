# Stock ledger

Append-only warehouse stock ledger built with **Java 17**, **Spring Boot 4**, and **PostgreSQL**.

Stock is never stored as a single number you keep updating. Every arrival, dispatch, transfer, or correction adds a row. Current stock and stock on a past date are calculated by summing those rows.

There is no web UI and no login. Send `recordedBy` as a name or id on each write.

Design decisions (negative stock, transfers, backdating, and so on) are explained in [`DESIGN.md`](DESIGN.md).

---

## Prerequisites

Install on a clean machine:

| Requirement | Notes |
|-------------|--------|
| **JDK 17 or 21** | Run `java -version`. It must **not** show 1.8. |
| **Docker Desktop** | Used to run PostgreSQL for the app. Start Docker before step 2 below. |
| **Network** | First Maven run downloads dependencies. First test run downloads an embedded Postgres binary. |

You do **not** need Maven installed globally; the project includes `mvnw` / `mvnw.cmd`.

---

## Run from scratch

All commands assume you are in the project root — the folder that contains `pom.xml`, `compose.yaml`, and `mvnw.cmd`:

```text
stock-ledger/
  pom.xml
  compose.yaml
  mvnw.cmd
  src/
  README.md
  DESIGN.md
```

If you unzipped into a parent folder, `cd` into the inner `stock-ledger` directory first.

### Windows (PowerShell)

```powershell
cd path\to\stock-ledger

# Only if java -version still shows 1.8 in this terminal:
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

java -version

docker compose up -d
docker compose ps

.\mvnw.cmd spring-boot:run
```

Leave that terminal open. The API listens on **http://localhost:8080**.

There is **no** homepage at `/`. A 404 on the root URL is normal. Use `/api/...` paths below.

In a **second** terminal (same project folder):

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/demo/seed
Invoke-RestMethod -Uri http://localhost:8080/api/items
Invoke-RestMethod -Uri http://localhost:8080/api/warehouses
```

Or open in a browser:

- http://localhost:8080/api/items  
- http://localhost:8080/api/warehouses  

### macOS / Linux

```bash
cd path/to/stock-ledger
java -version
docker compose up -d
./mvnw spring-boot:run
```

Seed (another terminal):

```bash
curl -X POST http://localhost:8080/api/demo/seed
curl http://localhost:8080/api/items
```

---

## Database

**PostgreSQL 16** (Docker image `postgres:16-alpine`).

Start it:

```bash
docker compose up -d
```

Connection settings (also in `src/main/resources/application.properties`):

| Setting | Value |
|---------|--------|
| Host | `localhost` |
| Port | `5432` |
| Database | `stock_ledger` |
| User | `stock` |
| Password | `stock` |

Check the container is healthy:

```bash
docker compose ps
```

**Schema:** [Flyway](https://flywaydb.org/) runs migration `V1__init.sql` when the app starts. You do not create tables manually.

If port 5432 is already in use, change the host port in `compose.yaml` and update `spring.datasource.url` in `application.properties` to match.

Stop Postgres when finished:

```bash
docker compose down
```

---

## Seed data (start recording movements immediately)

**Endpoint:** `POST /api/demo/seed`

Creates (or returns existing):

- Items: `PEN-BLUE`, `PAPER-A4`
- Warehouses: `WH-NORTH`, `WH-SOUTH`

PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/demo/seed
```

curl:

```bash
curl -X POST http://localhost:8080/api/demo/seed
```

The response includes item and warehouse ids. Use those ids in movement and stock requests. Calling seed again is idempotent — it does not duplicate rows.

---

## Tests

Tests use a **real PostgreSQL** process via [embedded-postgres](https://github.com/zonkyio/embedded-postgres) (not H2). **Docker is not required** to run tests.

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

Expect: `Tests run: 7`, `BUILD SUCCESS`.

Coverage includes past-date stock, cancelling a movement, item rename snapshots, backdated arrivals, disabled items, and negative-stock refusal.

---

## API overview

Base URL: `http://localhost:8080`

**Pagination:** `?page=0&size=20` (zero-based). Lists return:

```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 2, "totalPages": 1 }
```

**Errors:**

```json
{ "status": 409, "error": "Conflict", "message": "...", "timestamp": "..." }
```

| HTTP | When |
|------|------|
| 201 | Created (new item, warehouse, movement, cancel) |
| 400 | Validation / bad JSON |
| 404 | Unknown id |
| 409 | Duplicate code, insufficient stock, disabled master data, second cancel |

Replace `{id}`, `{itemId}`, `{whId}`, etc. with UUIDs from seed or list responses.

---

## Endpoints (one example each)

Examples use curl. On Windows PowerShell, use `Invoke-RestMethod` or `curl.exe` (PowerShell’s `curl` alias does not support `-X`).

### Demo

**POST /api/demo/seed** — create sample items and warehouses

```bash
curl -X POST http://localhost:8080/api/demo/seed
```

### Items

Units: `PIECES`, `KG`, `LITRES`.

**POST /api/items** — create item (201)

```bash
curl -X POST http://localhost:8080/api/items \
  -H "Content-Type: application/json" \
  -d '{"code":"PEN-BLUE","name":"Blue Pen","unit":"PIECES"}'
```

**GET /api/items** — list items (paged)

```bash
curl "http://localhost:8080/api/items?page=0&size=20"
```

**GET /api/items/{id}** — get one item

```bash
curl http://localhost:8080/api/items/ITEM_ID
```

**PATCH /api/items/{id}** — rename or change unit

```bash
curl -X PATCH http://localhost:8080/api/items/ITEM_ID \
  -H "Content-Type: application/json" \
  -d '{"name":"Blue Gel Pen"}'
```

**POST /api/items/{id}/disable** — disable item (blocks new movements)

```bash
curl -X POST http://localhost:8080/api/items/ITEM_ID/disable
```

### Warehouses

**POST /api/warehouses** — create warehouse (201)

```bash
curl -X POST http://localhost:8080/api/warehouses \
  -H "Content-Type: application/json" \
  -d '{"code":"WH-NORTH","name":"North Warehouse"}'
```

**GET /api/warehouses** — list warehouses (paged)

```bash
curl "http://localhost:8080/api/warehouses?page=0&size=20"
```

**GET /api/warehouses/{id}** — get one warehouse

```bash
curl http://localhost:8080/api/warehouses/WH_ID
```

**PATCH /api/warehouses/{id}** — rename warehouse

```bash
curl -X PATCH http://localhost:8080/api/warehouses/WH_ID \
  -H "Content-Type: application/json" \
  -d '{"name":"North Hub"}'
```

**POST /api/warehouses/{id}/disable** — disable warehouse

```bash
curl -X POST http://localhost:8080/api/warehouses/WH_ID/disable
```

### Movements

Required on every movement: `kind`, `itemId`, `quantity`, `reason`, `occurredAt` (ISO-8601 UTC, e.g. `2026-03-01T09:00:00Z`), `recordedBy`.

**POST /api/movements** — record IN

```bash
curl -X POST http://localhost:8080/api/movements \
  -H "Content-Type: application/json" \
  -d '{"kind":"IN","itemId":"ITEM_ID","quantity":100,"warehouseId":"WH_ID","reason":"purchase order 4471","occurredAt":"2026-03-01T09:00:00Z","recordedBy":"alex"}'
```

**OUT:** same URL, `"kind":"OUT"`, same `warehouseId`.

**TRANSFER:** same URL, `"kind":"TRANSFER"`, use `fromWarehouseId` and `toWarehouseId` instead of `warehouseId`.

```bash
curl -X POST http://localhost:8080/api/movements \
  -H "Content-Type: application/json" \
  -d '{"kind":"TRANSFER","itemId":"ITEM_ID","quantity":20,"fromWarehouseId":"WH_NORTH","toWarehouseId":"WH_SOUTH","reason":"restock south","occurredAt":"2026-03-04T08:00:00Z","recordedBy":"sam"}'
```

**POST /api/movements/{id}/cancel** — reverse a movement (201); original row is not deleted or edited

```bash
curl -X POST http://localhost:8080/api/movements/MOVEMENT_ID/cancel \
  -H "Content-Type: application/json" \
  -d '{"recordedBy":"alex","reason":"wrong quantity"}'
```

**GET /api/movements/{id}** — get one movement (includes cancel links)

```bash
curl http://localhost:8080/api/movements/MOVEMENT_ID
```

**GET /api/items/{itemId}/movements** — movement history, newest first (paged)

```bash
curl "http://localhost:8080/api/items/ITEM_ID/movements?page=0&size=20"
```

### Stock

**GET /api/stock** — current stock for one item (all warehouses, or one warehouse if `warehouseId` is set)

```bash
curl "http://localhost:8080/api/stock?itemId=ITEM_ID"
curl "http://localhost:8080/api/stock?itemId=ITEM_ID&warehouseId=WH_ID"
```

**GET /api/stock/as-of** — stock at a past date/time (`at` is required)

```bash
curl "http://localhost:8080/api/stock/as-of?itemId=ITEM_ID&at=2026-03-03T00:00:00Z"
curl "http://localhost:8080/api/stock/as-of?itemId=ITEM_ID&warehouseId=WH_ID&at=2026-03-03T00:00:00Z"
```

Stock uses `occurred_at` on movements. A cancellation affects stock from when it was **recorded**, not the original event date.

---

## PowerShell example (record IN after seed)

```powershell
$itemId = "PASTE_PEN_BLUE_ID"
$whId   = "PASTE_WH_NORTH_ID"

$body = @"
{"kind":"IN","itemId":"$itemId","quantity":100,"warehouseId":"$whId","reason":"purchase order 4471","occurredAt":"2026-03-01T09:00:00Z","recordedBy":"alex"}
"@

Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/movements `
  -ContentType "application/json" -Body $body
```

---

## Before you open the code

- **Append-only ledger:** movement rows are never updated or deleted. Cancelling inserts a new `CANCEL` movement with reversed line deltas.
- **Snapshots:** each movement stores the item’s name and unit at write time. Renaming an item does not change old history.
- **Stock is calculated:** `movement_lines.quantity_delta` is summed. There is no stock column on items or warehouses.
- **Transfer:** one movement, two lines (negative at source, positive at destination).
- **Negative stock:** OUT and TRANSFER are refused if they would drop below zero at `occurredAt` or at the current moment.
- **Disabled items:** new IN/OUT/TRANSFER are blocked; existing stock still appears in reports.
- **Package layout:** `domain` (entities), `repo`, `service`, `api` (controllers + DTOs), `config`. Schema in `src/main/resources/db/migration/`.

---

## Not finished / what I would do next

See [`DESIGN.md`](DESIGN.md) for full detail. Summary:

- No authentication or roles
- No UI
- No idempotency keys on writes
- No stock snapshot table (full history scan on every read; fine for this assignment, not for millions of rows)
- No “all items in one warehouse” report
- Changing an item’s **unit** is allowed but not recommended in production

---

## Running this for real

- Host **PostgreSQL** with backups and point-in-time recovery (managed Postgres or self-hosted with `compose.yaml` as a starting point only).
- Run **two or more** stateless app instances behind a load balancer.
- Keep **Flyway** for schema migrations; inject DB credentials via environment variables, not committed files.
- Add **authentication** before any production use.
- When movement volume grows large, add **periodic stock snapshots** per item/warehouse (see DESIGN.md) while keeping the append-only ledger as source of truth.
- Add **idempotency keys** on `POST /api/movements` before exposing the API to human operators or integrations.

No paid services are required for local development or evaluation.

---

