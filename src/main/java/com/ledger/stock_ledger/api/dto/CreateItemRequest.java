package com.ledger.stock_ledger.api.dto;

import com.ledger.stock_ledger.domain.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateItemRequest(
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$", message = "code must be letters, digits and hyphens")
        String code,
        @NotBlank @Size(max = 255) String name,
        @NotNull Unit unit
) {
}
