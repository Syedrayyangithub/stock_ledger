package com.ledger.stock_ledger.repo;

import com.ledger.stock_ledger.domain.Movement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovementRepository extends JpaRepository<Movement, UUID> {

    boolean existsByCancelsMovementId(UUID cancelsMovementId);

    Optional<Movement> findByCancelsMovementId(UUID cancelsMovementId);

    Page<Movement> findByItem_IdOrderByOccurredAtDescRecordedAtDescIdDesc(UUID itemId, Pageable pageable);

    List<Movement> findByCancelsMovementIdIn(List<UUID> movementIds);

    @Query("""
            SELECT COALESCE(SUM(ml.quantityDelta), 0)
            FROM MovementLine ml
            WHERE ml.movement.item.id = :itemId
              AND ml.warehouse.id = :warehouseId
              AND ml.movement.occurredAt <= :asOf
            """)
    BigDecimal stockAt(
            @Param("itemId") UUID itemId,
            @Param("warehouseId") UUID warehouseId,
            @Param("asOf") Instant asOf
    );

    @Query("""
            SELECT ml.warehouse.id, COALESCE(SUM(ml.quantityDelta), 0)
            FROM MovementLine ml
            WHERE ml.movement.item.id = :itemId
              AND ml.movement.occurredAt <= :asOf
            GROUP BY ml.warehouse.id
            """)
    List<Object[]> stockByWarehouse(
            @Param("itemId") UUID itemId,
            @Param("asOf") Instant asOf
    );
}
