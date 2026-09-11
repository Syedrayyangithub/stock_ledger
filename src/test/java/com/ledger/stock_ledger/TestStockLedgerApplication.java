package com.ledger.stock_ledger;

import org.springframework.boot.SpringApplication;

public class TestStockLedgerApplication {

	public static void main(String[] args) {
		SpringApplication.from(StockLedgerApplication::main).with(EmbeddedPostgresConfiguration.class).run(args);
	}

}
