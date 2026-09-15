package com.mricotta.cqrs.command.mapper;

import com.mricotta.cqrs.command.entity.Product;
import com.mricotta.cqrs.command.event.ProductPayload;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductEventMapper {

    ProductPayload toPayload(Product product);
}
