package com.ledger.stock_ledger.api;

import com.ledger.stock_ledger.api.dto.CreateItemRequest;
import com.ledger.stock_ledger.api.dto.ItemResponse;
import com.ledger.stock_ledger.api.dto.PageResponse;
import com.ledger.stock_ledger.api.dto.UpdateItemRequest;
import com.ledger.stock_ledger.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService items;

    public ItemController(ItemService items) {
        this.items = items;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(@Valid @RequestBody CreateItemRequest request) {
        return items.create(request);
    }

    @GetMapping
    public PageResponse<ItemResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return items.list(pageable);
    }

    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable UUID id) {
        return items.get(id);
    }

    @PatchMapping("/{id}")
    public ItemResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateItemRequest request) {
        return items.update(id, request);
    }

    @PostMapping("/{id}/disable")
    public ItemResponse disable(@PathVariable UUID id) {
        return items.disable(id);
    }
}
