package com.ledger.stock_ledger.api;

import com.ledger.stock_ledger.api.dto.SeedResponse;
import com.ledger.stock_ledger.service.DemoSeedService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoSeedService seed;

    public DemoController(DemoSeedService seed) {
        this.seed = seed;
    }

    @PostMapping("/seed")
    public SeedResponse seed() {
        return seed.seed();
    }
}
