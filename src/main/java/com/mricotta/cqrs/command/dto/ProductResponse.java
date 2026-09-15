package com.mricotta.cqrs.command.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Instant createdAt,
        Instant updatedAt) {
}
