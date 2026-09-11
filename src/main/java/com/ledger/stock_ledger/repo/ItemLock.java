package com.ledger.stock_ledger.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ItemLock {

    private final JdbcTemplate jdbc;

    public ItemLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void acquire(UUID itemId) {
        jdbc.execute((java.sql.Connection connection) -> {
            try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")) {
                statement.setString(1, itemId.toString());
                statement.execute();
            }
            return null;
        });
    }
}
