package com.mricotta.cqrs.command.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.0") @Digits(integer = 17, fraction = 2) BigDecimal price,
        @NotNull @PositiveOrZero Integer stock) {
}
