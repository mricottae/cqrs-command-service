package com.mricotta.cqrs.command.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import com.mricotta.cqrs.command.dto.ProductRequest;
import com.mricotta.cqrs.command.dto.ProductResponse;
import com.mricotta.cqrs.command.entity.Product;
import com.mricotta.cqrs.command.event.ProductEventType;
import com.mricotta.cqrs.command.exception.ProductNotFoundException;
import com.mricotta.cqrs.command.mapper.ProductMapper;
import com.mricotta.cqrs.command.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private ProductServiceImpl productService;

    private final ProductRequest request =
            new ProductRequest("Mouse", "Wireless mouse", new BigDecimal("19.99"), 10);

    @Test
    void createProduct_savesEntityThenRecordsCreatedEvent() {
        var unsaved = Product.builder().name("Mouse").build();
        var saved = Product.builder().id(1L).name("Mouse").version(0L).build();
        var expected = response(1L, 0L);
        given(productMapper.toEntity(request)).willReturn(unsaved);
        given(productRepository.saveAndFlush(unsaved)).willReturn(saved);
        given(productMapper.toDto(saved)).willReturn(expected);

        var result = productService.createProduct(request);

        assertThat(result).isEqualTo(expected);
        var inOrder = inOrder(productRepository, outboxEventService);
        inOrder.verify(productRepository).saveAndFlush(unsaved);
        inOrder.verify(outboxEventService).record(saved, ProductEventType.PRODUCT_CREATED);
    }

    @Test
    void updateProduct_whenProductExists_updatesThenRecordsUpdatedEvent() {
        var existing = Product.builder().id(1L).name("Old").version(0L).build();
        var expected = response(1L, 1L);
        given(productRepository.findById(1L)).willReturn(Optional.of(existing));
        given(productRepository.saveAndFlush(existing)).willReturn(existing);
        given(productMapper.toDto(existing)).willReturn(expected);

        var result = productService.updateProduct(1L, request);

        assertThat(result).isEqualTo(expected);
        var inOrder = inOrder(productMapper, productRepository, outboxEventService);
        inOrder.verify(productMapper).updateEntity(request, existing);
        inOrder.verify(productRepository).saveAndFlush(existing);
        inOrder.verify(outboxEventService).record(existing, ProductEventType.PRODUCT_UPDATED);
    }

    @Test
    void updateProduct_whenProductMissing_throwsAndRecordsNoEvent() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product with id 99 not found");
        then(productRepository).should(never()).saveAndFlush(any());
        verifyNoInteractions(outboxEventService);
    }

    private static ProductResponse response(Long id, Long version) {
        var now = Instant.now();
        return new ProductResponse(id, "Mouse", "Wireless mouse", new BigDecimal("19.99"), 10, now, now, version);
    }
}
