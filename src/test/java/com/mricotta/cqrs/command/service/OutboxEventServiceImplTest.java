package com.mricotta.cqrs.command.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.mricotta.cqrs.command.entity.OutboxEvent;
import com.mricotta.cqrs.command.entity.Product;
import com.mricotta.cqrs.command.event.ProductEvent;
import com.mricotta.cqrs.command.event.ProductEventType;
import com.mricotta.cqrs.command.event.ProductPayload;
import com.mricotta.cqrs.command.mapper.ProductEventMapper;
import com.mricotta.cqrs.command.repository.OutboxEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceImplTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private OutboxEventServiceImpl outboxEventService;

    @BeforeEach
    void setUp() {
        outboxEventService = new OutboxEventServiceImpl(
                outboxEventRepository, Mappers.getMapper(ProductEventMapper.class), jsonMapper);
    }

    @Test
    void record_savesUnpublishedOutboxRowWithSerializedEnvelope() {
        var createdAt = Instant.parse("2026-09-14T09:00:00Z");
        var updatedAt = Instant.parse("2026-09-14T10:00:00Z");
        var product = new Product(7L, "Mouse", "Wireless mouse", new BigDecimal("19.99"), 10, createdAt, updatedAt, 2L);

        outboxEventService.record(product, ProductEventType.PRODUCT_UPDATED);

        then(outboxEventRepository).should().save(outboxEventCaptor.capture());
        var saved = outboxEventCaptor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("Product");
        assertThat(saved.getAggregateId()).isEqualTo("7");
        assertThat(saved.getAggregateVersion()).isEqualTo(2L);
        assertThat(saved.getEventType()).isEqualTo("ProductUpdated");
        assertThat(saved.getEventId()).isNotNull();
        assertThat(saved.getOccurredAt()).isNotNull();
        assertThat(saved.getPublishedAt()).isNull();
        assertThat(saved.getAttempts()).isZero();

        assertThat(saved.getPayload())
                .contains("\"eventType\":\"ProductUpdated\"")
                .contains("\"updatedAt\":\"2026-09-14T10:00:00Z\"");
        var event = jsonMapper.readValue(saved.getPayload(), ProductEvent.class);
        assertThat(event.eventId()).isEqualTo(saved.getEventId());
        assertThat(event.aggregateId()).isEqualTo(7L);
        assertThat(event.aggregateVersion()).isEqualTo(2L);
        assertThat(event.payload()).isEqualTo(
                new ProductPayload(7L, "Mouse", "Wireless mouse", new BigDecimal("19.99"), 10, createdAt, updatedAt));
    }
}
