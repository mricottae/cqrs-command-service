package com.mricotta.cqrs.command.service;

import com.mricotta.cqrs.command.dto.ProductRequest;
import com.mricotta.cqrs.command.dto.ProductResponse;
import com.mricotta.cqrs.command.event.ProductEventType;
import com.mricotta.cqrs.command.exception.ProductNotFoundException;
import com.mricotta.cqrs.command.mapper.ProductMapper;
import com.mricotta.cqrs.command.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final OutboxEventService outboxEventService;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        var product = productMapper.toEntity(request);
        // Flush so id, version and timestamps are populated before building the event.
        var saved = productRepository.saveAndFlush(product);
        outboxEventService.record(saved, ProductEventType.PRODUCT_CREATED);
        return productMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productMapper.updateEntity(request, product);
        // Flush so @Version and @UpdateTimestamp are applied before building the event and response.
        var saved = productRepository.saveAndFlush(product);
        outboxEventService.record(saved, ProductEventType.PRODUCT_UPDATED);
        return productMapper.toDto(saved);
    }
}
