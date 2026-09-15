package com.mricotta.cqrs.command.event;

import java.math.BigDecimal;
import java.time.Instant;

/** Full product state carried by every product event. */
public record ProductPayload(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Instant createdAt,
        Instant updatedAt) {
}
