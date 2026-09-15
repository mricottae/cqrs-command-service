package com.mricotta.cqrs.command.messaging;

import com.mricotta.cqrs.command.entity.OutboxEvent;
import com.mricotta.cqrs.command.repository.OutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polling publisher for the transactional outbox: sends unpublished events to Kafka in insertion
 * order and marks them as published. Delivery is at-least-once; consumers must be idempotent.
 */
@Slf4j
@Component
public class OutboxRelay {

    static final String HEADER_EVENT_TYPE = "eventType";
    static final String HEADER_EVENT_ID = "eventId";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final int batchSize;
    private final Duration sendTimeout;
    private final Duration retention;

    public OutboxRelay(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.topics.product-events}") String topic,
            @Value("${app.outbox.batch-size}") int batchSize,
            @Value("${app.outbox.send-timeout}") Duration sendTimeout,
            @Value("${app.outbox.retention}") Duration retention) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.batchSize = batchSize;
        this.sendTimeout = sendTimeout;
        this.retention = retention;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms}")
    @Transactional
    public void publishPendingEvents() {
        for (var outboxEvent : outboxEventRepository.findUnpublishedBatchForUpdate(batchSize)) {
            try {
                kafkaTemplate.send(toProducerRecord(outboxEvent)).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
                outboxEvent.markPublished(Instant.now());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                outboxEvent.markFailed(ex.toString());
                return;
            } catch (ExecutionException | TimeoutException | RuntimeException ex) {
                log.warn("Failed to publish outbox event {} ({} for aggregate {}); will retry",
                        outboxEvent.getEventId(), outboxEvent.getEventType(), outboxEvent.getAggregateId(), ex);
                outboxEvent.markFailed(NestedExceptionUtils.getMostSpecificCause(ex).toString());
                // Stop here so later events for the same aggregate are never published before this one.
                return;
            }
        }
    }

    @Scheduled(cron = "${app.outbox.cleanup-cron}")
    @Transactional
    public void purgePublishedEvents() {
        var deleted = outboxEventRepository.deletePublishedBefore(Instant.now().minus(retention));
        if (deleted > 0) {
            log.info("Purged {} published outbox events older than {}", deleted, retention);
        }
    }

    private ProducerRecord<String, String> toProducerRecord(OutboxEvent outboxEvent) {
        var producerRecord = new ProducerRecord<>(topic, outboxEvent.getAggregateId(), outboxEvent.getPayload());
        producerRecord.headers()
                .add(HEADER_EVENT_TYPE, outboxEvent.getEventType().getBytes(StandardCharsets.UTF_8))
                .add(HEADER_EVENT_ID, outboxEvent.getEventId().toString().getBytes(StandardCharsets.UTF_8));
        return producerRecord;
    }
}
