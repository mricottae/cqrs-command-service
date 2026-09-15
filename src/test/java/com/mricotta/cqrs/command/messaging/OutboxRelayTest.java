package com.mricotta.cqrs.command.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.mricotta.cqrs.command.entity.OutboxEvent;
import com.mricotta.cqrs.command.repository.OutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final String TOPIC = "catalog.product.events";
    private static final int BATCH_SIZE = 100;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, String>> producerRecordCaptor;

    private OutboxRelay outboxRelay;

    @BeforeEach
    void setUp() {
        outboxRelay = new OutboxRelay(
                outboxEventRepository, kafkaTemplate, TOPIC, BATCH_SIZE, Duration.ofSeconds(1), Duration.ofDays(7));
    }

    @Test
    void publishPendingEvents_sendsKeyedRecordsWithHeadersInOrderAndMarksThemPublished() {
        var first = outboxEvent(1L, "10");
        var second = outboxEvent(2L, "20");
        given(outboxEventRepository.findUnpublishedBatchForUpdate(BATCH_SIZE)).willReturn(List.of(first, second));
        given(kafkaTemplate.send(any(ProducerRecord.class))).willReturn(succeeded());

        outboxRelay.publishPendingEvents();

        then(kafkaTemplate).should(times(2)).send(producerRecordCaptor.capture());
        var records = producerRecordCaptor.getAllValues();
        assertThat(records).extracting(ProducerRecord::topic).containsOnly(TOPIC);
        assertThat(records).extracting(ProducerRecord::key).containsExactly("10", "20");
        assertThat(records).extracting(ProducerRecord::value).containsExactly(first.getPayload(), second.getPayload());
        assertThat(header(records.getFirst(), OutboxRelay.HEADER_EVENT_TYPE)).isEqualTo("ProductCreated");
        assertThat(header(records.getFirst(), OutboxRelay.HEADER_EVENT_ID)).isEqualTo(first.getEventId().toString());

        assertThat(first.getPublishedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        assertThat(second.getPublishedAt()).isNotNull();
    }

    @Test
    void publishPendingEvents_whenSendFails_recordsAttemptAndStopsTheBatch() {
        var first = outboxEvent(1L, "10");
        var second = outboxEvent(2L, "10");
        given(outboxEventRepository.findUnpublishedBatchForUpdate(BATCH_SIZE)).willReturn(List.of(first, second));
        given(kafkaTemplate.send(any(ProducerRecord.class)))
                .willReturn(CompletableFuture.<SendResult<String, String>>failedFuture(
                        new IllegalStateException("broker unavailable")));

        outboxRelay.publishPendingEvents();

        then(kafkaTemplate).should(times(1)).send(any(ProducerRecord.class));
        assertThat(first.getPublishedAt()).isNull();
        assertThat(first.getAttempts()).isEqualTo(1);
        assertThat(first.getLastError()).contains("broker unavailable");
        assertThat(second.getPublishedAt()).isNull();
        assertThat(second.getAttempts()).isZero();
    }

    @Test
    void publishPendingEvents_whenNothingPending_sendsNothing() {
        given(outboxEventRepository.findUnpublishedBatchForUpdate(BATCH_SIZE)).willReturn(List.of());

        outboxRelay.publishPendingEvents();

        then(kafkaTemplate).shouldHaveNoInteractions();
    }

    @Test
    void purgePublishedEvents_deletesRowsOlderThanRetention() {
        var cutoffCaptor = ArgumentCaptor.forClass(Instant.class);

        outboxRelay.purgePublishedEvents();

        then(outboxEventRepository).should().deletePublishedBefore(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue())
                .isCloseTo(Instant.now().minus(Duration.ofDays(7)), within(5, ChronoUnit.SECONDS));
    }

    private static CompletableFuture<SendResult<String, String>> succeeded() {
        return CompletableFuture.completedFuture(null);
    }

    private static String header(ProducerRecord<String, String> producerRecord, String key) {
        return new String(producerRecord.headers().lastHeader(key).value(), StandardCharsets.UTF_8);
    }

    private static OutboxEvent outboxEvent(long id, String aggregateId) {
        return OutboxEvent.builder()
                .id(id)
                .eventId(UUID.randomUUID())
                .aggregateType("Product")
                .aggregateId(aggregateId)
                .aggregateVersion(0L)
                .eventType("ProductCreated")
                .payload("{\"aggregateId\":" + aggregateId + "}")
                .occurredAt(Instant.now())
                .build();
    }
}
