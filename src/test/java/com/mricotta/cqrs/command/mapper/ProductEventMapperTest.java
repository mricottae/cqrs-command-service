package com.mricotta.cqrs.command.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.mricotta.cqrs.command.entity.Product;
import com.mricotta.cqrs.command.event.ProductPayload;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ProductEventMapperTest {

    private final ProductEventMapper mapper = Mappers.getMapper(ProductEventMapper.class);

    @Test
    void toPayload_mapsFullProductState() {
        var now = Instant.now();
        var product = new Product(1L, "Mouse", "Wireless mouse", new BigDecimal("19.99"), 10, now, now, 5L);

        var payload = mapper.toPayload(product);

        assertThat(payload).isEqualTo(
                new ProductPayload(1L, "Mouse", "Wireless mouse", new BigDecimal("19.99"), 10, now, now));
    }
}
