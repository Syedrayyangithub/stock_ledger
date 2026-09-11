# Design

## Tables vs calculated values

Stored:

- `items` — code, name, unit, optional `disabled_at`. The current name lives here. It can change.
- `warehouses` — code, name, optional `disabled_at`.
- `movements` — one row per event (IN, OUT, TRANSFER, CANCEL). Quantity is always positive. The item’s code, name and unit are copied onto the row at write time. `occurred_at` is when the physical event happened. `recorded_at` is when we stored it. `cancels_movement_id` is set only on a cancellation.
- `movement_lines` — the effect on one warehouse. Signed `quantity_delta`. IN has one positive line. OUT has one negative line. TRANSFER has two lines. CANCEL copies the original lines with the signs flipped.

Worked out when somebody asks:

- Current stock, and stock at a past moment: `SUM(quantity_delta)` for that item (and warehouse, if given) over movements with `occurred_at <=` the moment.
- Whether a movement has been cancelled: look for a later row whose `cancels_movement_id` points at it. The original row is never updated with a “cancelled” flag.

## Stock on a past date

Take the timestamp the caller sent. Include every movement for that item whose `occurred_at` is on or before that timestamp, including backdated rows recorded later. Sum the line deltas. If they named a warehouse, keep only that warehouse’s lines.

A cancellation is dated at the moment it was recorded, not at the original event. Cancelling last Tuesday’s OUT today does not change last Wednesday’s figure. It changes stock from today onward.

## Cancelling, row by row

Suppose movement `M1` is IN 10 at North.

1. `movements`: `M1`, kind IN, quantity 10, snapshots, reason, occurred_at, recorded_by.
2. `movement_lines`: North, `+10`.

Someone cancels `M1`. We do not touch those two rows. We insert:

3. `movements`: `M2`, kind CANCEL, quantity 10, `cancels_movement_id = M1`, occurred_at and recorded_at = now, new reason and recorded_by. Item snapshots are taken from the item as it is now (the original name still sits on `M1`).
4. `movement_lines`: North, `-10`.

A unique constraint on `cancels_movement_id` stops a second cancel. Asking for history returns both rows; `M2` points at `M1`, and `M1` is shown with `cancelledByMovementId = M2`.

## Ten million movements

Summing the lines is the right model at ten thousand rows. At ten million it is still correct and it will get slow, especially “stock of one item in one warehouse”, which still scans that item’s history.

What breaks first: as-of queries and OUT checks that re-sum before each write. Indexes on `(item_id, occurred_at)` only delay that.

What I would add in production: a snapshot table, say weekly per item and warehouse. As-of stock = last snapshot before the date + sum of the lines after it. Writes stay append-only. I would not keep a live quantity column as the source of truth; that is the system this replaces.

## Decisions from the brief

**Stock cannot go below zero.** OUT and TRANSFER are refused if the warehouse would be short at `occurred_at` or right now. That matches a warehouse floor. It hurts when someone needs to record a known shortage, or when they backdate an OUT before a later IN; they must record the IN first. Concurrent requests are serialised per item with a Postgres advisory lock so two OUTs cannot both pass the check.

**A transfer is one movement and two lines.** One id, one reason, one recorded-by. Stock math stays “sum the lines”. The cost is that you cannot cancel half a transfer; you cancel the whole thing.

**Stock is summed on read.** No running total is stored. That keeps history as the only source of truth. The cost is read cost at large scale (see above).

**Direction lives on the line as a signed delta.** The movement header keeps a positive quantity and a kind, which is easier to read. The lines are what the calculator uses. The cost is two representations of the same fact; they are written together in one transaction.

**Backdated arrivals change the past.** If you record today that 10 arrived last Tuesday, last Wednesday’s stock includes those 10. `occurred_at` is the physical time. That is the point of the as-of query. The cost is that a late correction rewrites a figure you already sent to a customer; you still have `recorded_at` to explain when you learned it.

**API.** `/api/items`, `/api/warehouses`, `/api/movements`, `/api/stock` and `/api/stock/as-of`. Create returns 201. Duplicate codes, second cancel, disabled master data, and insufficient stock return 409. Missing rows return 404. Validation returns 400 with a `message` and optional `details`. Lists are paged, zero-based.

Other assumptions: disabling an item blocks new IN/OUT/TRANSFER but stock reports still include it; cancelling is still allowed so a mistake can be reversed. Disabling a warehouse blocks new lines against it. Cancellations cannot be cancelled; record a new movement. There are no user accounts; `recordedBy` is a string.

## Gaps before real warehouses

Not finished: authentication, roles, idempotency keys for retries, a UI, stock for all items in one warehouse, serial/batch tracking.

Still wrong for production: summing the whole history; no audit of who disabled an item; changing an item’s unit is allowed even though old and new movements would then mix units in one sum (rename is the supported edit).

To run it for real: Postgres with backups and PITR, two app instances, secrets in the environment, Flyway as now, then auth, then snapshots when the table is large. I would add an idempotency key on record-movement before anyone double-clicks a goods-in form.
