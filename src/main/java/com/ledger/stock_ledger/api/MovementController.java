package com.ledger.stock_ledger.api;

import com.ledger.stock_ledger.api.dto.CancelMovementRequest;
import com.ledger.stock_ledger.api.dto.CreateMovementRequest;
import com.ledger.stock_ledger.api.dto.MovementResponse;
import com.ledger.stock_ledger.api.dto.PageResponse;
import com.ledger.stock_ledger.service.MovementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class MovementController {

    private final MovementService movements;

    public MovementController(MovementService movements) {
        this.movements = movements;
    }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse record(@Valid @RequestBody CreateMovementRequest request) {
        return movements.record(request);
    }

    @PostMapping("/movements/{id}/cancel")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse cancel(@PathVariable UUID id, @Valid @RequestBody CancelMovementRequest request) {
        return movements.cancel(id, request);
    }

    @GetMapping("/movements/{id}")
    public MovementResponse get(@PathVariable UUID id) {
        return movements.get(id);
    }

    @GetMapping("/items/{itemId}/movements")
    public PageResponse<MovementResponse> history(
            @PathVariable UUID itemId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return movements.history(itemId, pageable);
    }
}
