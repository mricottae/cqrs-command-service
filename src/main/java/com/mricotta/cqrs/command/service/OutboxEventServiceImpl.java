package com.mricotta.cqrs.command.service;

import com.mricotta.cqrs.command.entity.OutboxEvent;
import com.mricotta.cqrs.command.entity.Product;
import com.mricotta.cqrs.command.event.ProductEvent;
import com.mricotta.cqrs.command.event.ProductEventType;
import com.mricotta.cqrs.command.mapper.ProductEventMapper;
import com.mricotta.cqrs.command.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class OutboxEventServiceImpl implements OutboxEventService {

    static final String AGGREGATE_TYPE = "Product";

    private final OutboxEventRepository outboxEventRepository;
    private final ProductEventMapper productEventMapper;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Product product, ProductEventType eventType) {
        var event = new ProductEvent(
                UUID.randomUUID(),
                eventType.wireName(),
                Instant.now(),
                product.getId(),
                product.getVersion(),
                productEventMapper.toPayload(product));

        outboxEventRepository.save(OutboxEvent.builder()
                .eventId(event.eventId())
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(event.aggregateId()))
                .aggregateVersion(event.aggregateVersion())
                .eventType(event.eventType())
                .payload(jsonMapper.writeValueAsString(event))
                .occurredAt(event.occurredAt())
                .build());
    }
}
