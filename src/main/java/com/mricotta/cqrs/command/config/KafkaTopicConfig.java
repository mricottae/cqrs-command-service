package com.mricotta.cqrs.command.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private static final int PRODUCT_EVENTS_PARTITIONS = 3;

    /**
     * Compacted: every event carries the full product state, so keeping the latest event per product
     * id is enough to rebuild any read model from scratch.
     */
    @Bean
    public NewTopic productEventsTopic(
            @Value("${app.kafka.topics.product-events}") String topic,
            @Value("${app.kafka.topics.replicas:1}") int replicas) {
        return TopicBuilder.name(topic)
                .partitions(PRODUCT_EVENTS_PARTITIONS)
                .replicas(replicas)
                .compact()
                .build();
    }
}
