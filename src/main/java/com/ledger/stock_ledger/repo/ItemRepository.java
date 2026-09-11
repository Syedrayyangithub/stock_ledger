package com.ledger.stock_ledger.repo;

import com.ledger.stock_ledger.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    Optional<Item> findByCode(String code);

    boolean existsByCode(String code);
}
