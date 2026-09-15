package com.mricotta.cqrs.command.service;

import com.mricotta.cqrs.command.dto.ProductRequest;
import com.mricotta.cqrs.command.dto.ProductResponse;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);
}
