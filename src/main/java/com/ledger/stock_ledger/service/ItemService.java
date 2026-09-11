package com.ledger.stock_ledger.service;

import com.ledger.stock_ledger.api.ApiException;
import com.ledger.stock_ledger.api.dto.CreateItemRequest;
import com.ledger.stock_ledger.api.dto.ItemResponse;
import com.ledger.stock_ledger.api.dto.PageResponse;
import com.ledger.stock_ledger.api.dto.UpdateItemRequest;
import com.ledger.stock_ledger.domain.Item;
import com.ledger.stock_ledger.repo.ItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ItemService {

    private final ItemRepository items;
    private final Clock clock;

    public ItemService(ItemRepository items, Clock clock) {
        this.items = items;
        this.clock = clock;
    }

    @Transactional
    public ItemResponse create(CreateItemRequest request) {
        String code = request.code().toUpperCase();
        if (items.existsByCode(code)) {
            throw ApiException.conflict("Item code already exists: " + code);
        }
        Item item = new Item(UUID.randomUUID(), code, request.name().trim(), request.unit(), Instant.now(clock));
        return ItemResponse.from(items.save(item));
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> list(Pageable pageable) {
        Page<Item> page = items.findAll(pageable);
        return new PageResponse<>(
                page.getContent().stream().map(ItemResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ItemResponse get(UUID id) {
        return ItemResponse.from(require(id));
    }

    @Transactional
    public ItemResponse update(UUID id, UpdateItemRequest request) {
        Item item = require(id);
        if (request.name() != null && !request.name().isBlank()) {
            item.rename(request.name().trim());
        }
        if (request.unit() != null) {
            item.changeUnit(request.unit());
        }
        return ItemResponse.from(item);
    }

    @Transactional
    public ItemResponse disable(UUID id) {
        Item item = require(id);
        if (!item.isDisabled()) {
            item.disable(Instant.now(clock));
        }
        return ItemResponse.from(item);
    }

    public Item require(UUID id) {
        return items.findById(id).orElseThrow(() -> ApiException.notFound("Item not found: " + id));
    }

    public Item requireByCode(String code) {
        return items.findByCode(code.toUpperCase())
                .orElseThrow(() -> ApiException.notFound("Item not found: " + code));
    }
}
