package com.mricotta.cqrs.command.service;

import com.mricotta.cqrs.command.entity.Product;
import com.mricotta.cqrs.command.event.ProductEventType;

public interface OutboxEventService {

    /** Stores the event in the outbox. Must run inside the transaction that changed the product. */
    void record(Product product, ProductEventType eventType);
}
