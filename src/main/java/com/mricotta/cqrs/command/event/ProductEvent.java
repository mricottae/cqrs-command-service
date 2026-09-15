package com.mricotta.cqrs.command.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event envelope published to {@code catalog.product.events}. The JSON shape is the contract with
 * consumers; add fields only in a backward-compatible way.
 */
public record ProductEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long aggregateId,
        Long aggregateVersion,
        ProductPayload payload) {
}
