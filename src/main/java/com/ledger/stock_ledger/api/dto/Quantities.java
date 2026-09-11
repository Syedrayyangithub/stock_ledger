package com.ledger.stock_ledger.api.dto;

import java.math.BigDecimal;

public final class Quantities {

    private Quantities() {
    }

    public static BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros().scale() < 0 ? value.stripTrailingZeros().setScale(0) : value.stripTrailingZeros();
    }
}
