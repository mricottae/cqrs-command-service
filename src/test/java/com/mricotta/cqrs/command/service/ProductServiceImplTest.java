package com.mricotta.cqrs.command.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.mricotta.cqrs.command.dto.ProductRequest;
import com.mricotta.cqrs.command.dto.ProductResponse;
import com.mricotta.cqrs.command.entity.Product;
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

    @InjectMocks
    private ProductServiceImpl productService;

    private final ProductRequest request =
            new ProductRequest("Mouse", "Wireless mouse", new BigDecimal("19.99"), 10);

    @Test
    void createProduct_savesMappedEntityAndReturnsDto() {
        var unsaved = Product.builder().name("Mouse").build();
        var saved = Product.builder().id(1L).name("Mouse").build();
        var expected = response(1L);
        given(productMapper.toEntity(request)).willReturn(unsaved);
        given(productRepository.save(unsaved)).willReturn(saved);
        given(productMapper.toDto(saved)).willReturn(expected);

        var result = productService.createProduct(request);

        assertThat(result).isEqualTo(expected);
        then(productRepository).should().save(unsaved);
    }

    @Test
    void updateProduct_whenProductExists_updatesAndReturnsDto() {
        var existing = Product.builder().id(1L).name("Old").build();
        var expected = response(1L);
        given(productRepository.findById(1L)).willReturn(Optional.of(existing));
        given(productRepository.saveAndFlush(existing)).willReturn(existing);
        given(productMapper.toDto(existing)).willReturn(expected);

        var result = productService.updateProduct(1L, request);

        assertThat(result).isEqualTo(expected);
        then(productMapper).should().updateEntity(request, existing);
        then(productRepository).should().saveAndFlush(existing);
    }

    @Test
    void updateProduct_whenProductMissing_throwsProductNotFoundException() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product with id 99 not found");
        then(productRepository).should(never()).saveAndFlush(any());
    }

    private static ProductResponse response(Long id) {
        var now = Instant.now();
        return new ProductResponse(id, "Mouse", "Wireless mouse", new BigDecimal("19.99"), 10, now, now);
    }
}
