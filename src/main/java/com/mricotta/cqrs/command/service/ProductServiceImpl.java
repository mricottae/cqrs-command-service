package com.mricotta.cqrs.command.service;

import com.mricotta.cqrs.command.dto.ProductRequest;
import com.mricotta.cqrs.command.dto.ProductResponse;
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

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        var product = productMapper.toEntity(request);
        var saved = productRepository.save(product);
        return productMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productMapper.updateEntity(request, product);
        // Flush so @UpdateTimestamp is applied before mapping the response.
        var saved = productRepository.saveAndFlush(product);
        return productMapper.toDto(saved);
    }
}
