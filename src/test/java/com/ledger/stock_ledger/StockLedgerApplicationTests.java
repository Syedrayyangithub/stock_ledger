package com.ledger.stock_ledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(EmbeddedPostgresConfiguration.class)
@SpringBootTest
class StockLedgerApplicationTests {

	@Test
	void contextLoads() {
	}

}
