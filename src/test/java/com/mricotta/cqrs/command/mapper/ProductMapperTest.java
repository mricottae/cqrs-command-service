package com.mricotta.cqrs.command.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.mricotta.cqrs.command.dto.ProductRequest;
import com.mricotta.cqrs.command.entity.Product;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ProductMapperTest {

    private final ProductMapper mapper = Mappers.getMapper(ProductMapper.class);

    @Test
    void toEntity_mapsRequestFieldsAndLeavesManagedFieldsEmpty() {
        var request = new ProductRequest("Mouse", "Wireless mouse", new BigDecimal("19.99"), 10);

        var product = mapper.toEntity(request);

        assertThat(product)
                .extracting(Product::getName, Product::getDescription, Product::getPrice, Product::getStock)
                .containsExactly("Mouse", "Wireless mouse", new BigDecimal("19.99"), 10);
        assertThat(product.getId()).isNull();
        assertThat(product.getCreatedAt()).isNull();
        assertThat(product.getUpdatedAt()).isNull();
    }

    @Test
    void toDto_mapsAllFields() {
        var now = Instant.now();
        var product = new Product(1L, "Mouse", "Wireless mouse", new BigDecimal("19.99"), 10, now, now);

        var dto = mapper.toDto(product);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Mouse");
        assertThat(dto.description()).isEqualTo("Wireless mouse");
        assertThat(dto.price()).isEqualByComparingTo("19.99");
        assertThat(dto.stock()).isEqualTo(10);
        assertThat(dto.createdAt()).isEqualTo(now);
        assertThat(dto.updatedAt()).isEqualTo(now);
    }

    @Test
    void updateEntity_overwritesMutableFieldsAndKeepsManagedOnes() {
        var createdAt = Instant.parse("2026-01-01T00:00:00Z");
        var product = new Product(1L, "Old", "Old desc", BigDecimal.ONE, 1, createdAt, createdAt);
        var request = new ProductRequest("Mouse", "Wireless mouse", new BigDecimal("19.99"), 10);

        mapper.updateEntity(request, product);

        assertThat(product)
                .extracting(Product::getName, Product::getDescription, Product::getPrice, Product::getStock)
                .containsExactly("Mouse", "Wireless mouse", new BigDecimal("19.99"), 10);
        assertThat(product.getId()).isEqualTo(1L);
        assertThat(product.getCreatedAt()).isEqualTo(createdAt);
    }
}
