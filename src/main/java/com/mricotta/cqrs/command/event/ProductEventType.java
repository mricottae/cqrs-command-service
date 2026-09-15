package com.mricotta.cqrs.command.event;

public enum ProductEventType {

    PRODUCT_CREATED("ProductCreated"),
    PRODUCT_UPDATED("ProductUpdated");

    private final String wireName;

    ProductEventType(String wireName) {
        this.wireName = wireName;
    }

    /** Name used in the event JSON and the {@code eventType} Kafka header. */
    public String wireName() {
        return wireName;
    }
}
